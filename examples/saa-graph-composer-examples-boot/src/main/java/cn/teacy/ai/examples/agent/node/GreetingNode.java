package cn.teacy.ai.examples.agent.node;

import cn.teacy.ai.examples.agent.graph.GreetingGraphWithBeanNodeComposer;
import cn.teacy.ai.examples.service.GreetingService;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.Map;

// #region snippet
@Component
public class GreetingNode implements NodeAction {

    private static final String KEY_INPUT = GreetingGraphWithBeanNodeComposer.KEY_INPUT;
    private static final String KEY_OUTPUT = GreetingGraphWithBeanNodeComposer.KEY_OUTPUT;

    private final GreetingService greetingService;

    public GreetingNode(GreetingService greetingService) {
        this.greetingService = greetingService;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String someone = state.value(KEY_INPUT, "world");
        String greet = greetingService.greet(someone);
        return Map.of(KEY_OUTPUT, greet);
    }

}
// #region snippet
