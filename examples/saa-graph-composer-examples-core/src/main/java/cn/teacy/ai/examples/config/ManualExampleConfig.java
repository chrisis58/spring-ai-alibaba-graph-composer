package cn.teacy.ai.examples.config;

import cn.teacy.ai.core.GraphCompiler;
import cn.teacy.ai.core.SpringReflectiveGraphCompiler;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

// #region snippet
@Configuration
// 不再需要 @EnableGraphComposer
@ComponentScan("cn.teacy.ai.examples")
public class ManualExampleConfig {

    @Bean
    public GraphCompiler graphCompiler(ApplicationContext applicationContext) {
        return new SpringReflectiveGraphCompiler(applicationContext);
    }

}
// #endregion snippet
