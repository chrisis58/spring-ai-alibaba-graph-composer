package cn.teacy.ai.examples.agent.graph;

import cn.teacy.ai.annotation.GraphComposer;
import cn.teacy.ai.annotation.GraphKey;
import cn.teacy.ai.annotation.GraphNode;
import cn.teacy.ai.examples.agent.node.GreetingNode;
import com.alibaba.cloud.ai.graph.StateGraph;

// #region snippet
/**
 * Bean 引用模式：适合复杂的业务场景。
 * 节点逻辑完全剥离为独立的 Spring Bean，Composer 仅负责声明结构。
 */
@GraphComposer
public class GreetingGraphWithBeanNodeComposer {

    @GraphKey
    public static final String KEY_INPUT = "name";

    @GraphKey
    public static final String KEY_OUTPUT = "result";

    private static final String NODE_GREETING = "greetingNode";

    // 框架会自动寻找并注入类型为 GreetingNode 的 Bean
    @GraphNode(id = NODE_GREETING, isStart = true, next = StateGraph.END)
    private GreetingNode greetingNode;

}
// #endregion snippet