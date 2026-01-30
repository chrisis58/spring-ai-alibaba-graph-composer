# 扩展 Graph Compiler <Badge type="tip" text="0.2.1+" vertical="middle" />

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
    public void addKeyStrategy(String key, KeyStrategy strategy) { ... }

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
扩展逻辑的主要任务是调用 `context#registerOperation` 来注册 `GraphOperation`。这些操作不会立即执行，而是在解析完图的定义后，在 `compile` 方法的最后阶段统一应用到 `StateGraph` 构建器上。
:::

## 2. 示例一：支持批量节点注册

该示例演示如何通过一个自定义接口 `GraphModule`，将一组节点批量注册到图中，而不是手动定义每一个 `@GraphNode`。

**第一步：定义接口**

<ExampleWrapper path="saa-graph-composer-examples-extend/src/main/java/cn/teacy/ai/example/agent/extend/interfaces/GraphModule.java">

<<< @/../examples/saa-graph-composer-examples-extend/src/main/java/cn/teacy/ai/example/agent/extend/interfaces/GraphModule.java#snippet{java}

</ExampleWrapper>

**第二步: 扩展编译器处理逻辑**

<ExampleWrapper path="saa-graph-composer-examples-extend/src/main/java/cn/teacy/ai/example/agent/config/GraphComposerConfig.java">

<<< @/../examples/saa-graph-composer-examples-extend/src/main/java/cn/teacy/ai/example/agent/config/GraphComposerConfig.java#extendGraphNode{java}

</ExampleWrapper>

::: tip 最佳实践
`context#registerOperation` 方法是向编译上下文注册构建操作的推荐方式。它额外接收一个字符串模板以及参数，用于生成更具描述性的错误信息。
:::

## 3. 示例二：支持自定义注解

除了扩展节点，你还可以引入全新的注解来改变图的构建方式。

**示例目标**：引入一个 `@GraphEdge` 注解，允许用户通过字段定义边，将“边的连接”与“节点的定义”解耦。

**第一步：定义注解**

<ExampleWrapper path="saa-graph-composer-examples-extend/src/main/java/cn/teacy/ai/example/agent/extend/annotation/GraphEdge.java">

<<< @/../examples/saa-graph-composer-examples-extend/src/main/java/cn/teacy/ai/example/agent/extend/annotation/GraphEdge.java#snippet{java}

</ExampleWrapper>

**第二步：扩展编译器处理逻辑**

重写 `handleOtherField` 方法来拦截并处理这个注解。

<ExampleWrapper path="saa-graph-composer-examples-extend/src/main/java/cn/teacy/ai/example/agent/config/GraphComposerConfig.java">

<<< @/../examples/saa-graph-composer-examples-extend/src/main/java/cn/teacy/ai/example/agent/config/GraphComposerConfig.java#extendOtherField{java}

</ExampleWrapper>

::: tip 最佳实践
`context#registerOperation` 方法是向编译上下文注册构建操作的推荐方式。它额外接收一个字符串模板以及参数，用于生成更具描述性的错误信息。
:::

## 4. 综合使用示例

完成上述扩展后，你可以在 Composer 中混合使用标准功能和自定义功能。

**实现一个简单的模块**

<ExampleWrapper path="saa-graph-composer-examples-extend/src/main/java/cn/teacy/ai/example/agent/node/DemoModule.java">

<<< @/../examples/saa-graph-composer-examples-extend/src/main/java/cn/teacy/ai/example/agent/node/DemoModule.java#snippet{java}

</ExampleWrapper>

**在 Composer 中使用**

<ExampleWrapper path="saa-graph-composer-examples-extend/src/main/java/cn/teacy/ai/example/agent/graph/ExtendExampleGraphComposer.java">

<<< @/../examples/saa-graph-composer-examples-extend/src/main/java/cn/teacy/ai/example/agent/graph/ExtendExampleGraphComposer.java#snippet{java}

</ExampleWrapper>

## 5. 覆盖默认的 Graph Compiler

如果你定义了自定义的编译器，必须将其注册为 Spring Bean 以替换框架默认的 `ReflectiveGraphCompiler`。

<ExampleWrapper path="saa-graph-composer-examples-extend/src/main/java/cn/teacy/ai/example/agent/config/GraphComposerConfig.java">

<<< @/../examples/saa-graph-composer-examples-extend/src/main/java/cn/teacy/ai/example/agent/config/GraphComposerConfig.java#overrideCompiler{java}

</ExampleWrapper>

::: warning ⚠️ 重要提示：Bean 名称必须匹配

本框架的 AutoConfiguration 通过 Bean 名称 `graphCompiler` 来判断是否需要创建默认编译器。因此，在覆盖默认实现时，**必须**将 Bean 的名称指定为 `graphCompiler`（建议使用常量 `ComposerConfigConstants.GRAPH_COMPILER_BEAN_NAME`）。

**如果 Bean 名称不匹配，框架将无法识别你的自定义编译器，并会继续使用默认实现。**

::: details 示例
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
:::

## 6. 运行示例

你可以在 `saa-graph-composer-examples-extend` 示例项目中找到完整的扩展编译器实现与使用示例。运行该项目的测试类 `ExtendCompilerTest`，即可验证自定义注解和节点类型的功能是否生效。

<ExampleWrapper path="saa-graph-composer-examples-extend/src/test/java/cn/teacy/ai/example/ExtendCompilerTest.java">

<<< @/../examples/saa-graph-composer-examples-extend/src/test/java/cn/teacy/ai/example/ExtendCompilerTest.java#snippet{java}

</ExampleWrapper>