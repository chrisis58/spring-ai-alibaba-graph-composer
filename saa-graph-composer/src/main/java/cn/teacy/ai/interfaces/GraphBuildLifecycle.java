package cn.teacy.ai.interfaces;

import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;

public interface GraphBuildLifecycle {

    default void afterKeyRegistration(StateGraph builder) throws GraphStateException {}

    default void beforeCompile(StateGraph builder) throws GraphStateException {}

}
