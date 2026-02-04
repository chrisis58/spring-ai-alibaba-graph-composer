package cn.teacy.ai.examples.agent.graph;

import cn.teacy.ai.annotation.GraphComposer;
import cn.teacy.ai.annotation.GraphNode;
import cn.teacy.ai.interfaces.GraphBuildLifecycle;
import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

// #region snippet
@GraphComposer
public class HybridGraphComposer implements GraphBuildLifecycle {

    private static final Logger log = LoggerFactory.getLogger(HybridGraphComposer.class);

    public static final String NODE_A = "nodeA";

    // 1. 使用注解定义节点，但故意省略 next 和 isStart 属性
    // 框架会自动注册此节点，但暂时不建立任何连接
    @GraphNode(id = NODE_A)
    final AsyncNodeActionWithConfig actionA = (state, config) -> {
        String input = (String) state.value("input").orElse("");
        return CompletableFuture.completedFuture(Map.of("result", input + "-processed"));
    };

    @Override
    public void beforeCompile(StateGraph builder) throws GraphStateException {
        // 2. 在钩子中手动补充连线逻辑
        // 逻辑：Start -> NodeA -> End
        builder.addEdge(StateGraph.START, NODE_A);
        builder.addEdge(NODE_A, StateGraph.END);

        log.info(String.valueOf(builder.getGraph(GraphRepresentation.Type.MERMAID)));
    }

}
// #endregion snippet
