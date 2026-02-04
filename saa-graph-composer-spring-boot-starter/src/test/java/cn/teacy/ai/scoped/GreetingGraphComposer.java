package cn.teacy.ai.scoped;

import cn.teacy.ai.annotation.GraphComposer;
import cn.teacy.ai.annotation.GraphKey;
import cn.teacy.ai.annotation.GraphNode;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.NodeAction;

import java.util.Map;

@GraphComposer
public class GreetingGraphComposer {

    @GraphKey
    public static final String KEY_INPUT = "name";

    @GraphKey
    public static final String KEY_OUTPUT = "result";

    private static final String NODE_GREETING = "greetingNode";

    @GraphNode(id = NODE_GREETING, isStart = true, next = StateGraph.END)
    private final NodeAction greetingNode = (state) -> {
        String name = state.value(KEY_INPUT, "World");
        return Map.of(KEY_OUTPUT, "Hello, " + name + "!");
    };

}
