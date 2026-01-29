package cn.teacy.ai.tests.other;

import cn.teacy.ai.annotation.GraphComposer;
import cn.teacy.ai.annotation.GraphNode;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.NodeAction;

import java.util.Collections;

@GraphComposer(id = "otherWorkflow", description = "just a other workflow for test")
public class OtherWorkflow {

    @GraphNode(id = "onlyNode", isStart = true, next = StateGraph.END)
    final NodeAction action = state -> Collections.emptyMap();

    @OtherAnnotation
    private static final String OTHER_ANNOTATED_VALUE = "other annotated value";

}
