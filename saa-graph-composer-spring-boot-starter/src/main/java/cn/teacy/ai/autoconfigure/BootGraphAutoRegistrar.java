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
