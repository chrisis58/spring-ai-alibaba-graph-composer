package cn.teacy.ai.tests.scoped;

import cn.teacy.ai.annotation.EnableGraphComposer;
import cn.teacy.ai.annotation.GraphComposer;
import cn.teacy.ai.annotation.GraphNode;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Configuration
@EnableGraphComposer
public class TestGraphConfig {

    @Bean("testGraphComposer")
    public TestGraphComposer testGraphComposer() {
        return new TestGraphComposer();
    }

    @GraphComposer
    public static class TestGraphComposer {
        @GraphNode(id = "start", next = StateGraph.END, isStart = true)
        final AsyncNodeAction start = state -> CompletableFuture.completedFuture(Map.of("k", "v"));
    }

}
