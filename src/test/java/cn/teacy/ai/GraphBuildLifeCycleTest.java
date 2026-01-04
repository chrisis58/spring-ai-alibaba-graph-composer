package cn.teacy.ai;


import cn.teacy.ai.annotation.GraphComposer;
import cn.teacy.ai.annotation.GraphKey;
import cn.teacy.ai.annotation.GraphNode;
import cn.teacy.ai.core.ReflectiveGraphBuilder;
import cn.teacy.ai.interfaces.GraphBuildLifecycle;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class GraphBuildLifeCycleTest {

    private ReflectiveGraphBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new ReflectiveGraphBuilder();
    }

    @DisplayName("Manual Build: Pure Lifecycle Implementation")
    @Test
    void manualBuildByGraphBuildLifecycle() {
        CompiledGraph compiledGraph = builder.build(new ManualGraphComposer());

        OverAllState state = compiledGraph.invoke(Map.of()).orElseThrow();

        assertThat(state.value(ManualGraphComposer.KEY_DATA))
                .isPresent()
                .get()
                .isInstanceOf(String.class)
                .isEqualTo("manual build success");
    }

    @GraphComposer
    static class ManualGraphComposer implements GraphBuildLifecycle {

        @GraphKey
        public static final String KEY_DATA = "data";

        public static final String NODE_ID = "node";

        final AsyncNodeActionWithConfig node;

        public ManualGraphComposer() {
            this.node = (state, config) ->
                CompletableFuture.completedFuture(Map.of(KEY_DATA, "manual build success"));
        }

        @Override
        public void beforeCompile(StateGraph builder) throws GraphStateException {
            builder.addNode(NODE_ID, node);
            builder.addEdge(StateGraph.START, NODE_ID);
            builder.addEdge(NODE_ID, StateGraph.END);
        }

    }

    @DisplayName("Hybrid Build: Annotation Node + Manual Edge Logic")
    @Test
    void hybridBuildTest() {
        HybridGraphComposer composer = new HybridGraphComposer();
        CompiledGraph compiledGraph = builder.build(composer);

        OverAllState state = compiledGraph.invoke(Map.of("input", "test")).orElseThrow();

        assertThat(state.value("result"))
                .isPresent()
                .get()
                .isEqualTo("test-processed");
    }

    @GraphComposer
    static class HybridGraphComposer implements GraphBuildLifecycle {

        public static final String NODE_A = "nodeA";

        @GraphNode(id = NODE_A) // absent isStart and next
        final AsyncNodeActionWithConfig actionA = (state, config) -> {
            String input = (String) state.value("input").orElse("");
            return CompletableFuture.completedFuture(Map.of("result", input + "-processed"));
        };

        @Override
        public void beforeCompile(StateGraph builder) throws GraphStateException {
            builder.addEdge(StateGraph.START, NODE_A);
            builder.addEdge(NODE_A, StateGraph.END);
        }
    }

}
