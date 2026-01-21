# Spring 集成 <Badge type="tip" text="0.2.2+" />

拓展后的 `SpringReflectiveGraphCompiler` 允许你利用 Spring 容器来管理节点和边的具体实现。

### 核心机制
当编译器扫描 `@GraphComposer` 类时，对于被标记为图组件 (`@GraphNode`, `@ConditionalEdge` 或 `@GraphCompileConfig`) 且值为 `null` 的字段，编译器会尝试从 Spring 的 `ApplicationContext` 中查找匹配的 Bean 并自动填充。

::: warning 仅限图组件
对于普通的业务依赖（如 `UserService`），请继续使用 Spring 标准的构造函数注入或 `@Autowired`。
:::

### 使用示例

假设你有一个复杂的业务逻辑节点，被定义为一个独立的 Spring Bean：

**1. 定义节点 Bean**
```java
@Component("orderValidator") // Bean 名称
public class OrderValidatorNode implements AsyncNodeAction {
    
    @Autowired
    private OrderService orderService; // 节点内部依然可以使用标准的 Spring 注入

    @Override
    public CompletableFuture<Map<String, Object>> execute(State state) {
        // ... 复杂的校验逻辑 ...
        return CompletableFuture.completedFuture(result);
    }
}
```

**2. 在图中引用该 Bean**

在你的 `GraphComposer` 中，你不需要手动 `new` 这个节点，只需要声明字段并加上注解：

```java
@GraphComposer
public class OrderWorkflow {

    // 引用 Spring 容器中现有的 Bean
    // 编译器会自动找到名为 "orderValidator" 的 Bean 并注入
    @GraphNode(id = "orderValidator", next = "payment")
    private AsyncNodeAction orderValidator; 
    
    // 依然支持内联定义的简单节点
    @GraphNode(id = "payment", isStart = false)
    final AsyncNodeAction payment = state -> {
        System.out.println("Processing payment...");
        return CompletableFuture.completedFuture(Collections.emptyMap());
    };
}
```

### 解析规则

1. **第一级：候选名称匹配**

   * **条件**：调用方提供了候选名称（`candidateName`），且不为空。
   * **逻辑**：检查容器中是否存在名为 `candidateName` 的 Bean，且该 Bean 的类型与字段类型兼容。

2. **第二级：字段名称匹配**

   * **条件**：字段名与候选名称不同（或者没有候选名称）。
   * **逻辑**：检查容器中是否存在与 **Java 字段名** 同名的 Bean，且该 Bean 的类型与字段类型兼容。

3. **第三级：类型匹配**

   * **条件**：前两级均未命中。
   * **逻辑**：尝试在容器中查找该 **字段类型** 的 Bean。

::: tip 关于 `candidateName`
目前只有 `@GraphNode` 注解的 `id` 属性会被用作 `candidateName`。
:::

::: warning 找不到唯一匹配的情况
如果根据上述规则找不到**唯一**的匹配，Composer 将无法正确通过编译，抛出 `GraphDefinitionException` 异常。
:::

