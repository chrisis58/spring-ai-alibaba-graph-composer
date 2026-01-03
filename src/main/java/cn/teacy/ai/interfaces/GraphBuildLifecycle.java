package cn.teacy.ai.interfaces;

import com.alibaba.cloud.ai.graph.StateGraph;

public interface GraphBuildLifecycle {

    default void beforeCompile(StateGraph builder) {}

}
