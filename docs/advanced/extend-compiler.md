# 扩展 Graph Compiler

::: warning 版本要求
本章节内容适用于 `saa-graph-composer` 版本 `0.2.1` 及以上。
:::

`ReflectiveGraphCompiler` 是本框架的核心编译器，负责将用户定义的 Composer 对象解析为可运行的 `CompiledGraph`。

该编译器基于**模板方法模式**设计。 虽然核心编译流程是固定的，但它暴露了一系列 `protected` 的钩子方法，允许开发者通过继承的方式介入并修改特定阶段的行为。

通过扩展 Graph Compiler，你可以实现：

1. **自定义节点类型**：让图构建器支持除 `NodeAction` 标准接口之外的任意业务对象。
2. **自定义注解**：引入新的声明式注解来简化配置。

## 1. 核心扩展机制

要扩展编译器，你需要创建一个继承自 `ReflectiveGraphCompiler` 的子类。编译器通过 `protected` 修饰符暴露了关键的钩子方法，允许子类介入字段解析过程。

以下是可供重写或调用的核心扩展点：

| 方法 | 可见性 | 描述 |
| --- | --- | --- |
| `handleGraphNode` | `protected` | **扩展节点类型**。专门用于处理带有 `@GraphNode` 注解的字段。 |
| `handleOtherField` | `protected` | **扩展自定义注解**。处理未被框架标准注解（如 `@GraphNode`, `@GraphKey`）标记的字段。 |

**构建上下文 (CompileContext)**

子类通过 `CompileContext` 记录来与编译流程交互，它是扩展逻辑的核心载体。

```java
protected static class CompileContext {

    /** 获取当前正在编译的 Composer 对象实例 */
    public Object composerInstance() { ... }

    /** 检查是否已设置编译配置 */
    public boolean hasCompileConfig() { ... }

    /** 设置编译配置 */
    public void setCompileConfig(CompileConfig config) { ... }
    
    /** 检查 key 是否已经存在 */
    public boolean containsKey(String key) { ... }

    /** 注册 Key 策略 */
    public void addKeyStrategy(String key, KeyStrategy strategy, String fieldName) { ... }

    /**
     * 注册图构建操作
     * @param op          操作逻辑 (Lambda)
     * @param errorFormat 错误描述模板 (例如 "add node '%s' (field: %s)")
     * @param args        错误描述参数
     */
    public void registerOperation(GraphOperation op, String errorFormat, Object... args) { ... }
}

@FunctionalInterface
protected interface GraphOperation {
    void execute(StateGraph builder) throws GraphStateException;
}

```

::: info 关于 `GraphOperation`
扩展逻辑的主要任务调用 `context#registerOperation` 来注册 `GraphOperation`。这些操作不会立即执行，而是在解析完图的定义后，在 `compile` 方法的最后阶段统一应用到 `StateGraph` 构建器上。
:::

## 2. 示例一：支持批量节点注册

该示例演示如何通过一个自定义接口 `GraphModule`，将一组节点批量注册到图中，而不是手动定义每一个 `@GraphNode`。

**第一步：定义接口**

```java
public interface GraphModule {
    /**
     * 返回一组需要注册的节点
     * Key: 节点 ID
     * Value: 节点逻辑
     */
    Map<String, NodeAction> namedNodes();
}

```

**第二步: 扩展编译器处理逻辑**

```java
public class MyCustomCompiler extends ReflectiveGraphCompiler {

    @Override
    protected void handleGraphNode(CompileContext context, Field field, GraphNode anno) {
        // 1. 获取字段值
        ReflectionUtils.makeAccessible(field);
        Object instance = ReflectionUtils.getField(field, context.composerInstance());

        // 2. 识别 GraphModule 接口
        if (instance instanceof GraphModule module) {
            context.registerOperation(builder -> {
                Map<String, NodeAction> nodes = module.namedNodes();
                if (nodes == null || nodes.isEmpty()) {
                    return;
                }

                // 3.1 批量注册模块内的所有节点
                nodes.forEach((nodeId, action) -> {
                    // 如果 action 类型复杂，可以使用 UnifyUtils 进行转换
                    builder.addNode(nodeId, AsyncNodeAction.node_async(action));
                });

                // 3.2 处理模块入口连线
                if (anno.isStart() && StringUtils.hasText(anno.id())) {
                    builder.addEdge(StateGraph.START, anno.id());
                }

            }, "register GraphModule nodes from field '%s'", field.getName());

            // 4. 处理完毕，阻断父类逻辑
            return;
        }

        // 5. 非模块类型，务必回退给父类处理标准类型
        super.handleGraphNode(context, field, anno);
    }
}

```

::: tip 最佳实践
`context#registerOperation` 方法是向编译上下文注册构建操作的推荐方式。它额外接收一个字符串模板以及参数，用于生成更具描述性的错误信息。
:::

## 3. 示例二：支持自定义注解

除了扩展节点，你还可以引入全新的注解来改变图的构建方式。

