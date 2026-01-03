package cn.teacy.ai.core;

import cn.teacy.ai.annotation.*;
import cn.teacy.ai.exception.GraphDefinitionException;
import cn.teacy.ai.interfaces.GraphBuildLifecycle;
import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.action.*;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class ReflectiveGraphBuilder implements IGraphBuilder {

    private static final Logger log = LoggerFactory.getLogger(ReflectiveGraphBuilder.class);

    public CompiledGraph build(Object graphComposer) {
        Class<?> clazz = graphComposer.getClass();
        GraphComposer composerAnno = clazz.getAnnotation(GraphComposer.class);

        if (composerAnno == null) {
            throw new IllegalArgumentException("Provided object is not annotated with @GraphComposer: " + clazz.getName());
        }

        String graphId = StringUtils.hasText(composerAnno.id())
                ? composerAnno.id()
                : clazz.getSimpleName();

        try {
            Map<String, KeyStrategy> keyStrategies = scanKeys(clazz);

            StateGraph builder = new StateGraph(graphId, () -> keyStrategies);

            Map<String, String> linearEdges = scanNodes(graphComposer, builder);

            linearEdges.forEach((from, to) -> {
                log.debug("Adding Edge: {} -> {}", from, to);
                try {
                    builder.addEdge(from, to);
                } catch (GraphStateException e) {
                    throw new GraphDefinitionException(
                            String.format("Invalid Edge in [%s]: Cannot connect '%s' -> '%s'. Cause: %s",
                                    graphId, from, to, e.getMessage()), e);
                }
            });

            scanConditionalEdges(graphComposer, builder, graphId);

            if (graphComposer instanceof GraphBuildLifecycle lifecycle) {
                lifecycle.beforeCompile(builder);
            }

            CompileConfig compileConfig = scanCompileConfig(graphComposer);

            return builder.compile(compileConfig);

        } catch (GraphStateException e) {
            throw new GraphDefinitionException(
                    String.format("Failed to compile Graph [%s]. Structure is invalid. Cause: %s",
                            graphId, e.getMessage()), e);
        } catch (GraphDefinitionException e) {
            throw e;
        } catch (Exception e) {
            throw new GraphDefinitionException("Unexpected error building graph: " + graphId, e);
        }
    }

    private Map<String, KeyStrategy> scanKeys(Class<?> clazz) {
        Map<String, KeyStrategy> strategyMap = new HashMap<>();

        ReflectionUtils.doWithFields(clazz, field -> {
            if (field.isAnnotationPresent(GraphKey.class)) {
                ReflectionUtils.makeAccessible(field);
                GraphKey anno = field.getAnnotation(GraphKey.class);

                try {
                    // 1. annotation#value
                    // 2. static field value
                    // 3. field name
                    String keyName = anno.value();
                    if (!StringUtils.hasText(keyName)) {
                        Object staticValue = field.get(null);
                        if (staticValue instanceof String) {
                            keyName = (String) staticValue;
                        } else {
                            keyName = field.getName();
                        }
                    }

                    KeyStrategy strategy = BeanUtils.instantiateClass(anno.strategy());

                    strategyMap.put(keyName, strategy);

                } catch (Exception e) {
                    throw new IllegalStateException("Failed to register Key from field: " + field.getName(), e);
                }
            }
        });

        return strategyMap;
    }

    private Map<String, String> scanNodes(Object composer, StateGraph builder) {
        Map<String, String> edges = new HashMap<>();

        ReflectionUtils.doWithFields(composer.getClass(), field -> {
            if (field.isAnnotationPresent(GraphNode.class)) {
                ReflectionUtils.makeAccessible(field);
                GraphNode anno = field.getAnnotation(GraphNode.class);

                String nodeId = StringUtils.hasText(anno.id()) ? anno.id() : field.getName();

                try {
                    Object nodeInstance = field.get(composer);
                    if (nodeInstance == null) {
                        throw new IllegalStateException("GraphNode field '" + field.getName() + "' is null. Please initialize it.");
                    }

                    if (nodeInstance instanceof AsyncNodeActionWithConfig action) {
                        builder.addNode(nodeId, action);
                    } else if (nodeInstance instanceof AsyncNodeAction action) {
                        builder.addNode(nodeId, action);
                    } else {
                        throw new IllegalArgumentException("Field '" + field.getName() + "' annotated with @GraphNode must implement a NodeAction interface.");
                    }

                    if (anno.isStart()) {
                        edges.put(StateGraph.START, nodeId);
                    }

                    if (StringUtils.hasText(anno.next())) {
                        edges.put(nodeId, anno.next());
                    }

                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("Cannot access node field: " + field.getName(), e);
                } catch (GraphStateException e) {
                    throw new GraphDefinitionException(
                            String.format("Failed to add node '%s'. Cause: %s", nodeId, e.getMessage()), e);
                }
            }
        });
        return edges;
    }

    private void scanConditionalEdges(Object composer, StateGraph builder, String graphId) {
        ReflectionUtils.doWithMethods(composer.getClass(), method -> {
            if (method.isAnnotationPresent(ConditionalEdge.class)) {
                ReflectionUtils.makeAccessible(method);
                ConditionalEdge anno = method.getAnnotation(ConditionalEdge.class);
                assert anno != null;

                String sourceNodeId = anno.source();
                Map<String, String> routeMap = parseMappings(anno.mappings(), method.getName());

                AsyncCommandAction unifiedAction = (state, config) -> {
                    try {
                        Object[] args = prepareArguments(method, state, config);
                        Object result = method.invoke(composer, args);
                        return normalizeResult(result);
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException("Error executing conditional edge: " + method.getName(), e.getTargetException());
                    } catch (Exception e) {
                        throw new RuntimeException("Reflection error in method: " + method.getName(), e);
                    }
                };

                log.debug("Registering conditional edge for source '{}'", sourceNodeId);

                try {
                    builder.addConditionalEdges(sourceNodeId, unifiedAction, routeMap);
                } catch (GraphStateException e) {
                    throw new GraphDefinitionException(
                            String.format("Invalid Conditional Edge in [%s]: Method '%s' (source='%s'). Check if source node exists or mappings are valid.",
                                    graphId, method.getName(), sourceNodeId), e);
                }
            }
        });
    }

    private CompileConfig scanCompileConfig(Object composer) {

        AtomicReference<CompileConfig> configRef = new AtomicReference<>();

        ReflectionUtils.doWithMethods(composer.getClass(), method -> {
            if (method.isAnnotationPresent(GraphCompileConfig.class)) {
                if (configRef.get() != null) {
                    throw new IllegalStateException("Multiple @GraphConfig methods found in " + composer.getClass().getSimpleName());
                }

                ReflectionUtils.makeAccessible(method);

                if (!CompileConfig.class.isAssignableFrom(method.getReturnType())) {
                    throw new IllegalStateException("@GraphConfig method must return CompileConfig");
                }

                try {
                    CompileConfig config = (CompileConfig) method.invoke(composer);
                    configRef.set(config);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to invoke @GraphConfig method", e);
                }
            }
        });

        return Optional.ofNullable(configRef.get()).orElseGet(() -> CompileConfig.builder().build());

    }

    private Object[] prepareArguments(Method method, OverAllState state, RunnableConfig config) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Class<?> type = parameters[i].getType();
            if (OverAllState.class.isAssignableFrom(type)) {
                args[i] = state;
            } else if (RunnableConfig.class.isAssignableFrom(type)) {
                args[i] = config;
            } else {
                args[i] = null;
            }
        }
        return args;
    }

    private CompletableFuture<Command> normalizeResult(Object result) {
        CompletableFuture<?> futureResult;

        if (result instanceof CompletableFuture) {
            futureResult = (CompletableFuture<?>) result;
        } else {
            futureResult = CompletableFuture.completedFuture(result);
        }

        return futureResult.thenApply(res -> {
            if (res instanceof Command cmd) {
                return cmd;
            } else if (res instanceof String route) {
                return new Command(route);
            } else if (res == null) {
                throw new IllegalStateException("Conditional edge returned null result.");
            } else {
                throw new IllegalStateException("Unsupported return type: " + res.getClass().getName() +
                        ". Expected String or Command.");
            }
        });
    }

    private Map<String, String> parseMappings(String[] mappings, String methodName) {
        if (mappings.length % 2 != 0) {
            throw new IllegalArgumentException("Mappings must be pairs in method: " + methodName);
        }
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < mappings.length; i += 2) {
            map.put(mappings[i], mappings[i + 1]);
        }
        return map;
    }
}