package cn.teacy.ai.examples.agent.graph;

import cn.teacy.ai.annotation.GraphComposer;
import cn.teacy.ai.annotation.GraphKey;
import cn.teacy.ai.annotation.GraphNode;
import cn.teacy.ai.examples.service.GreetingService;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.NodeAction;

import java.util.Map;

// #region snippet
/**
 * Adaptor 模式：适合简单的逻辑组装。
 * 在构造函数中编写 Lambda 表达式作为胶水代码，代码紧凑、直观。
 */
@GraphComposer
public class GreetingGraphWithAdaptorNodeComposer {

    @GraphKey
    public static final String KEY_INPUT = "name";

    @GraphKey
    public static final String KEY_OUTPUT = "result";

    private static final String NODE_GREETING = "greetingNode";

    @GraphNode(id = NODE_GREETING, isStart = true, next = StateGraph.END)
    private final NodeAction greetingNode;

    public GreetingGraphWithAdaptorNodeComposer(
            GreetingService greetingService
    ) {
        this.greetingNode = (state) -> {
            // 提取参数并委托给 Service
            String name = state.value(KEY_INPUT, "World");
            String result = greetingService.greet(name);
            // 返回更新的状态
            return Map.of(KEY_OUTPUT, result);
        };
    }

}
// #endregion snippet