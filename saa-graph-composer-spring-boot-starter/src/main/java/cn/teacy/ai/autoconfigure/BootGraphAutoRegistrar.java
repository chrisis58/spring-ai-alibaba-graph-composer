package cn.teacy.ai.autoconfigure;

import cn.teacy.ai.core.GraphAutoRegistrar;
import jakarta.annotation.Nonnull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Collection;

/**
 * Extension of {@link GraphAutoRegistrar} specifically designed for Spring Boot applications.
 * <p>This registrar enhances the base scanning logic by integrating with
 * {@link AutoConfigurationPackages}. When no specific base packages are defined, it
 * automatically falls back to the packages used by the {@code @SpringBootApplication}
 * or {@code @EnableAutoConfiguration} annotations.
 * <p>Additionally, this implementation suppresses the default compiler registration
 * ({@link #registerDefaultCompilerIfNecessary}) to allow the Starter's
 * Auto-Configuration class to manage the {@code GraphCompiler} Bean with full
 * conditional support (e.g., {@code @ConditionalOnMissingBean}).
 *
 * @since 0.3.0
 * @see AutoConfigurationPackages
 * @see GraphAutoRegistrar
 */
public class BootGraphAutoRegistrar extends GraphAutoRegistrar implements BeanFactoryAware {

    private BeanFactory beanFactory;

    @Override
    public void setBeanFactory(@Nonnull BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    protected void registerDefaultCompilerIfNecessary(BeanDefinitionRegistry registry) {
        // pass here to disable default compiler registration
    }

    @Override
    protected Collection<String> resolveEmptyBasePackage(AnnotationMetadata importingClassMetadata) {
        if (this.beanFactory != null && AutoConfigurationPackages.has(this.beanFactory)) {
            return AutoConfigurationPackages.get(this.beanFactory);
        }

        return super.resolveEmptyBasePackage(importingClassMetadata);
    }
}
