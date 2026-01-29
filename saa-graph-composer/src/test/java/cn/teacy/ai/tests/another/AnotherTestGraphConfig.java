package cn.teacy.ai.tests.another;

import cn.teacy.ai.annotation.EnableGraphComposer;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableGraphComposer(basePackages = "cn.teacy.ai.tests.other")
public class AnotherTestGraphConfig {

}
