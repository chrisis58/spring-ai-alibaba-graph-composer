package cn.teacy.ai.examples.agent.node;

import cn.teacy.ai.examples.agent.graph.GreetingGraphWithBeanNodeComposer;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.Map;

// #region snippet
@Component
public class GreetingNode implements NodeAction {

    private static final String KEY_INPUT = GreetingGraphWithBeanNodeComposer.KEY_INPUT;
    private static final String KEY_OUTPUT = GreetingGraphWithBeanNodeComposer.KEY_OUTPUT;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String someone = state.value(KEY_INPUT, "world");
        String greet = "Hello, " + someone + "! Welcome to SAA Graph Composer.";
        return Map.of(KEY_OUTPUT, greet);
    }

}
// #endregion snippet
