package cn.teacy.ai.core;

import com.alibaba.cloud.ai.graph.CompiledGraph;

public interface GraphCompiler {

    CompiledGraph compile(Object graphComposer);

}
