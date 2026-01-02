package cn.teacy.ai.core;

import com.alibaba.cloud.ai.graph.CompiledGraph;

public interface IGraphBuilder {

    CompiledGraph build(Object graphComposer);

}
