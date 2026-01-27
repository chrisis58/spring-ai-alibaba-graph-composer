package cn.teacy.ai.examples.agent.graph;

import cn.teacy.ai.annotation.GraphComposer;
import cn.teacy.ai.annotation.GraphKey;
import cn.teacy.ai.annotation.GraphNode;
import cn.teacy.ai.examples.service.GreetingService;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.NodeAction;

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
            String someone = state.value(KEY_INPUT, "world");
            String greet = greetingService.greet(someone);
            return java.util.Map.of(KEY_OUTPUT, greet);
        };
    }

}
