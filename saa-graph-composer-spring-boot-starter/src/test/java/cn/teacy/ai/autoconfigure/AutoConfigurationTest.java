package cn.teacy.ai.autoconfigure;

import cn.teacy.ai.core.GraphCompiler;
import cn.teacy.ai.core.ReflectiveGraphCompiler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class AutoConfigurationTest {
    @Test
    @DisplayName("Direct import of nested configuration should work")
    void testDirectImportOfNestedConfiguration() {
        new SaaGraphComposerAutoConfiguration.AutoRegistrarConfiguration();

        new ApplicationContextRunner()
                .withUserConfiguration(SaaGraphComposerAutoConfiguration.AutoRegistrarConfiguration.class)
                .withUserConfiguration(UserConfig.class)
                .withInitializer(context -> {
                    AutoConfigurationPackages.register((BeanDefinitionRegistry) context, "cn.teacy.ai.tests.other");
                })
                .run(context -> {
                    assertThat(context).hasSingleBean(GraphCompiler.class);
                    assertThat(context).hasBean("otherWorkflowCompiled");
                });
    }

    @Configuration
    static class UserConfig {

        @Bean
        public GraphCompiler graphCompiler() {
            return new ReflectiveGraphCompiler();
        }

    }
}
