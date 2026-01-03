package cn.teacy.ai;

import cn.teacy.ai.annotation.*;
import cn.teacy.ai.core.ReflectiveGraphBuilder;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class ReflectiveGraphBuilderTest {

    private ReflectiveGraphBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new ReflectiveGraphBuilder();
    }

    @Test
    @DisplayName("Build Routed Graph")
    void buildRoutedGraph() {
        RoutedGraphComposer composer = new RoutedGraphComposer();
        CompiledGraph graph = builder.build(composer);

        assertThat(graph).isNotNull();

        OverAllState state = graph.invoke(Map.of("query", "2c")).orElseThrow();
        assertThat(state.value(RoutedGraphComposer.KEY_RESULT).orElse("")).isEqualTo("c");

        state = graph.invoke(Map.of("query", "2b")).orElseThrow();
        assertThat(state.value(RoutedGraphComposer.KEY_RESULT).orElse("")).isEqualTo("b");
    }

    @GraphComposer(id = "routed_graph_composer")
    static class RoutedGraphComposer {

        @GraphKey(value = "query", internal = false)
        public static final String KEY_QUERY = "query";

        @GraphKey(internal = false)
        public static final String KEY_RESULT = "result";

        private static final String NODE_B = "nodeB";
        private static final String NODE_C = "nodeC";

        @ConditionalEdge(source = StateGraph.START, mappings = {"b", NODE_B, "c", NODE_C})
        public String route(OverAllState state) {
            String query = state.value(KEY_QUERY).orElseThrow().toString();
            return query.contains("b") ? "b" : "c";
        }

        @GraphNode(id = NODE_B, next = StateGraph.END)
        final AsyncNodeAction nodeB = state -> CompletableFuture.completedFuture(Map.of(KEY_RESULT, "b"));

        @GraphNode(id = NODE_C, next = StateGraph.END)
        final AsyncNodeAction nodeC = state -> CompletableFuture.completedFuture(Map.of(KEY_RESULT, "c"));

    }

}