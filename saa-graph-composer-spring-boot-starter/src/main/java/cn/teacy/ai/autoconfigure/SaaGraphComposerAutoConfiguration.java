package cn.teacy.ai.autoconfigure;

import cn.teacy.ai.core.GraphCompiler;
import cn.teacy.ai.core.SpringReflectiveGraphCompiler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static cn.teacy.ai.constants.ComposerConfigConstants.GRAPH_COMPILER_BEAN_NAME;

@AutoConfiguration
@EnableConfigurationProperties(SaaGraphComposerProperties.class)
@ConditionalOnProperty(prefix = "spring.ai.graph-composer", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SaaGraphComposerAutoConfiguration {

    @Bean(GRAPH_COMPILER_BEAN_NAME)
    @ConditionalOnMissingBean(name = GRAPH_COMPILER_BEAN_NAME)
    public GraphCompiler graphCompiler(ApplicationContext applicationContext) {
        return new SpringReflectiveGraphCompiler(applicationContext);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "spring.ai.graph-composer", name = "auto-compile", havingValue = "true", matchIfMissing = true)
    @Import(BootGraphAutoRegistrar.class)
    static class AutoRegistrarConfiguration {}

}
