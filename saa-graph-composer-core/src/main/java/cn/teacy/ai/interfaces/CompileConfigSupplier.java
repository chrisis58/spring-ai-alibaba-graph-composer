package cn.teacy.ai.interfaces;

import com.alibaba.cloud.ai.graph.CompileConfig;

import java.util.function.Supplier;

@FunctionalInterface
public interface CompileConfigSupplier extends Supplier<CompileConfig> {

    CompileConfig get();

}
