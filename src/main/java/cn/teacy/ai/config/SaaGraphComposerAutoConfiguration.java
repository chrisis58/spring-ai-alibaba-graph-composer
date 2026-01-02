package cn.teacy.ai.config;

import cn.teacy.ai.core.GraphAutoRegistrar;
import cn.teacy.ai.core.IGraphBuilder;
import cn.teacy.ai.core.ReflectiveGraphBuilder;
import cn.teacy.ai.properties.SaaGraphComposerProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SaaGraphComposerProperties.class)
@ConditionalOnProperty(prefix = "spring.ai.graph-composer", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SaaGraphComposerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public IGraphBuilder graphBuilder() {
        return new ReflectiveGraphBuilder();
    }

    @Bean
    @ConditionalOnMissingBean
    public GraphAutoRegistrar graphAutoRegistrar(IGraphBuilder graphBuilder) {
        return new GraphAutoRegistrar(graphBuilder);
    }

}
