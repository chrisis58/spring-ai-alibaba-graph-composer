package cn.teacy.ai.core;

import jakarta.annotation.Nonnull;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;

public class SpringReflectiveGraphCompiler extends ReflectiveGraphCompiler implements ApplicationContextAware {

    private static final Log log = LogFactory.getLog(SpringReflectiveGraphCompiler.class);

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(@Nonnull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
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
        if (applicationContext == null) {
            return super.resolveMissingField(field, candidateName);
        }

        Class<?> type = field.getType();

        if (StringUtils.hasText(candidateName) &&
                applicationContext.containsBean(candidateName) &&
                applicationContext.isTypeMatch(candidateName, type)) {
            return applicationContext.getBean(candidateName, type);
        }

        String fieldName = field.getName();
        if (!fieldName.equals(candidateName) &&
                applicationContext.containsBean(fieldName) &&
                applicationContext.isTypeMatch(fieldName, type)) {
            return applicationContext.getBean(fieldName, type);
        }

        try {
            return applicationContext.getBean(type);
        } catch (NoUniqueBeanDefinitionException e) {
            log.error("Multiple beans of type " + type.getName() +
                    " found in ApplicationContext. ", e);
            return null;
        } catch (NoSuchBeanDefinitionException e) {
            log.error("No bean of type " + type.getName() +
                    " found in ApplicationContext.", e);
            return null;
        }
    }

}
