package cn.teacy.ai.core;

import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A GraphCompiler that integrates with Spring's ApplicationContext to resolve dependencies.
 *
 * @since 0.2.2
 */
public class SpringReflectiveGraphCompiler extends ReflectiveGraphCompiler {

    private static final Logger log = LoggerFactory.getLogger(SpringReflectiveGraphCompiler.class);

    private final ConfigurableListableBeanFactory beanFactory;

    public SpringReflectiveGraphCompiler(@Nonnull ConfigurableListableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    /**
     * Resolve missing field by looking up Spring ApplicationContext.
     *
     * @param field the field that is null
     * @param candidateName the candidate bean name
     * @return the resolved bean or null if not found
     */
    @Nullable
    @Override
    protected Object resolveMissingField(Field field, String candidateName) {
        Class<?> type = field.getType();
        String fieldName = field.getName();

        if (StringUtils.hasText(candidateName)
                && beanFactory.containsBean(candidateName) && beanFactory.isTypeMatch(candidateName, type)) {
            return beanFactory.getBean(candidateName, type);
        }

        if (beanFactory.containsBean(fieldName) && beanFactory.isTypeMatch(fieldName, type)) {
            return beanFactory.getBean(fieldName, type);
        }

        try {
            DependencyDescriptor descriptor = new DependencyDescriptor(field, false);
            Set<String> autowiredBeanNames = new LinkedHashSet<>(1);

            Object result = beanFactory.resolveDependency(descriptor, candidateName, autowiredBeanNames, null);

            if (result == null) {
                log.warn("No bean found for field {} of type {}", field.getName(), field.getType().getName());
            }

            return result;

        } catch (BeansException e) {
            log.warn("Failed to resolve dependency for field {}", field.getName(), e);
            return null;
        }
    }

}
