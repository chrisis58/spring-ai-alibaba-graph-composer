package cn.teacy.ai;

import cn.teacy.ai.annotation.EnableGraphComposer;
import cn.teacy.ai.autoconfigure.BootGraphAutoRegistrar;
import cn.teacy.ai.autoconfigure.SaaGraphComposerAutoConfiguration;
import cn.teacy.ai.constants.ComposerConfigConstants;
import cn.teacy.ai.core.GraphCompiler;
import cn.teacy.ai.core.ReflectiveGraphCompiler;
import cn.teacy.ai.tests.another.AnotherTestGraphConfig;
import cn.teacy.ai.tests.scoped.TestGraphConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Collection;
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
    @DisplayName("Should detect and scan packages from AutoConfigurationPackages")
    void testPackageScanning() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(SaaGraphComposerAutoConfiguration.class))
                .withInitializer(context -> {
                    AutoConfigurationPackages.register((BeanDefinitionRegistry) context, "cn.teacy.ai.tests.other");
                })
                .run(context -> {
                    assertThat(context).hasSingleBean(GraphCompiler.class);
                    assertThat(context).hasBean("otherWorkflowCompiled");
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
    @DisplayName("Auto Registrar should not register CompiledGraph beans when auto-compile is disabled")
    void shouldNotRegisterGraphsWhenAutoCompileIsDisabled() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(SaaGraphComposerAutoConfiguration.class))
                .withUserConfiguration(UserConfig.class)
                .withPropertyValues("spring.ai.graph-composer.auto-compile=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(GraphCompiler.class);
                    assertThat(context).doesNotHaveBean(CompiledGraph.class);
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

    @Test
    @DisplayName("Should handle null beanFactory gracefully")
    void testNullBeanFactory() {
        class SpyRegistrar extends BootGraphAutoRegistrar {
            @Override
            public Collection<String> resolveEmptyBasePackage(AnnotationMetadata metadata) {
                return super.resolveEmptyBasePackage(metadata);
            }
        }

        SpyRegistrar spy = new SpyRegistrar();

        AnnotationMetadata metadata = AnnotationMetadata.introspect(SomeConfiguration.class);
        Collection<String> result = spy.resolveEmptyBasePackage(metadata);

        assertThat(result).containsExactly(SomeConfiguration.class.getPackageName());
    }

    @Configuration
    @EnableGraphComposer
    static class SomeConfiguration {}

}
