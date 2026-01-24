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

将 `saa-graph-composer` 添加到你的项目中。

::: code-group

```xml [Maven]
<dependency>
    <groupId>cn.teacy.ai</groupId>
    <artifactId>saa-graph-composer</artifactId>
    <version>0.2.2</version>
</dependency>

```

```groovy [Gradle]
implementation 'cn.teacy.ai:saa-graph-composer:0.2.2'

```

:::

## 3. 编写业务逻辑 (Service)

我们倡导 **关注点分离**。请根据你的场景选择以下一种方式定义业务逻辑。

:::code-group

```java [Adaptor]
@Service
public class GreetingService {

    public String generateGreeting(String name) {
        // 模拟一个耗时的 AI 或业务操作
        return "Hello, " + name + "! Welcome to SAA Graph Composer.";
    }
}

```

```java [Spring Bean]
@Component
public class GreetingNode implements AsyncNodeAction {
    
    // 这里使用与 Composer 中相同的常量，确保一致性
    private static final String KEY_INPUT = HelloWorldGraphComposer.KEY_INPUT;
    private static final String KEY_OUTPUT = HelloWorldGraphComposer.KEY_OUTPUT;

    public String generateGreeting(String name) {
        // 模拟一个耗时的 AI 或业务操作
        return "Hello, " + name + "! Welcome to SAA Graph Composer.";
    }

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state) {
        String name = (String) state.value(KEY_INPUT).orElse("World");
        String result = this.generateGreeting(name);
        return CompletableFuture.completedFuture(Map.of(KEY_OUTPUT, result));
    }
}
```
:::

## 4. 编写图编排 (Composer)

现在，我们使用 **声明式注解** 来组装这个图。 现在 `HelloWorldGraphComposer` 类充当了 **路由层** 的角色。

:::code-group

```java [Adaptor]
// 通过 targetBeanName 定义目标 Bean 名称，方便在其他地方注入。
@GraphComposer(targetBeanName = "helloWorldGraph")
public class HelloWorldGraphComposer {

    // 定义图状态的键
    @GraphKey
    public static final String KEY_INPUT = "name";

    @GraphKey
    public static final String KEY_OUTPUT = "result";

    // 定义节点 ID 常量
    private static final String NODE_GREETING = "greetingNode";

    // 使用 Adaptor 模式，在 Composer 内部编写节点逻辑
    @GraphNode(id = NODE_GREETING, isStart = true, next = StateGraph.END)
    public AsyncNodeAction sayHello = state -> {
        // 使用常量提取参数
        String name = (String) state.value(KEY_INPUT).orElse("World");

        // 委托 Service 执行业务
        String result = greetingService.generateGreeting(name);

        // 返回异步结果
        return CompletableFuture.completedFuture(Map.of(KEY_OUTPUT, result));
    };

    // 处理依赖注入
    private final GreetingService greetingService;

    public HelloWorldGraphComposer(GreetingService greetingService) {
        this.greetingService = greetingService;
    }
}

```

```java [Spring Bean]
// 通过 targetBeanName 定义目标 Bean 名称，方便在其他地方注入。
@GraphComposer(targetBeanName = "helloWorldGraph")
public class HelloWorldGraphComposer {

    // 定义图状态的键
    @GraphKey
    public static final String KEY_INPUT = "name";

    @GraphKey
    public static final String KEY_OUTPUT = "result";

    // 定义节点 ID 常量
    private static final String NODE_GREETING = "greetingNode";

    // 这里不初始化字段，框架会自动使用 GreetingNode Bean
    @GraphNode(id = NODE_GREETING, isStart = true, next = StateGraph.END)
    private GreetingNode greetingNode;

}
```
:::

::: tip ✨ 最佳实践：如何选择？
- **Adaptor Mode**：适合简单的逻辑组装。代码紧凑，直观。
- **Bean Reference**：适合复杂的业务场景。利用 Spring 容器管理节点生命周期，实现编排与执行的彻底解耦。
:::
::: tip ✨ 最佳实践
虽然直接使用字符串（如 "greetingNode"）也能工作，但我们强烈建议定义 static final 常量。这样做不仅能避免拼写错误，还能让 Composer 类成为一份自解释的图状态文档。 
:::

## 5. 启用配置

在你的 Spring Boot 启动类或配置类上添加 `@EnableGraphComposer` 注解，以启动图扫描与自动注册功能。

```java
@SpringBootApplication
@EnableGraphComposer // 添加注解
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}

```

::: tip 🔍 扫描范围
默认情况下，框架会扫描启动类所在的包及其子包下的 `@GraphComposer` 组件。如果你的组件定义在其他包路径下，请确保它们能被 Spring 上下文扫描到。 
:::

## 6. 运行与测试

`saa-graph-composer` 会自动扫描 `@GraphComposer` 注解，并将编译好的图注册为 Spring Bean。你可以直接注入并运行它。

```java
@SpringBootTest
public class GraphTest {

    @Autowired
    // 注入时使用注解中定义的 ID
    // 由于 CompiledGraph 实例是动态生成的，所以 IDE 可能在此处会提示找不到 Bean，实际运行时不会有问题。
    @Qualifier("helloWorldGraph")
    private CompiledGraph graph;

    @Test
    public void testRun() throws Exception {
        // 1. 准备初始输入
        Map<String, Object> input = Map.of("name", "Developer");

        // 2. 执行图
        OverAllState state = graph.invoke(input).orElseThrow();

        // 3. 验证结果
        System.out.println("输出结果: " + state.value("result").orElse("无结果"));
        // Output: Hello, Developer! Welcome to SAA Graph Composer.
    }
}

```

## 下一步

恭喜！你已经成功运行了第一个声明式 Graph。

但这只是开始，接下来你可以探索更多功能：

* **[注解详解](../reference/configuration)**: 查阅 `@GraphComposer` 和 `@GraphNode` 等的所有参数。
