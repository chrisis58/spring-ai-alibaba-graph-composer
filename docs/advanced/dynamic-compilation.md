# 手动与动态编译

虽然 `@EnableGraphComposer` 提供的自动扫描和注册功能非常便捷，但在某些场景下，你可能需要绕过这一机制，手动控制图的编译过程。

主要适用场景包括：

1. **显式配置**：为了消除 IDE 的自动注入告警，或获得更强的类型安全支持。
2. **单元测试**：在不启动 Spring 上下文的情况下快速测试图逻辑。

## 核心接口：GraphCompiler

`GraphCompiler` 是框架的核心构建器接口。它的职责是将一个包含 `@GraphComposer` 注解的 Java 对象，解析并编译为可执行的 `CompiledGraph`。

```java
public interface GraphCompiler {
    /**
     * 将图定义对象编译为可执行图
     * @param graphComposer 包含 @GraphComposer 注解的对象实例
     * @return 编译后的可执行图
     */
    CompiledGraph compile(Object graphComposer);
}

```

## 显式配置 Bean (Spring 环境)

如果你希望完全消除 IDE 关于 "Could not autowire" 的误报，或者希望明确控制 Bean 的依赖关系，可以使用显式配置模式替代 `@EnableGraphComposer`。

### 1. 移除自动配置并禁用自动编译

首先，确保你的启动类上**没有**标注 `@EnableGraphComposer`。

另外，如果你使用的是 Spring Boot Starter，可以通过添加以下配置来禁用自动编译：

```yaml
spring:
  ai:
    graph-composer:
      auto-compile: false
```

### 2. 定义配置类

创建一个 `@Configuration` 类，注入框架自动提供的 `GraphCompiler`，并手动注册每一个图。

```java
@Configuration
public class GraphConfig {

    private GraphCompiler graphCompiler;

    public GraphConfig(GraphCompiler graphCompiler) {
        // 注入框架默认的构建器实现
        this.graphCompiler = graphCompiler;
    }

    /**
     * 手动注册 LogAnalysisGraph。
     * 优势：
     * 1. 方法名即 Bean Name，IDE 能完美识别。
     * 2. 支持点击跳转和重构。
     */
    @Bean
    public CompiledGraph logAnalysisGraph(LogAnalysisGraphComposer composer) {
        // 实例化图定义
        return graphCompiler.compile(composer);
    }

    @Bean
    public CompiledGraph otherGraph() {
        return graphCompiler.compile(new OtherGraphComposer());
    }
}

```

### 3. 使用

现在，你可以安全地在其他服务中注入这些 Bean，IDE 将提供完善的代码提示和类型检查。

```java
@Service
public class MyService {
    // ✅ IDE 不会再报错，可以准确找到 Bean 定义
    public MyService(@Qualifier("logAnalysisGraph") CompiledGraph graph) { ... }
}

```

## 单元测试 (非 Spring 环境)

在编写单元测试时，启动整个 Spring Boot 上下文通常比较耗时。由于 `GraphCompiler` 的逻辑通常不依赖 Spring 容器，你可以直接实例化它来测试图逻辑。

### 示例代码

假设你有一个图定义类 `MyWorkflow`：

```java
@GraphComposer("test_flow")
public class MyWorkflow {
    // ... 节点定义 ...
}

```

编写 JUnit 测试：

```java
import cn.teacy.ai.core.GraphCompiler;
import cn.teacy.ai.core.ReflectiveGraphCompiler;
import org.junit.jupiter.api.Test;

import java.util.Map;

class MyWorkflowTest {

    // 1. 直接 new 一个构建器 (无需 Spring)
    // 注意：请使用框架提供的具体实现类
    private final GraphCompiler graphCompiler = new ReflectiveGraphCompiler();

    @Test
    void testFlowExecution() {
        // 2. 准备图定义对象 (它只是一个普通的 Java 对象)
        MyWorkflow workflow = new MyWorkflow();

        // 3. 手动编译
        CompiledGraph graph = graphCompiler.compile(workflow);

        // 4. 执行与断言
        Map<String, Object> input = Map.of("data", "test");
        Map<String, Object> result = graph.invoke(input);

        // 断言结果...
    }
}

```

::: tip 💡 提示
手动编译模式下，`@GraphComposer` 上的 `targetBeanName` 等 Spring 相关属性将被忽略，因为这些属性仅在 Bean 注册阶段生效。手动编译只关注图的结构和执行逻辑。
:::