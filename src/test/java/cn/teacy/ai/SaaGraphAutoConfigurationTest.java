package cn.teacy.ai;

import cn.teacy.ai.config.SaaGraphComposerAutoConfiguration;
import cn.teacy.ai.core.GraphAutoRegistrar;
import cn.teacy.ai.core.IGraphBuilder;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class SaaGraphAutoConfigurationTest {


    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SaaGraphComposerAutoConfiguration.class));

    @Test
    @DisplayName("Auto Configuration should work with default settings")
    void testDefaultConfiguration() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(IGraphBuilder.class);
        });
    }

    @Test
    @DisplayName("Bean should not be created when disabled via properties")
    void testDisabledConfiguration() {
        runner.withPropertyValues("spring.ai.graph-composer.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(IGraphBuilder.class);
                });
    }

    @Test
    @DisplayName("Bean should be overridden by user-defined Bean")
    void testUserOverride() {
        runner.withUserConfiguration(UserConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(IGraphBuilder.class);
                    assertThat(context.getBean(IGraphBuilder.class))
                            .isInstanceOf(CustomGraphBuilder.class);
                });
    }

    @Configuration
    static class UserConfig {
        @Bean
        public IGraphBuilder graphBuilder() {
            return new CustomGraphBuilder();
        }
    }

    static class CustomGraphBuilder implements IGraphBuilder {
        @Override
        public CompiledGraph build(Object graphComposer) {
            return null;
        }
    }

}
