package cn.teacy.ai.example.agent.node;

import cn.teacy.ai.example.agent.extend.interfaces.GraphModule;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.Map;

import static cn.teacy.ai.example.agent.graph.ExtendExampleGraphComposer.KEY_LOGS;

// #region snippet
@Component
public class DemoModule implements GraphModule {
    @Override
    public Map<String, NodeAction> namedNodes() {
        return Map.of(
                "node_a", (state) -> Map.of(KEY_LOGS, "Executed node_a"),
                "node_b", (state) -> Map.of(KEY_LOGS, "Executed node_b")
        );
    }
}
// #endregion snippet
