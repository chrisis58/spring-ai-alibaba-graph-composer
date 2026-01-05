# 图定义

本部分详细介绍如何使用注解定义图的结构、节点逻辑、状态键管理以及流转规则。

## 1. 容器定义

### @GraphComposer

**目标**：`TYPE` (类)

将一个类标记为图蓝图。

| 属性 | 类型        | 默认值 | 说明                           |
| --- |-----------|--|------------------------------|
| `id` | `String`  | `""` | 图的唯一标识符                      |
| `description` | `String`  | `""` | (可选) 图的描述                    |
| `autoRegister` | `boolean` | `true` | 是否自动注册 CompiledGraph         |
| `targetBeanName` | `String`  | "" | 自动注册 CompileGraph 时的 Bean 名称 |

::: tip 💡 关于 `targetBeanName` 属性
如果这个属性留空，框架会根据一定的规则生成 Bean 名称，详情请参考 [Bean 注册与命名策略](../reference/configuration.md#_2-编译图-bean-的命名规则)。
:::

### @GraphCompileConfig

**目标**：`FIELD` (字段)

定义图的编译和运行配置，该字段的类型需要为 `CompileConfig` 或 `() -> CompileConfig`。

**示例**：

```java
@GraphComposer
public class MyGraph {
    // 定义配置项
    @GraphCompileConfig
    final CompileConfig config = CompileConfig.builder()
            .saverConfig(SaverConfig.builder()
                    .register(new MemorySaver())
                    .build())
            .build();
    // ...
}

```

## 2. 状态键定义

### @GraphKey

**目标**：`FIELD` (字段)

定义 State (上下文) 中使用的键。建议使用 `public static final String` 常量定义，以便在代码中引用。

| 属性 | 类型                             | 默认值                     | 说明        |
| --- |--------------------------------|-------------------------|-----------|
| `strategy` | `Class<? extends KeyStrategy>` | `ReplaceStrategy.class` | 值合并策略     |
| `internal` | `boolean`                      | `false`                 | 标记键是否是仅内部 |

::: tip 💡 关于 `internal` 属性
当前的版本中，`internal` 属性仅作为标记使用，不会影响图的编译或执行逻辑。未来版本可能会引入对内部键的特殊处理。
:::

**示例 (普通与追加模式)**：

```java
@GraphKey
public static final String KEY_INPUT = "input";

// 追加模式：多次写入会自动聚合为 List<Object>
@GraphKey(strategy = AppendStrategy.class)
public static final String KEY_LOGS = "logs"; 

```

## 3. 节点定义

### @GraphNode

**目标**：`FIELD` (字段)

定义一个执行节点。字段类型支持多种函数式接口（如 `NodeAction`, `AsyncNodeAction`）甚至另一个 `CompiledGraph`（子图）。

| 属性 | 类型 | 默认值 | 说明                  |
| --- | --- | --- |---------------------|
| `id` | `String` | `""` | 节点 ID，若为空则使用字段名     |
| `isStart` | `boolean` | `false` | 标记是否为起始节点，可以有多个起始节点 |
| `next` | `String[]` | `{}` | 指定后继节点 ID |      

#### 支持的字段类型

* `NodeAction`: 同步执行 `(OverallState) -> Map`
* `AsyncNodeAction`: 异步执行 `(OverallState) -> CompletableFuture<Map>`
* `NodeActionWithConfig`: 带配置的执行 `(OverallState, RunnableConfig) -> Map`
* `AsyncNodeActionWithConfig`: 带配置的异步执行 `(OverallState, RunnableConfig) -> CompletableFuture<Map>`
* `CompiledGraph`: **子图嵌套**，将另一个编译好的图作为一个节点执行。

**示例 1：串行流转 (A -> B -> End)**

```java
@GraphNode(id = "NodeA", isStart = true, next = "NodeB")
final NodeAction actionA = state -> Map.of("step", 1);

@GraphNode(id = "NodeB", next = StateGraph.END)
final NodeAction actionB = state -> Map.of("step", 2);

```

**示例 2：并行广播 (A -> [B, C])**

```java
// 执行完 NodeA 后，同时触发 NodeB 和 NodeC
@GraphNode(id = "NodeA", isStart = true, next = {"NodeB", "NodeC"})
final NodeAction actionA = state -> Map.of();

```

**示例 3：子图嵌套**

```java
@Autowired
private CompiledGraph subGraph; // 已存在的子图 Bean

// 将子图包装为当前图的一个节点
@GraphNode(id = "sub_flow", next = StateGraph.END)
final CompiledGraph nestedNode = subGraph;

```

## 4. 路由与条件边

### @ConditionalEdge

**目标**：`FIELD` (字段)

定义**条件流转逻辑**。该字段不代表节点，而是代表一条“智能的边”。它根据当前 State 计算出一个“指令字符串”，然后根据 `mappings` 路由到下一个节点。

| 属性 | 类型 | 默认值  | 说明                                                     |
| --- | --- |------|--------------------------------------------------------|
| `source` | `String` | -    | 源节点 ID,表示从哪个节点出来后执行此判断                                 |
| `mappings` | `String[]` | `{}` | 路由映射表,格式为 `{"指令1", "目标节点ID", "指令2", "目标节点ID"}` 的键值对数组 |

#### 支持的字段类型

* `EdgeAction`: 同步执行 `(OverallState) -> String`
* `AsyncEdgeAction`: 同步执行 `(OverallState) -> CompletableFuture<String>`
* `CommandAction`: 同步执行复杂指令 `(OverallState, RunnableConfig) -> Command`
* `AsyncCommandAction`: 异步执行复杂指令 `(OverallState, RunnableConfig) -> CompletableFuture<Command>`

**示例：基于内容的路由**

```java
public static final String NODE_B = "nodeB";
public static final String NODE_C = "nodeC";

// 逻辑：从 START 节点开始，检查 query 内容。
// 如果包含 "b" -> 返回 "b" -> 路由到 NODE_B
// 否则 -> 返回 "c" -> 路由到 NODE_C
@ConditionalEdge(
    source = StateGraph.START, 
    mappings = {
        "b", NODE_B, 
        "c", NODE_C
    }
)
final EdgeAction routingEdge = (state) -> {
    String query = (String) state.value("query").orElse("");
    return query.contains("b") ? "b" : "c";
};

```

## 5. 常见模式综合示例

### 循环模式 (Looping)

结合 `@ConditionalEdge` 和 `AppendStrategy` 实现循环计数逻辑。

```java
@GraphComposer
public class LoopGraph {
    private static final String NODE_PROCESS = "process";
    private static final String NODE_CHECK = "check";

    @GraphKey // 计数器
    private static final String KEY_COUNT = "count";

    @GraphKey(strategy = AppendStrategy.class) // 记录每次循环的日志
    private static final String KEY_LOGS = "logs";

    // 1. 执行任务，写入日志
    @GraphNode(id = NODE_PROCESS, isStart = true, next = NODE_CHECK)
    final NodeAction process = state -> {
        int count = (int) state.value(KEY_COUNT).orElse(0);
        return Map.of(KEY_LOGS, "log-" + count);
    };

    // 2. 计数器 +1
    @GraphNode(id = NODE_CHECK)
    final NodeAction check = state -> {
        int count = (int) state.value(KEY_COUNT).orElse(0);
        return Map.of(KEY_COUNT, count + 1);
    };

    // 3. 判断是否继续循环
    @ConditionalEdge(
        source = NODE_CHECK, 
        mappings = {"continue", NODE_PROCESS, "stop", StateGraph.END}
    )
    final EdgeAction checkLoop = state -> {
        int count = (int) state.value(KEY_COUNT).orElse(0);
        return count < 3 ? "continue" : "stop";
    };
    
    @GraphCompileConfig
    final CompileConfig config = CompileConfig.builder().build();
}

```