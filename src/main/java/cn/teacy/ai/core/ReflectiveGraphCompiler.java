package cn.teacy.ai.core;

import cn.teacy.ai.annotation.*;
import cn.teacy.ai.exception.GraphDefinitionException;
import cn.teacy.ai.interfaces.GraphBuildLifecycle;
import cn.teacy.ai.utils.UnifyUtils;
import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.action.*;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Supplier;

public class ReflectiveGraphCompiler implements GraphCompiler {

    private static final Logger log = LoggerFactory.getLogger(ReflectiveGraphCompiler.class);

    protected static class CompileContext {
        private final Object composerInstance;
        private final Map<String, KeyStrategy> keyStrategies = new HashMap<>();
        private final List<GraphOperation> operations = new ArrayList<>();
        private CompileConfig compileConfig;

        protected CompileContext(Object composerInstance) {
            this.composerInstance = composerInstance;
        }

        public Object composerInstance() {
            return composerInstance;
        }

        public boolean containsKey(String key) {
            return this.keyStrategies.containsKey(key);
        }

        public void addKeyStrategy(String key, KeyStrategy strategy) {
            this.keyStrategies.put(key, strategy);
        }

        public boolean hasCompileConfig() {
            return this.compileConfig != null;
        }

        public void setCompileConfig(CompileConfig config) {
            this.compileConfig = config;
        }

        public void registerOperation(GraphOperation op, String errorFormat, Object... args) {
            this.operations.add(builder -> {
                try {
                    op.execute(builder);
                } catch (Exception e) {
                    String msg = String.format(errorFormat, args);
                    throw new GraphDefinitionException(String.format("Failed to %s. Cause: %s", msg, e.getMessage()), e);
                }
            });
        }

        public GraphDefinition toDefinition() {
            return new GraphDefinition(
                    composerInstance,
                    Map.copyOf(keyStrategies),
                    List.copyOf(operations),
                    compileConfig
            );
        }

    }

