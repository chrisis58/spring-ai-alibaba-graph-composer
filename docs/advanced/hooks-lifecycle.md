# 生命周期钩子

除了静态的注解定义外，SAA Graph Composer 提供了 `GraphBuildLifecycle` 接口，允许你在图被最终编译成 `CompiledGraph` 之前，直接介入并修改底层的 `StateGraph` 构建器。

这为开发者提供了极大的灵活性，不仅可以解决注解无法表达的复杂连线问题，还能实现动态构建、结构校验等高级需求。

## 接口定义

实现该接口的 `@GraphComposer` 类将获得介入图构建流程的能力。框架提供了两个关键的生命周期钩子，允许在自动化编排的不同阶段注入自定义逻辑。

```java
public interface GraphBuildLifecycle {
    /**
     * 在所有的键注册完成后触发，即 new StateGraph() 之后，但在任何节点或连线添加之前。
     * 
     * @param builder 底层图构建器
     * @throws GraphStateException 如果构建逻辑有误
     */
    default void afterKeyRegistration(StateGraph builder) throws GraphStateException {}

    /**
     * 在编译前触发，暴露底层的 StateGraph 构建器。
     * 你可以在此方法中补充节点、添加连线或进行校验。
     *
     * @param builder 底层图构建器
     * @throws GraphStateException 如果构建逻辑有误
     */
    default void beforeCompile(StateGraph builder) throws GraphStateException {}
}

```

## 混合构建

**这是最常见的使用场景。**

当节点逻辑适合用 Spring Bean 管理（使用 `@GraphNode`），但连线逻辑过于复杂（例如动态条件、依赖配置）而不便在 `@GraphNode` 的 `next` 属性中硬编码时，可以采用“**注解定义节点 + 代码定义连线**”的混合模式。

### 示例代码

<ExampleWrapper path="saa-graph-composer-examples-hook/src/main/java/cn/teacy/ai/examples/agent/graph/HybridGraphComposer.java">

<<< @/../examples/saa-graph-composer-examples-hook/src/main/java/cn/teacy/ai/examples/agent/graph/HybridGraphComposer.java#snippet{java}

</ExampleWrapper>

::: tip 💡 图构建 API
关于 `addNode`, `addEdge`, `addConditionalEdge` 等方法的详细参数说明和更多高级用法，请参阅 [Spring AI Alibaba 官方文档](https://java2ai.com/docs/frameworks/graph-core/quick-start)。
:::

## 动态修剪与扩展

利用 `beforeCompile`，你可以根据环境变量、配置中心的配置或 License 授权情况，动态地调整图结构。

### 示例：根据配置开启审计节点

<ExampleWrapper path="saa-graph-composer-examples-hook/src/main/java/cn/teacy/ai/examples/agent/graph/ConditionalAuditGraphComposer.java">

<<< @/../examples/saa-graph-composer-examples-hook/src/main/java/cn/teacy/ai/examples/agent/graph/ConditionalAuditGraphComposer.java#snippet{java}

</ExampleWrapper>
