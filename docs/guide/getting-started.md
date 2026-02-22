# 快速开始

只需几分钟，你就能将第一个声明式 AI Agent 工作流运行起来。

本指南将带你构建一个最基础的 "Hello World" ，将展示两种推荐的实现模式：

1. **Adaptor 模式**：在 Composer 中直接编写 Lambda 表达式作为胶水代码，适合简单的参数转换。
2. **Bean 引用模式** <Badge type="tip" text="0.2.2+" vertical="middle" />：将节点逻辑完全剥离为独立的 Spring Bean，Composer 仅负责引用，适合复杂的业务逻辑。

## 1. 环境准备

在开始之前，请确保你的开发环境满足以下要求：

* **JDK**: 17 或更高版本
* **Spring Boot**: 3.x
* **Spring AI Alibaba**: 1.1.0.0

::: warning ⚠️ 不支持的版本
- Spring AI Alibaba: 1.0.0.x
:::

## 2. 引入依赖

从 **0.3.0** 版本开始，我们推荐使用 Spring Boot Starter 来快速接入。

::: code-group

```xml [Maven]
<dependency>
    <groupId>cn.teacy.ai</groupId>
    <artifactId>saa-graph-composer-spring-boot-starter</artifactId>
    <version>0.3.1</version>
</dependency>

```

```groovy [Gradle]
implementation 'cn.teacy.ai:saa-graph-composer-spring-boot-starter:0.3.1'

```

:::

::: tip 💡 非 Spring Boot 项目？
如果你正在使用纯 Spring Framework 或需要手动集成核心库，请参阅 [核心库集成](../advanced/core-library.md)。
:::

## 3. 编写业务逻辑 (Service)

我们倡导 **关注点分离**。请根据你的场景选择以下一种方式定义业务逻辑。

<ExampleTabs>
<ExampleWrapper path="saa-graph-composer-examples-boot/src/main/java/cn/teacy/ai/examples/service/GreetingService.java" label="Adaptor">

<<< @/../examples/saa-graph-composer-examples-boot/src/main/java/cn/teacy/ai/examples/service/GreetingService.java#snippet{java}

</ExampleWrapper>
<ExampleWrapper path="saa-graph-composer-examples-boot/src/main/java/cn/teacy/ai/examples/agent/node/GreetingNode.java" label="Bean Ref">

<<< @/../examples/saa-graph-composer-examples-boot/src/main/java/cn/teacy/ai/examples/agent/node/GreetingNode.java#snippet{java}

</ExampleWrapper>

</ExampleTabs>

## 4. 编写图编排 (Composer)

现在，我们使用 **声明式注解** 来组装这个图。 现在 `HelloWorldGraphComposer` 类充当了 **路由层** 的角色。

<ExampleTabs>
<ExampleWrapper path="saa-graph-composer-examples-boot/src/main/java/cn/teacy/ai/examples/agent/graph/GreetingGraphWithAdaptorNodeComposer.java" label="Adaptor">

<<< @/../examples/saa-graph-composer-examples-boot/src/main/java/cn/teacy/ai/examples/agent/graph/GreetingGraphWithAdaptorNodeComposer.java#snippet{java}

</ExampleWrapper>
<ExampleWrapper path="saa-graph-composer-examples-boot/src/main/java/cn/teacy/ai/examples/agent/graph/GreetingGraphWithBeanNodeComposer.java" label="Bean Ref">

<<< @/../examples/saa-graph-composer-examples-boot/src/main/java/cn/teacy/ai/examples/agent/graph/GreetingGraphWithBeanNodeComposer.java#snippet{java}

</ExampleWrapper>

</ExampleTabs>

::: tip ✨ 最佳实践：如何选择？
- **Adaptor Mode**：适合简单的逻辑组装。代码紧凑，直观。
- **Bean Reference**：适合复杂的业务场景。利用 Spring 容器管理节点生命周期，实现编排与执行的彻底解耦。
:::
::: tip ✨ 最佳实践
虽然直接使用字符串（如 "greetingNode"）也能工作，但我们强烈建议定义 static final 常量。这样做不仅能避免拼写错误，还能让 Composer 类成为一份自解释的图状态文档。 
:::

## 5. 运行与测试

`saa-graph-composer` 会自动扫描 `@GraphComposer` 注解，并将编译好的图注册为 Spring Bean。你可以直接注入并运行它。

<ExampleTabs>
<ExampleWrapper path="saa-graph-composer-examples-boot/src/test/java/cn/teacy/ai/examples/GreetingGraphDemoTest.java" label="Adaptor">

<<< @/../examples/saa-graph-composer-examples-boot/src/test/java/cn/teacy/ai/examples/GreetingGraphDemoTest.java#adaptor{java}

</ExampleWrapper>
<ExampleWrapper path="saa-graph-composer-examples-boot/src/test/java/cn/teacy/ai/examples/GreetingGraphDemoTest.java" label="Bean Ref">

<<< @/../examples/saa-graph-composer-examples-boot/src/test/java/cn/teacy/ai/examples/GreetingGraphDemoTest.java#beanRef{java}

</ExampleWrapper>
</ExampleTabs>

## 下一步

恭喜！你已经成功运行了第一个声明式 Graph。

但这只是开始，接下来你可以探索更多功能：

- **[图定义](../reference/graph-definition)**: 查阅 `@GraphComposer` 和 `@GraphNode` 等用于定义图的注解。
