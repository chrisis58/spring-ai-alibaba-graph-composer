# 核心库集成

如果你正在维护一个传统的 **Spring Framework** 项目（非 Spring Boot），或者你需要对 `saa-graph-composer` 进行更底层的控制，你可以直接集成核心库。

与 Starter 不同，核心库不包含自动配置机制，你需要显式地启用功能。

## 1. 引入依赖

仅引入核心库，不包含 Spring Boot 相关的依赖。

::: code-group

```xml [Maven]
<dependency>
    <groupId>cn.teacy.ai</groupId>
    <artifactId>saa-graph-composer</artifactId>
    <version>0.3.0</version>
</dependency>
```

```groovy [Gradle]
implementation 'cn.teacy.ai:saa-graph-composer:0.3.0'
```

:::

## 2. 显式启用

在你的 Spring 配置类（标注了 `@Configuration` 的类）上，必须添加 `@EnableGraphComposer` 注解。
这是核心库工作的开关，它负责注册 `GraphAutoRegistrar` 以处理图的扫描与编译。

<ExampleWrapper path="saa-graph-composer-examples-core/src/main/java/cn/teacy/ai/examples/config/ExampleConfig.java">

<<< @/../examples/saa-graph-composer-examples-core/src/main/java/cn/teacy/ai/examples/config/ExampleConfig.java#snippet{java}

</ExampleWrapper>

## 3. 运行与调用

在非 Spring Boot 环境下，你需要使用 `AnnotationConfigApplicationContext` 或 `ClassPathXmlApplicationContext` 来启动 Spring 容器。

启动后，你可以像获取普通 Bean 一样获取编译好的 `CompiledGraph`。

<ExampleWrapper path="saa-graph-composer-examples-core/src/main/java/cn/teacy/ai/examples/ExamplesMainApplication.java">

<<< @/../examples/saa-graph-composer-examples-core/src/main/java/cn/teacy/ai/examples/ExamplesMainApplication.java#snippet{java}

</ExampleWrapper>

## 4. 手动配置 (进阶)

如果你不希望使用 `@EnableGraphComposer` 的自动扫描机制，或者你需要自定义 `GraphCompiler` 的行为（例如传入特殊的 `ClassLoader` 或自定义 Bean 获取逻辑），你可以选择手动定义 Bean。

只需在配置类中注册一个 `GraphCompiler` 类型的 Bean 即可。

<ExampleWrapper path="saa-graph-composer-examples-core/src/main/java/cn/teacy/ai/examples/config/ManualExampleConfig.java">

<<< @/../examples/saa-graph-composer-examples-core/src/main/java/cn/teacy/ai/examples/config/ManualExampleConfig.java#snippet{java}

</ExampleWrapper>

一旦 GraphCompiler Bean 存在于容器中，你可以手动触发编译，或者监听 Spring 的 ContextRefreshedEvent 事件来处理图的注册。但在大多数场景下，使用 `@EnableGraphComposer` 是最简单且推荐的方式。

<ExampleWrapper path="saa-graph-composer-examples-core/src/main/java/cn/teacy/ai/examples/ManualExampleMainApplication.java">

<<< @/../examples/saa-graph-composer-examples-core/src/main/java/cn/teacy/ai/examples/ManualExampleMainApplication.java#snippet{java}

</ExampleWrapper>