package cn.teacy.ai.example.agent.extend.interfaces;

import com.alibaba.cloud.ai.graph.action.NodeAction;

import java.util.Map;

// #region snippet
public interface GraphModule {
    /**
     * 返回一组需要注册的节点
     * Key: 节点 ID
     * Value: 节点逻辑
     */
    Map<String, NodeAction> namedNodes();
}
// #endregion snippet
