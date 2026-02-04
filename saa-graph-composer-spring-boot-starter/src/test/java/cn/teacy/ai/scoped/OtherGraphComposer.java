package cn.teacy.ai.scoped;

import cn.teacy.ai.annotation.GraphComposer;
import cn.teacy.ai.annotation.GraphKey;
import cn.teacy.ai.annotation.GraphNode;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.NodeAction;

import java.util.Map;

@GraphComposer
public class OtherGraphComposer {

    @GraphKey
    public static final String KEY_OUTPUT = "output";

    @GraphNode(id = "node", isStart = true, next = StateGraph.END)
    NodeAction action = (state) -> Map.of(KEY_OUTPUT, "other graph output");

}
