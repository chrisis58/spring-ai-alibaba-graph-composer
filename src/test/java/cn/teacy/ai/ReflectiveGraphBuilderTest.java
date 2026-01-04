package cn.teacy.ai;

import cn.teacy.ai.annotation.*;
import cn.teacy.ai.core.ReflectiveGraphBuilder;
import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
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

        @GraphKey(value = "query")
        public static final String KEY_QUERY = "query";

        @GraphKey
        public static final String KEY_RESULT = "result";

        private static final String NODE_B = "nodeB";
        private static final String NODE_C = "nodeC";

        @ConditionalEdge(source = StateGraph.START, mappings = {"b", NODE_B, "c", NODE_C})
        final EdgeAction routingEdge = (state) -> {
            String query = state.value(KEY_QUERY).orElseThrow().toString();
            return query.contains("b") ? "b" : "c";
        };

        @GraphNode(id = NODE_B, next = StateGraph.END)
        final AsyncNodeAction nodeB = state -> CompletableFuture.completedFuture(Map.of(KEY_RESULT, "b"));

        @GraphNode(id = NODE_C, next = StateGraph.END)
        final AsyncNodeAction nodeC = state -> CompletableFuture.completedFuture(Map.of(KEY_RESULT, "c"));

        @GraphCompileConfig
        final CompileConfig config = CompileConfig.builder().build();

    }

    @Test
    @DisplayName("Linear Chain (A -> B -> End) with Constructor Init")
    void buildLinearGraph() {
        LinearGraphComposer composer = new LinearGraphComposer();
        CompiledGraph graph = builder.build(composer);

        OverAllState state = graph.invoke(Map.of("input", "start")).orElseThrow();

        assertThat(state.value("step1").isPresent()).isTrue();
        assertThat(state.value("step2").isPresent()).isTrue();
        assertThat(state.value("final_result").orElse("")).isEqualTo("start-processed-finished");
    }

    @GraphComposer
    static class LinearGraphComposer {
        private static final String NODE_A = "NodeA";
        private static final String NODE_B = "NodeB";

        @GraphKey
        private static final String KEY_INPUT = "input";
        @GraphKey
        private static final String KEY_RES = "final_result";

        @GraphNode(id = NODE_A, isStart = true, next = NODE_B)
        final AsyncNodeAction actionA;

        @GraphNode(id = NODE_B, next = StateGraph.END)
        final AsyncNodeAction actionB;

        @GraphCompileConfig
        final CompileConfig config;

        public LinearGraphComposer() {
            this.actionA = state -> {
                String val = (String) state.value(KEY_INPUT).orElse("");
                return CompletableFuture.completedFuture(Map.of("step1", true, KEY_RES, val + "-processed"));
            };

            this.actionB = state -> {
                String val = (String) state.value(KEY_RES).orElse("");
                return CompletableFuture.completedFuture(Map.of("step2", true, KEY_RES, val + "-finished"));
            };

            this.config = CompileConfig.builder().build();
        }
    }

    @Test
    @DisplayName("Looping & Append Strategy")
    void buildLoopGraph() {
        LoopGraphComposer composer = new LoopGraphComposer();
        CompiledGraph graph = builder.build(composer);

        OverAllState state = graph.invoke(Map.of("count", 0)).orElseThrow();

        Integer finalCount = (Integer) state.value("count").orElse(0);
        assertThat(finalCount).isEqualTo(3);

        Object logsObj = state.value("logs").orElse(null);
        assertThat(logsObj).isInstanceOf(List.class);
        List<?> logs = (List<?>) logsObj;
        assertThat(logs).hasSize(3);
        assertThat(logs.get(0)).isEqualTo("loop-0");
        assertThat(logs.get(1)).isEqualTo("loop-1");
        assertThat(logs.get(2)).isEqualTo("loop-2");
    }

    @GraphComposer
    static class LoopGraphComposer {
        private static final String NODE_PROCESS = "process";
        private static final String NODE_CHECK = "check";

        @GraphKey
        private static final String KEY_COUNT = "count";

        @GraphKey(strategy = AppendStrategy.class)
        private static final String KEY_LOGS = "logs";

        @GraphNode(id = NODE_PROCESS, isStart = true, next = NODE_CHECK)
        final AsyncNodeAction process = state -> {
            Integer count = (Integer) state.value(KEY_COUNT).orElse(0);
            return CompletableFuture.completedFuture(Map.of(
                    KEY_LOGS, "loop-" + count
            ));
        };

        @GraphNode(id = NODE_CHECK)
        final AsyncNodeAction check = state -> {
            Integer count = (Integer) state.value(KEY_COUNT).orElse(0);
            return CompletableFuture.completedFuture(Map.of(KEY_COUNT, count + 1));
        };

        @ConditionalEdge(source = NODE_CHECK, mappings = {"loop", NODE_PROCESS, "end", StateGraph.END})
        final EdgeAction checkLoop = state -> {
            Integer count = (Integer) state.value(KEY_COUNT).orElse(0);
            return count < 3 ? "loop" : "end";
        };

        @GraphCompileConfig
        final CompileConfig config = CompileConfig.builder().build();
    }

}