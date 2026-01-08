package cn.teacy.ai.core;

import cn.teacy.ai.annotation.*;
import cn.teacy.ai.exception.GraphDefinitionException;
import cn.teacy.ai.interfaces.GraphBuildLifecycle;
import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.action.*;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ReflectiveGraphCompiler implements GraphCompiler {

    private static final Logger log = LoggerFactory.getLogger(ReflectiveGraphCompiler.class);

    protected record CompileContext(
            Object composerInstance,
            @Nonnull Map<String, KeyStrategy> keyStrategies,
            @Nonnull List<GraphOperation> operations,
            @Nonnull AtomicReference<CompileConfig> configRef
    ) {}

    @FunctionalInterface
    protected interface GraphOperation {
        void execute(StateGraph builder) throws GraphStateException;
    }

    @Override
    public final CompiledGraph compile(Object graphComposer) {
        Class<?> clazz = graphComposer.getClass();
        GraphComposer composerAnno = clazz.getAnnotation(GraphComposer.class);

        if (composerAnno == null) {
            throw new IllegalArgumentException("Provided object is not annotated with @GraphComposer: " + clazz.getName());
        }

        String graphId = StringUtils.hasText(composerAnno.id())
                ? composerAnno.id()
                : clazz.getSimpleName();

        try {
            CompileContext context = collectCompileContextFromComposer(graphComposer);

            StateGraph builder = new StateGraph(graphId, () -> context.keyStrategies);

            if (graphComposer instanceof GraphBuildLifecycle lifecycleHook) {
                lifecycleHook.afterKeyRegistration(builder);
            }

            for (GraphOperation modification : context.operations) {
                modification.execute(builder);
            }

            if (graphComposer instanceof GraphBuildLifecycle lifecycleHook) {
                lifecycleHook.beforeCompile(builder);
            }

            CompileConfig compileConfig = context.configRef.get();

            return compileConfig == null
                    ? builder.compile()
                    : builder.compile(compileConfig);

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

    private CompileContext collectCompileContextFromComposer(Object composer) {
        Class<?> clazz = composer.getClass();

        CompileContext context = new CompileContext(
                composer,
                new HashMap<>(),
                new ArrayList<>(),
                new AtomicReference<>()
        );

        ReflectionUtils.doWithFields(clazz, field -> {
            if (field.isAnnotationPresent(GraphKey.class)) {
                handleGraphKey(context, field, field.getAnnotation(GraphKey.class));
            } else if (field.isAnnotationPresent(GraphNode.class)) {
                handleGraphNode(context, field, field.getAnnotation(GraphNode.class));
            } else if (field.isAnnotationPresent(ConditionalEdge.class)) {
                handleConditionalEdge(context, field, field.getAnnotation(ConditionalEdge.class));
            } else if (field.isAnnotationPresent(GraphCompileConfig.class)) {
                handleCompileConfig(context, field, field.getAnnotation(GraphCompileConfig.class));
            } else {
                handleOtherField(context, field);
            }
        });

        return context;

    }

    protected void handleGraphKey(CompileContext context, Field field, GraphKey annotation) {
        if (!Modifier.isFinal(field.getModifiers())) {
            throw new GraphDefinitionException(
                    String.format("Field '%s' must be 'final'. Graph keys should be immutable constants.", field.getName()));
        }

        if (!Modifier.isStatic(field.getModifiers())) {
            throw new GraphDefinitionException(
                    String.format("Field '%s' must be 'static'. Graph keys supposed to be global constants (e.g., public static final String).", field.getName()));
        }

        if (field.getType() != String.class) {
            throw new GraphDefinitionException("Field type must be String.");
        }

        ReflectionUtils.makeAccessible(field);
        String keyName = (String) ReflectionUtils.getField(field, null);

        KeyStrategy strategy = BeanUtils.instantiateClass(annotation.strategy());

        if (context.keyStrategies.containsKey(keyName)) {
            throw new GraphDefinitionException("Duplicate Graph Key detected: " + keyName +
                    ". Defined in field: " + field.getName());
        }
        context.keyStrategies.put(keyName, strategy);
    }

    protected void handleGraphNode(CompileContext context, Field field, GraphNode annotation) {
        String nodeId = StringUtils.hasText(annotation.id()) ? annotation.id() : field.getName();

        ReflectionUtils.makeAccessible(field);
        Object nodeInstance = ReflectionUtils.getField(field, context.composerInstance);

        if (nodeInstance == null) {
            throw new IllegalStateException("GraphNode field '" + field.getName() + "' is null. Please initialize it.");
        }

        if (nodeInstance instanceof AsyncNodeActionWithConfig action) {
            context.operations.add(builder -> builder.addNode(nodeId, action));
        } else if (nodeInstance instanceof AsyncNodeAction action) {
            context.operations.add(builder -> builder.addNode(nodeId, action));
        } else if (nodeInstance instanceof NodeActionWithConfig action) {
            context.operations.add(builder -> builder.addNode(nodeId, AsyncNodeActionWithConfig.node_async(action)));
        } else if (nodeInstance instanceof NodeAction action) {
            context.operations.add(builder -> builder.addNode(nodeId, AsyncNodeAction.node_async(action)));
        } else if (nodeInstance instanceof CompiledGraph subGraph) {
            context.operations.add(builder -> builder.addNode(nodeId, subGraph));
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

        if (annotation.isStart()) {
            context.operations.add(builder -> builder.addEdge(StateGraph.START, nodeId));
        }

        for (String next : annotation.next()) {
            if (!StringUtils.hasText(next)) {
                continue;
            }
            context.operations.add(builder -> builder.addEdge(nodeId, next));
        }

    }

    protected void handleConditionalEdge(CompileContext context, Field field, ConditionalEdge annotation) {
        Map<String, String> routeMap = parseMappings(annotation.mappings(), field.getName());
        String sourceNodeId = annotation.source();

        ReflectionUtils.makeAccessible(field);
        Object fieldVal = ReflectionUtils.getField(field, context.composerInstance);

        if (fieldVal == null) {
            throw new GraphDefinitionException("Conditional Edge field '"
                    + field.getName()
                    + "' is null. Please initialize it with a lambda expression or instance.");
        }

        AsyncCommandAction unifiedAction = getUnifiedAction(fieldVal, field.getName());

        context.operations.add(builder -> builder.addConditionalEdges(sourceNodeId, unifiedAction, routeMap));
    }

    @Nonnull
    protected static AsyncCommandAction getUnifiedAction(Object fieldVal, String fieldName) {
        if (fieldVal instanceof AsyncCommandAction action) {
            return action;
        }
        if (fieldVal instanceof CommandAction action) {
            return AsyncCommandAction.node_async(action);
        }
        if (fieldVal instanceof AsyncEdgeAction action) {
            // wrap once
            return AsyncCommandAction.of(action);
        }
        if (fieldVal instanceof EdgeAction action) {
            // wrap twice
            AsyncEdgeAction asyncWrapper = AsyncEdgeAction.edge_async(action);
            return AsyncCommandAction.of(asyncWrapper);
        }
        throw new GraphDefinitionException(String.format(
                "Field '%s' type [%s] is not supported. Must be one of: [EdgeAction, AsyncEdgeAction, CommandAction, AsyncCommandAction]",
                fieldName, fieldVal.getClass().getSimpleName()));
    }

    protected void handleCompileConfig(CompileContext context, Field field, GraphCompileConfig annotation) {
        if (context.configRef.get() != null) {
            throw new IllegalStateException("Multiple @GraphCompileConfig fields found in " + context.composerInstance.getClass().getSimpleName());
        }

        ReflectionUtils.makeAccessible(field);
        Object value = ReflectionUtils.getField(field, context.composerInstance);

        if (value == null) {
            throw new GraphDefinitionException("@GraphCompileConfig field '" + field.getName() + "' must not be null.");
        }

        if (value instanceof CompileConfig) {
            context.configRef.set((CompileConfig) value);
        } else if (value instanceof Supplier) {
            Object suppliedValue = ((Supplier<?>) value).get();
            if (suppliedValue instanceof CompileConfig) {
                context.configRef.set((CompileConfig) suppliedValue);
            } else {
                throw new GraphDefinitionException("The Supplier in field '" + field.getName() + "' returned null or an invalid type.");
            }
        } else {
            throw new GraphDefinitionException("Field '" + field.getName() + "' must be of type CompileConfig or Supplier<CompileConfig>.");
        }

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

    /** Placeholder for handling other field types in the future. */
    protected void handleOtherField(CompileContext context, Field field) {
        log.debug("Field '{}' is not annotated with recognized graph annotations.", field.getName());
    }

}