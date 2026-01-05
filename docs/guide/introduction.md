# 简介

**SAA Graph Composer** 是为 Spring AI Alibaba Graph 量身打造的**声明式编排扩展库**。

它将复杂的底层流式构建转化为**注解驱动开发**，让定义 AI Agent 流程像编写 Spring Controller 一样简单、直观且符合工程直觉。

## 💡 为什么需要它？

在使用原生 Spring AI Alibaba Graph 开发复杂的 Agent 流程时，你可能会遇到以下问题：

* **构建逻辑繁琐**：大量的 `builder.addNode()`, `builder.addEdge()` 过程式代码堆叠在构建方法中，导致核心业务流程难以直观呈现，维护成本高。
* **关注点混合**：节点的具体业务逻辑与图的结构定义交织在一起，既降低了代码的可读性，也增加了复用难度。
* **集成体验非原生**：缺乏对 Spring 容器的深度支持，无法像管理普通 Service Bean 那样便捷地进行自动装配与配置注入。

## 核心特性

1. **声明式开发**：通过注解直观定义节点职责与流转路径，而无需在代码中堆砌 `addNode()` 和 `addEdge()`。
2. **Spring 深度集成**：遵循 Spring 标准开发模式。图定义类与编译后的图实例均被托管为标准 Bean，支持原生依赖注入与配置。
3. **灵活的混合构建**：针对复杂的动态连线，可通过生命周期钩子访问底层 API 补全逻辑，实现静态定义与动态构建的结合。
4. **多样的编译方式**：既支持通过 `@EnableGraphComposer` 自动扫描注册，也支持在不启动 Spring 上下文的情况下利用 `IGraphBuilder` 手动构建。

## 代码对比

看看它如何简化你的代码：

### 原生 API

你需要手动管理节点注册和连线逻辑：

```java
@Bean
public CompiledGraph helloGraph() {
    StateGraph builder = new StateGraph(() -> {
        Map<String, KeyStrategy> map = new HashMap<>();
        initialState.put("greeting", new ReplaceStrategy());
        return map;
    });

    NodeAction helloAction = state ->
            Map.of("greeting", "Hello, Spring AI Alibaba!");

    builder.addNode("hello", AsyncNodeAction.node_async(helloAction))
            .addEdge(StateGraph.START, "hello")
            .addEdge("hello", StateGraph.END);

    return builder.compile();
}

```

### Composer 方式

逻辑一目了然，结构即代码：

```java
@GraphComposer
public class HelloGraphComposer {

    @GraphKey
    public static final String KEY_GREETING = "greeting";

    @GraphNode(id = "hello", isStart = true, next = StateGraph.END)
    final NodeAction helloAction = state ->
            Map.of(KEY_GREETING, "Hello, Graph Composer!");
}

```

## 🤝 与 Spring AI Alibaba 的关系

SAA Graph Composer **不是** 替代品，而是 **增强包**。

* 它的底层完全基于 Spring AI Alibaba 的 `StateGraph` 构建。
* 生成的 `CompiledGraph` 是原生的对象，你可以无缝使用原生的所有功能（如 `invoke`, `stream`）。
* 你可以随时通过 `GraphBuildLifecycle` 访问到底层 API。

<div style="text-align: right; margin-top: 2rem; font-size: 1rem;">
  <strong>准备好动手了吗？</strong> <br/>
  继续阅读下一章：快速开始 👇
</div>