    protected record GraphDefinition(
            @Nonnull Object composerInstance,
            @Nonnull Map<String, KeyStrategy> keyStrategies,
            @Nonnull List<GraphOperation> operations,
            @Nullable CompileConfig compileConfig
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
            GraphDefinition definition = collectGraphDefinition(graphComposer);

            StateGraph builder = new StateGraph(graphId, definition::keyStrategies);

            if (graphComposer instanceof GraphBuildLifecycle lifecycleHook) {
                lifecycleHook.afterKeyRegistration(builder);
            }

            for (GraphOperation modification : definition.operations) {
                modification.execute(builder);
            }

            if (graphComposer instanceof GraphBuildLifecycle lifecycleHook) {
                lifecycleHook.beforeCompile(builder);
            }

            CompileConfig compileConfig = definition.compileConfig;

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

    private GraphDefinition collectGraphDefinition(Object composer) {
        Class<?> clazz = composer.getClass();

        CompileContext context = new CompileContext(composer);

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

        return context.toDefinition();
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

        if (context.containsKey(keyName)) {
            throw new GraphDefinitionException("Duplicate Graph Key: " + keyName + ". Defined in field: " + field.getName());
        }

        KeyStrategy strategy = BeanUtils.instantiateClass(annotation.strategy());

        context.addKeyStrategy(keyName, strategy);
    }

    protected void handleGraphNode(CompileContext context, Field field, GraphNode annotation) {
        String nodeId = StringUtils.hasText(annotation.id()) ? annotation.id() : field.getName();

        ReflectionUtils.makeAccessible(field);
        Object nodeInstance = ReflectionUtils.getField(field, context.composerInstance());

        if (nodeInstance == null) {
            nodeInstance = resolveMissingField(field, nodeId);
        }

        if (nodeInstance == null) {
            throw new IllegalStateException("GraphNode field '" + field.getName() + "' is null. Please initialize it.");
        }

        String fieldName = field.getName();

        try {
            if (nodeInstance instanceof CompiledGraph subGraph) {
                context.registerOperation(b -> b.addNode(nodeId, subGraph),
                        "add SubGraph node '%s' (field: %s)", nodeId, fieldName);
            } else {
                AsyncNodeActionWithConfig action = UnifyUtils.getUnifiedNodeAction(nodeInstance);
                context.registerOperation(b -> b.addNode(nodeId, action),
                        "add NodeAction node '%s' (field: %s)", nodeId, fieldName);
            }
        } catch (IllegalArgumentException e) {
            throw new GraphDefinitionException(String.format(
                    "Field '%s' type [%s] for @GraphNode is not supported. Must be one of: [CompiledGraph, NodeAction, AsyncNodeAction, NodeActionWithConfig, AsyncNodeActionWithConfig]",
                    fieldName, nodeInstance.getClass().getSimpleName()), e);
        }

        if (annotation.isStart()) {
            context.registerOperation(builder -> builder.addEdge(StateGraph.START, nodeId),
                    "add start edge to node '%s' (field: %s)", nodeId, fieldName);
        }

        for (String next : annotation.next()) {
            if (!StringUtils.hasText(next)) {
                continue;
            }
            context.registerOperation(builder -> builder.addEdge(nodeId, next),
                    "add edge from node '%s' to next node '%s' (field: %s)", nodeId, next, fieldName);
        }

    }

    protected void handleConditionalEdge(CompileContext context, Field field, ConditionalEdge annotation) {
        Map<String, String> routeMap = parseMappings(annotation.mappings(), field.getName());
        String sourceNodeId = annotation.source();

        ReflectionUtils.makeAccessible(field);
        Object fieldVal = ReflectionUtils.getField(field, context.composerInstance());

        if (fieldVal == null) {
            fieldVal = resolveMissingField(field, null);
        }

        if (fieldVal == null) {
            throw new GraphDefinitionException("Conditional Edge field '"
                    + field.getName()
                    + "' is null. Please initialize it with a lambda expression or instance.");
        }

        String fieldName = field.getName();
        try {
            AsyncCommandAction unifiedAction = UnifyUtils.getUnifiedCommandAction(fieldVal);

            context.registerOperation(builder -> builder.addConditionalEdges(sourceNodeId, unifiedAction, routeMap),
                    "add ConditionalEdges from source node '%s' (field: %s)", sourceNodeId, fieldName);
        } catch (IllegalArgumentException e) {
            throw new GraphDefinitionException(String.format(
                    "Field '%s' type [%s] for @ConditionalEdge is not supported. Must be one of: [EdgeAction, AsyncEdgeAction, CommandAction, AsyncCommandAction]",
                    fieldName, fieldVal.getClass().getSimpleName()), e);
        }
    }

    protected void handleCompileConfig(CompileContext context, Field field, GraphCompileConfig annotation) {
        if (context.hasCompileConfig()) {
            throw new IllegalStateException("Multiple @GraphCompileConfig fields found in " + context.composerInstance().getClass().getSimpleName());
        }

        ReflectionUtils.makeAccessible(field);
        Object value = ReflectionUtils.getField(field, context.composerInstance());

        if (value == null) {
            value = resolveMissingField(field, null);
        }

        if (value == null) {
            throw new GraphDefinitionException("@GraphCompileConfig field '" + field.getName() + "' must not be null.");
        }

        if (value instanceof CompileConfig) {
            context.setCompileConfig((CompileConfig) value);
        } else if (value instanceof Supplier) {
            Object suppliedValue = ((Supplier<?>) value).get();
            if (suppliedValue instanceof CompileConfig) {
                context.setCompileConfig((CompileConfig) suppliedValue);
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

    /**
     * Extension point for handling fields that are not annotated with any of the
     * framework's recognized graph annotations.
     * <p>
     * The default implementation only logs that the field was ignored. Subclasses
     * may override this method to process custom annotations or additional field
     * types that are specific to their own usage.
     *
     * @param context the current compile context for the graph being built
     * @param field   the field that did not match any known graph annotation
     */
    protected void handleOtherField(CompileContext context, Field field) {
        if (field.getAnnotations().length == 0) {
            return;
        }
        log.debug("Field '{}' is not annotated with recognized graph annotations.", field.getName());
    }

    /**
     * Extension point for handling null fields annotated with graph-related annotations.
     *
     * @param field the field that is null
     * @return an object to use in place of the null field, or null to indicate no substitution
     */
    @Nullable
    protected Object resolveMissingField(Field field, @Nullable String candidateName) {
        return null;
    }

}