**示例目标**：引入一个 `@GraphEdge` 注解，允许用户通过字段定义边，将“边的连接”与“节点的定义”解耦。

**第一步：定义注解**

```java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GraphEdge {
    /** 边的起始节点 ID */
    String source();
}

```

**第二步：扩展编译器处理逻辑**

重写 `handleOtherField` 方法来拦截并处理这个注解。

```java
public class MyFullFeatureCompiler extends ReflectiveGraphCompiler {

    @Override
    protected void handleOtherField(CompileContext context, Field field) {
        // 1. 检查字段是否包含自定义注解
        if (field.isAnnotationPresent(GraphEdge.class)) {
            handleCustomGraphEdge(context, field, field.getAnnotation(GraphEdge.class));
            return; // 处理完毕，阻断父类逻辑
        }

        // 2. 对于不认识的字段，必须调用父类（父类会记录 Debug 日志）
        super.handleOtherField(context, field);
    }

    private void handleCustomGraphEdge(CompileContext context, Field field, GraphEdge anno) {
        // 1. 校验字段类型 (约定字段值必须是目标节点ID)
        if (field.getType() != String.class) {
            throw new IllegalArgumentException(
                String.format("Field '%s' annotated with @GraphEdge must be of type String.", field.getName()));
        }

        ReflectionUtils.makeAccessible(field);

        // 2. 获取字段值（即 Target Node ID）
        Object value = ReflectionUtils.getField(field, context.composerInstance());
        String targetId = (String) value;

        if (!StringUtils.hasText(targetId)) {
            throw new IllegalArgumentException(
                String.format("Field '%s' must provide a valid target node ID.", field.getName()));
        }

        String sourceId = anno.source();

        // 3. 核心步骤：向 Context 添加操作指令
        // 这些指令会在 compile() 的最终阶段执行
        context.registerOperation(builder -> {
            builder.addEdge(sourceId, targetId);
        }, "add custom edge from '%s' to '%s' (field: %s)", sourceId, targetId, field.getName());
    }
}

```

::: tip 最佳实践
`context#registerOperation` 方法是向编译上下文注册构建操作的推荐方式。它额外接收一个字符串模板以及参数，用于生成更具描述性的错误信息。
:::

## 4. 综合使用示例

完成上述扩展后，你可以在 Composer 中混合使用标准功能和自定义功能。

**实现一个简单的模块**

```java
public class DemoModule implements GraphModule {
    @Override
    public Map<String, NodeAction> namedNodes() {
        return Map.of(
                "node_a", (state) -> { System.out.println("A"); return Map.of(); },
                "node_b", (state) -> { System.out.println("B"); return Map.of(); }
        );
    }
}

```

**在 Composer 中使用**

```java
@GraphComposer
public class SimpleComposer {

    // --- 批量注册 ---
    // 这将自动注册 "node_a" 和 "node_b"
    // 指定 id="node_a" 配合 isStart=true，让编译器自动连接 START -> node_a
    @GraphNode(id = "node_a", isStart = true)
    public GraphModule myModule = new DemoModule();

    // --- 标准节点 ---
    @GraphNode(id = "node_c")
    public NodeAction nodeC = (state) -> Map.of();

    // --- 自定义注解连线 ---

    // node_a -> node_b (模块内部连线)
    @GraphEdge(source = "node_a")
    private final String link1 = "node_b";

    // node_b -> node_c (模块连向外部)
    @GraphEdge(source = "node_b")
    private final String link2 = "node_c";

    // node_c -> END
    @GraphEdge(source = "node_c")
    private final String link3 = StateGraph.END;
}

```

## 5. 覆盖默认的 Graph Compiler

如果你定义了自定义的编译器，必须将其注册为 Spring Bean 以替换框架默认的 `ReflectiveGraphCompiler`。

```java
@Configuration
public class MyGraphConfig {

    // ✅ 推荐：显式指定 Bean Name 常量
    @Bean(ComposerConfigConstants.GRAPH_COMPILER_BEAN_NAME)
    public GraphCompiler myCustomCompiler() {
        return new MyFullFeatureCompiler();
    }
    
    // ✅ 或者：方法名直接叫 graphCompiler
    // @Bean
    // public GraphCompiler graphCompiler() { ... }

    // ❌ 错误：Bean 名称默认为 "customCompiler"
    // 框架会检测到 "graphCompiler" 缺失，从而再次创建默认编译器。
    // 结果：你的自定义逻辑不会生效。
    // @Bean
    // public GraphCompiler customCompiler() {
    //     return new MyFullFeatureCompiler();
    // }
}

```

::: warning ⚠️ 重要提示：Bean 名称必须匹配

本框架的 AutoConfiguration 通过 Bean 名称 `graphCompiler` 来判断是否需要创建默认编译器。因此，在覆盖默认实现时，**必须**将 Bean 的名称指定为 `graphCompiler`（建议使用常量 `ComposerConfigConstants.GRAPH_COMPILER_BEAN_NAME`）。

**如果 Bean 名称不匹配，框架将无法识别你的自定义编译器，并会继续使用默认实现。**
:::
