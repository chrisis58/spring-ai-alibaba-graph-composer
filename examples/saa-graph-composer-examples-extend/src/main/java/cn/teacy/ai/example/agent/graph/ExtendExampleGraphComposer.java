package cn.teacy.ai.example.agent.graph;

import cn.teacy.ai.example.agent.extend.annotation.GraphEdge;
import cn.teacy.ai.example.agent.extend.interfaces.GraphModule;
import cn.teacy.ai.annotation.GraphComposer;
import cn.teacy.ai.annotation.GraphKey;
import cn.teacy.ai.annotation.GraphNode;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;

import java.util.Map;

// #region snippet
@GraphComposer
public class ExtendExampleGraphComposer {

    @GraphKey(strategy = AppendStrategy.class)
    public static final String KEY_LOGS = "logs";

    // --- 批量注册 ---
    // 这将自动注册 "node_a" 和 "node_b"
    // 指定 id="node_a" 配合 isStart=true，让编译器自动连接 START -> node_a
    @GraphNode(id = "node_a", isStart = true)
    public GraphModule demoModule;

    // --- 标准节点 ---
    @GraphNode(id = "node_c")
    public NodeAction nodeC = (state) -> Map.of(KEY_LOGS, "Executed node_c");

    // --- 自定义注解连线 ---

    // node_a -> node_b
    @GraphEdge(source = "node_a")
    private final String link1 = "node_b";

    // node_b -> node_c
    @GraphEdge(source = "node_b")
    private final String link2 = "node_c";

    // node_c -> END
    @GraphEdge(source = "node_c")
    private final String link3 = StateGraph.END;

}
// #endregion snippet
