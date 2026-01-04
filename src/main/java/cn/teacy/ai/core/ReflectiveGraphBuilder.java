package cn.teacy.ai.core;

import cn.teacy.ai.annotation.*;
import cn.teacy.ai.exception.GraphDefinitionException;
import cn.teacy.ai.interfaces.GraphBuildLifecycle;
import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.action.*;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
                    } else if (nodeInstance instanceof NodeActionWithConfig action) {
                        builder.addNode(nodeId, AsyncNodeActionWithConfig.node_async(action));
                    } else if (nodeInstance instanceof NodeAction action) {
                        builder.addNode(nodeId, AsyncNodeAction.node_async(action));
                    } else if (nodeInstance instanceof CompiledGraph subGraph) {
                        builder.addNode(nodeId, subGraph);
                    } else {
                        String supportedTypes = Set.of(
                                NodeAction.class,
                                AsyncNodeAction.class,
                                NodeActionWithConfig.class,
                                AsyncNodeActionWithConfig.class,
                                CompiledGraph.class
                        ).stream().map(Class::getSimpleName).collect(Collectors.joining(" or "));
                        throw new IllegalArgumentException("Field '" + field.getName() + "' annotated with @GraphNode must be instance of " + supportedTypes + ". ");
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
        ReflectionUtils.doWithFields(composer.getClass(), field -> {
            if (field.isAnnotationPresent(ConditionalEdge.class)) {
                ReflectionUtils.makeAccessible(field);
                ConditionalEdge anno = field.getAnnotation(ConditionalEdge.class);
                assert anno != null;

                Map<String, String> routeMap = parseMappings(anno.mappings(), field.getName());
                String sourceNodeId = anno.source();

                try {
                    Object fieldVal = field.get(composer);

                    if (fieldVal == null) {
                        throw new GraphDefinitionException(String.format(
                                "Conditional Edge field '%s' in graph [%s] is null. " +
                                        "Please initialize it with a lambda expression or instance.",
                                field.getName(), graphId));
                    }

                    AsyncCommandAction unifiedAction = getUnifiedAction(fieldVal, field.getName());

                    builder.addConditionalEdges(sourceNodeId, unifiedAction, routeMap);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Cannot access field: " + field.getName(), e);
                } catch (GraphStateException e) {
                    throw new GraphDefinitionException(
                            String.format("Failed to add conditional edges from node '%s' in graph '%s'. Cause: %s",
                                    sourceNodeId, graphId, e.getMessage()), e);
                }

            }
        });
    }

    @NotNull
    private static AsyncCommandAction getUnifiedAction(Object fieldVal, String fieldName) {
        if (fieldVal instanceof AsyncCommandAction) {
            return (AsyncCommandAction) fieldVal;
        }
        if (fieldVal instanceof AsyncEdgeAction) {
            // wrap once
            return AsyncCommandAction.of((AsyncEdgeAction) fieldVal);
        }
        if (fieldVal instanceof EdgeAction) {
            // wrap twice
            AsyncEdgeAction asyncWrapper = AsyncEdgeAction.edge_async((EdgeAction) fieldVal);
            return AsyncCommandAction.of(asyncWrapper);
        }

        throw new GraphDefinitionException(String.format(
                "Field '%s' type [%s] is not supported. Must be one of: [EdgeAction, AsyncEdgeAction, AsyncCommandAction]",
                fieldName, fieldVal.getClass().getSimpleName()));
    }

    private CompileConfig scanCompileConfig(Object composer) {

        AtomicReference<CompileConfig> configRef = new AtomicReference<>();

        ReflectionUtils.doWithFields(composer.getClass(), field -> {
            if (field.isAnnotationPresent(GraphCompileConfig.class)) {
                if (configRef.get() != null) {
                    throw new IllegalStateException("Multiple @GraphCompileConfig fields found in " + composer.getClass().getSimpleName());
                }

                ReflectionUtils.makeAccessible(field);

                try {
                    Object value = field.get(composer);

                    if (value == null) {
                        throw new IllegalStateException("@GraphCompileConfig field '" + field.getName() + "' must not be null.");
                    }

                    if (value instanceof CompileConfig) {
                        configRef.set((CompileConfig) value);
                    } else if (value instanceof Supplier) {
                        Object suppliedValue = ((Supplier<?>) value).get();

                        if (suppliedValue instanceof CompileConfig) {
                            configRef.set((CompileConfig) suppliedValue);
                        } else {
                            throw new IllegalStateException("The Supplier in field '" + field.getName() + "' returned null or an invalid type.");
                        }
                    } else {
                        throw new IllegalStateException("Field '" + field.getName() + "' must be of type CompileConfig or Supplier<CompileConfig>.");
                    }

                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Failed to access @GraphCompileConfig field: " + field.getName(), e);
                }
            }
        });

        return Optional.ofNullable(configRef.get()).orElseGet(() -> CompileConfig.builder().build());

    }

    private Map<String, String> parseMappings(String[] mappings, String fieldName) {
        if (mappings.length % 2 != 0) {
            throw new IllegalArgumentException("Mappings must be pairs in field: " + fieldName);
        }
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < mappings.length; i += 2) {
            map.put(mappings[i], mappings[i + 1]);
        }
        return map;
    }
}