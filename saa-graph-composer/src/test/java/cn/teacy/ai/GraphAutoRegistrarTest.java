package cn.teacy.ai;

import cn.teacy.ai.constants.ComposerConfigConstants;
import cn.teacy.ai.core.GraphCompiler;
import cn.teacy.ai.core.SpringReflectiveGraphCompiler;
import cn.teacy.ai.tests.another.AnotherTestGraphConfig;
import cn.teacy.ai.tests.scoped.TestGraphConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class GraphAutoRegistrarTest {

    @Configuration
    public static class GraphCompilerConfiguration {

        @Bean
        public GraphCompiler graphCompiler(ConfigurableListableBeanFactory beanFactory) {
            return new SpringReflectiveGraphCompiler(beanFactory);
        }

    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(GraphCompilerConfiguration.class);

    @Test
    @DisplayName("Auto Configuration should work with default settings")
    void testDefaultConfiguration() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(GraphCompiler.class);
        });
    }

    @Configuration
    static class UserConfig {
        @Bean(ComposerConfigConstants.GRAPH_COMPILER_BEAN_NAME)
        public GraphCompiler graphCompiler() {
            return new CustomGraphCompiler();
        }
    }

    static class CustomGraphCompiler implements GraphCompiler {
        @Override
        public CompiledGraph compile(Object graphComposer) {
            return null;
        }
    }

    @Test
    @DisplayName("Should automatically register CompiledGraph bean for @GraphComposer beans")
    void testAutoRegistration() {
        runner.withUserConfiguration(TestGraphConfig.class)
                .run(context -> {
                    assertThat(context).hasBean("testGraphComposer");

                    assertThat(context).hasSingleBean(CompiledGraph.class);

                    CompiledGraph graph = context.getBean(CompiledGraph.class);
                    assertThat(graph).isNotNull();

                    assertThat(graph.invoke(Map.of()).isPresent()).isTrue();
                });
    }

    @Test
    @DisplayName("Should automatically register GraphCompiler and CompiledGraph bean for @GraphComposer beans")
    void testAutoRegistrationWithDefaultGraphCompiler() {
        ApplicationContextRunner rawRunner = new ApplicationContextRunner();

        rawRunner.withUserConfiguration(TestGraphConfig.class)
                .run(context -> {
                    assertThat(context).hasBean("graphCompiler");

                    assertThat(context).hasBean("testGraphComposer");

                    assertThat(context).hasSingleBean(CompiledGraph.class);

                    CompiledGraph graph = context.getBean(CompiledGraph.class);
                    assertThat(graph).isNotNull();

                    assertThat(graph.invoke(Map.of()).isPresent()).isTrue();
                });
    }

    @Test
    void testAutoRegistrationWithComposerInAnotherPackage() {
        runner.withUserConfiguration(AnotherTestGraphConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(CompiledGraph.class);

                    CompiledGraph graph = context.getBean(CompiledGraph.class);
                    assertThat(context).hasBean("otherWorkflowCompiled");
                    assertThat(graph).isNotNull();

                    assertThat(graph.invoke(Map.of()).isPresent()).isTrue();
                });
    }

}
