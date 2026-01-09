package cn.teacy.ai;

import cn.teacy.ai.config.SaaGraphComposerAutoConfiguration;
import cn.teacy.ai.constants.ComposerConfigConstants;
import cn.teacy.ai.core.GraphCompiler;
import cn.teacy.ai.tests.another.AnotherTestGraphConfig;
import cn.teacy.ai.tests.scoped.TestGraphConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class SaaGraphAutoConfigurationTest {


    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SaaGraphComposerAutoConfiguration.class));

    @Test
    @DisplayName("Auto Configuration should work with default settings")
    void testDefaultConfiguration() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(GraphCompiler.class);
        });
    }

    @Test
    @DisplayName("Bean should not be created when disabled via properties")
    void testDisabledConfiguration() {
        runner.withPropertyValues("spring.ai.graph-composer.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(GraphCompiler.class);
                });
    }

    @Test
    @DisplayName("Bean should be overridden by user-defined Bean")
    void testUserOverride() {
        runner.withUserConfiguration(UserConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(GraphCompiler.class);
                    assertThat(context.getBean(GraphCompiler.class))
                            .isInstanceOf(CustomGraphCompiler.class);
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
