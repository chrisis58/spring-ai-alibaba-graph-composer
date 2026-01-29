package cn.teacy.ai.examples.config;


import cn.teacy.ai.annotation.EnableGraphComposer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

// #region snippet
@Configuration
@ComponentScan("cn.teacy.ai.examples")
@EnableGraphComposer(basePackages = "cn.teacy.ai.examples")
public class ExampleConfig {

}
// #endregion snippet
