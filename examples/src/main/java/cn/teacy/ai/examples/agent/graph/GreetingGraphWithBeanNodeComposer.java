package cn.teacy.ai.examples.agent.graph;

import cn.teacy.ai.annotation.GraphComposer;
import cn.teacy.ai.annotation.GraphKey;
import cn.teacy.ai.annotation.GraphNode;
import cn.teacy.ai.examples.agent.node.GreetingNode;
import com.alibaba.cloud.ai.graph.StateGraph;

@GraphComposer
public class GreetingGraphWithBeanNodeComposer {

    @GraphKey
    public static final String KEY_INPUT = "name";

    @GraphKey
    public static final String KEY_OUTPUT = "result";

    private static final String NODE_GREETING = "greetingNode";

    @GraphNode(id = NODE_GREETING, isStart = true, next = StateGraph.END)
    private GreetingNode greetingNode;

}
