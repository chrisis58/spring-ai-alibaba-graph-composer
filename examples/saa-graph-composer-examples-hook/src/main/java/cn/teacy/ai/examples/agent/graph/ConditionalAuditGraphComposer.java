package cn.teacy.ai.examples.agent.graph;

import cn.teacy.ai.annotation.GraphComposer;
import cn.teacy.ai.annotation.GraphKey;
import cn.teacy.ai.annotation.GraphNode;
import cn.teacy.ai.interfaces.GraphBuildLifecycle;
import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;

@GraphComposer
public class ConditionalAuditGraphComposer implements GraphBuildLifecycle {

    private static final Logger log = LoggerFactory.getLogger(ConditionalAuditGraphComposer.class);

    @GraphKey
    public static final String KEY_VALUE = "value";

    public static final String NODE_PROCESS = "nodeProcess";
    public static final String NODE_AUDIT = "nodeAudit";

    @GraphNode(id = NODE_PROCESS, isStart = true)
    NodeAction nodeProcess = (state) -> {
        String value = state.value(KEY_VALUE, "__VALUE__");
        return Map.of(KEY_VALUE, "(" + value + ")");
    };

    NodeAction nodeAudit = (state) -> {
        String value = state.value(KEY_VALUE, "__VALUE__");
        log.info("Auditing value: {}", value);
        return Map.of(KEY_VALUE, "{" + value + "}");
    };

    // #region snippet

    @Value("${app.audit.enabled:false}")
    private boolean auditEnabled;

    @Override
    public void beforeCompile(StateGraph builder) throws GraphStateException {

        if (auditEnabled) {
            // If auditing is enabled, add the audit node between process and end
            builder.addNode(NODE_AUDIT, AsyncNodeAction.node_async(nodeAudit));
            builder.addEdge(NODE_PROCESS, NODE_AUDIT);
            builder.addEdge(NODE_AUDIT, StateGraph.END);
        } else {
            // Directly connect process node to end
            builder.addEdge(NODE_PROCESS, StateGraph.END);
        }

        log.info(String.valueOf(builder.getGraph(GraphRepresentation.Type.MERMAID)));
    }
    // #endregion snippet
}
