package cn.teacy.ai;

import cn.teacy.ai.annotation.ConditionalEdge;
import cn.teacy.ai.annotation.GraphComposer;
import cn.teacy.ai.annotation.GraphKey;
import cn.teacy.ai.annotation.GraphNode;
import cn.teacy.ai.config.SaaGraphComposerAutoConfiguration;
import cn.teacy.ai.core.GraphCompiler;
import cn.teacy.ai.exception.GraphDefinitionException;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@ExtendWith(OutputCaptureExtension.class)
public class SpringReflectiveGraphCompilerTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SaaGraphComposerAutoConfiguration.class));

    private static final String OUTPUT = "output";

    @Test
    @DisplayName("Null fields in GraphComposer should be injected from Spring context")
    void testSpringBeanInjectionInGraphComposer() {
        runner.withUserConfiguration(TestConfiguration.class)
                .run(context -> {
                    GraphCompiler compiler = context.getBean(GraphCompiler.class);
                    IndexedByNodeIdSpringContextGraphComposer composer = new IndexedByNodeIdSpringContextGraphComposer();
                    var compiledGraph = compiler.compile(composer);

                    var state = compiledGraph.invoke(Map.of()).orElseThrow();
                    Optional<Object> output = state.value(OUTPUT);
                    Assertions.assertThat(output).isPresent();

                    Assertions.assertThat(output.get())
                            .asInstanceOf(InstanceOfAssertFactories.list(String.class))
                            .hasSize(4)
                            .containsExactlyInAnyOrder(
                                    "nodeA executed",
                                    "nodeB executed",
                                    "asyncNodeA executed",
                                    "asyncNodeB executed");
                });
    }


    @GraphComposer
    static class IndexedByNodeIdSpringContextGraphComposer {

        @GraphKey(strategy = AppendStrategy.class)
        public static final String KEY_OUTPUT = OUTPUT;

        @GraphNode(id = "nodeA", isStart = true, next = "nodeB")
        NodeAction nodeA;

        @GraphNode(id = "nodeB", next = "asyncNodeA")
        NodeAction nodeB;

        @GraphNode(id = "asyncNodeA", next = "asyncNodeB")
        AsyncNodeAction asyncNodeA;

        @GraphNode(id = "asyncNodeB", next = StateGraph.END)
        AsyncNodeAction asyncNodeB;

    }

    @Test
    @DisplayName("Null fields in GraphComposer should be injected from Spring context (indexed by field name)")
    void testSpringBeanInjectionInGraphComposerIndexedByFieldName() {
        runner.withUserConfiguration(TestConfiguration.class)
                .run(context -> {
                    GraphCompiler compiler = context.getBean(GraphCompiler.class);
                    IndexedByFieldNameSpringContextGraphComposer composer = new IndexedByFieldNameSpringContextGraphComposer();
                    var compiledGraph = compiler.compile(composer);

                    var state = compiledGraph.invoke(Map.of()).orElseThrow();
                    Optional<Object> output = state.value(OUTPUT);
                    Assertions.assertThat(output).isPresent();

                    Assertions.assertThat(output.get())
                            .asInstanceOf(InstanceOfAssertFactories.list(String.class))
                            .hasSize(4)
                            .containsExactlyInAnyOrder(
                                    "nodeA executed",
                                    "nodeB executed",
                                    "asyncNodeA executed",
                                    "asyncNodeB executed");
                });
    }

    @GraphComposer
    static class IndexedByFieldNameSpringContextGraphComposer {

        @GraphKey(strategy = AppendStrategy.class)
        public static final String KEY_OUTPUT = OUTPUT;

        @GraphNode(id = "a", isStart = true, next = "b")
        NodeAction nodeA;

        @GraphNode(id = "b", next = "aa")
        NodeAction nodeB;

        @GraphNode(id = "aa", next = "ab")
        AsyncNodeAction asyncNodeA;

        @GraphNode(id = "ab")
        AsyncNodeAction asyncNodeB;

        @ConditionalEdge(source = "ab", mappings = {StateGraph.END, StateGraph.END})
        EdgeAction edgeA;

    }

    @Test
    @DisplayName("Null fields in GraphComposer should be injected from Spring context (indexed by type)")
    void testSpringBeanInjectionInGraphComposerIndexedByType() {
        runner.withUserConfiguration(TestConfiguration.class)
                .run(context -> {
                    GraphCompiler compiler = context.getBean(GraphCompiler.class);
                    IndexByTypeSpringContextGraphComposer composer = new IndexByTypeSpringContextGraphComposer();
                    var compiledGraph = compiler.compile(composer);

                    var state = compiledGraph.invoke(Map.of()).orElseThrow();
                    Optional<Object> output = state.value(OUTPUT);
                    Assertions.assertThat(output).isPresent();

                    Assertions.assertThat(output.get())
                            .asInstanceOf(InstanceOfAssertFactories.list(String.class))
                            .hasSize(1)
                            .containsExactlyInAnyOrder("nodeA executed");
                });
    }

    @GraphComposer
    static class IndexByTypeSpringContextGraphComposer {

        @GraphKey(strategy = AppendStrategy.class)
        public static final String KEY_OUTPUT = OUTPUT;

        @GraphNode(id = "a", isStart = true)
        NodeAction nodeA;

        @ConditionalEdge(source = "a", mappings = {StateGraph.END, StateGraph.END})
        EdgeAction ae;

    }

    @Test
    @DisplayName("When multiple beans of the same type exist, an error should be logged")
    void testSpringBeanInjectionWithNoUniqueBean(CapturedOutput output) {
        runner.withUserConfiguration(TestConfiguration.class)
                .run(context -> {
                    GraphCompiler compiler = context.getBean(GraphCompiler.class);
                    NoUniqueBeanGraphComposer composer = new NoUniqueBeanGraphComposer();

                    Assertions.assertThatThrownBy(() -> compiler.compile(composer))
                            .isInstanceOf(GraphDefinitionException.class)
                            .hasCauseInstanceOf(IllegalStateException.class)
                            .satisfies(e -> {
                                Assertions.assertThat(e.getCause())
                                        .hasMessageContaining("is null");
                            });

                    Assertions.assertThat(output.getOut())
                            .contains("Multiple beans of type")
                            .contains("found in ApplicationContext");

                    Assertions.assertThat(output.getOut())
                            .contains("NoUniqueBeanDefinitionException");
                });
    }

    @GraphComposer
    static class NoUniqueBeanGraphComposer {

        @GraphNode(id = "a", isStart = true, next = StateGraph.END)
        NodeAction a;

    }

    @Test
    @DisplayName("When no bean definition found in context, an error should be logged")
    void testSpringBeanInjectionWithNoBeanDefinition(CapturedOutput output) {
        runner.withUserConfiguration(TestConfiguration.class)
                .run(context -> {
                    GraphCompiler compiler = context.getBean(GraphCompiler.class);
                    NoBeanDefinitionGraphComposer composer = new NoBeanDefinitionGraphComposer();

                    Assertions.assertThatThrownBy(() -> compiler.compile(composer))
                            .isInstanceOf(GraphDefinitionException.class)
                            .hasCauseInstanceOf(IllegalStateException.class)
                            .satisfies(e -> {
                                Assertions.assertThat(e.getCause())
                                        .hasMessageContaining("is null");
                            });

                    Assertions.assertThat(output.getOut())
                            .contains("No bean of type")
                            .contains("found in ApplicationContext");

                    Assertions.assertThat(output.getOut())
                            .contains("NoSuchBeanDefinitionException");
                });
    }

    @GraphComposer
    static class NoBeanDefinitionGraphComposer {

        @GraphNode(id = "node", isStart = true, next = StateGraph.END)
        AsyncNodeActionWithConfig node;

    }

    @Test
    @DisplayName("Spring bean injection should fallback to field name when id do not match")
    void testSpringBeanInjectionWithMismatchedType() {
        runner.withUserConfiguration(TestConfiguration.class)
                .run(context -> {
                    GraphCompiler compiler = context.getBean(GraphCompiler.class);
                    IndexedByNodeIdWithMismatchedTypeSpringContextGraphComposer composer = new IndexedByNodeIdWithMismatchedTypeSpringContextGraphComposer();
                    var compiledGraph = compiler.compile(composer);

                    var state = compiledGraph.invoke(Map.of()).orElseThrow();
                    Optional<Object> output = state.value(OUTPUT);
                    Assertions.assertThat(output).isPresent();

                    Assertions.assertThat(output.get())
                            .asInstanceOf(InstanceOfAssertFactories.list(String.class))
                            .hasSize(1)
                            .containsExactlyInAnyOrder("asyncNodeA executed");
                });
    }

    @GraphComposer
    static class IndexedByNodeIdWithMismatchedTypeSpringContextGraphComposer {

        @GraphKey(strategy = AppendStrategy.class)
        public static final String KEY_OUTPUT = OUTPUT;

        @GraphNode(id = "nodeA", isStart = true, next = StateGraph.END)
        AsyncNodeAction asyncNodeA;

    }

    @Test
    @DisplayName("Spring bean injection should fallback to type when id and field name do not match")
    void testSpringBeanInjectionWithMismatchedIdAndFieldName() {
        runner.withUserConfiguration(TestConfiguration.class)
                .run(context -> {
                    GraphCompiler compiler = context.getBean(GraphCompiler.class);
                    IndexedByFieldNameWithMismatchedTypeSpringContextGraphComposer composer = new IndexedByFieldNameWithMismatchedTypeSpringContextGraphComposer();
                    var compiledGraph = compiler.compile(composer);

                    var state = compiledGraph.invoke(Map.of()).orElseThrow();
                    Optional<Object> output = state.value(OUTPUT);
                    Assertions.assertThat(output).isPresent();

                    Assertions.assertThat(output.get())
                            .asInstanceOf(InstanceOfAssertFactories.list(String.class))
                            .hasSize(1)
                            .containsExactlyInAnyOrder("asyncNodeA executed");
                });
    }

    @GraphComposer
    static class IndexedByFieldNameWithMismatchedTypeSpringContextGraphComposer {

        @GraphKey(strategy = AppendStrategy.class)
        public static final String KEY_OUTPUT = OUTPUT;

        @GraphNode(id = "nodeB", isStart = true, next = StateGraph.END)
        AsyncNodeAction nodeA;

    }

    @Configuration
    public static class TestConfiguration {

        @Bean
        public NodeAction nodeA() {
            return context -> Map.of(OUTPUT, "nodeA executed");
        }

        @Bean
        public NodeAction nodeB() {
            return context -> Map.of(OUTPUT, "nodeB executed");
        }

        @Bean
        @Primary
        public AsyncNodeAction asyncNodeA() {
            return context -> CompletableFuture.completedFuture(
                    Map.of(OUTPUT, "asyncNodeA executed")
            );
        }

        @Bean
        public AsyncNodeAction asyncNodeB() {
            return context -> CompletableFuture.completedFuture(
                    Map.of(OUTPUT, "asyncNodeB executed")
            );
        }

        @Primary
        @Bean
        public EdgeAction edgeA() {
            return (state) -> StateGraph.END;
        }

    }

}
