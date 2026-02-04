package cn.teacy.ai.scoped;

import cn.teacy.ai.annotation.CompiledFrom;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(properties = "spring.ai.graph-composer.base-packages=cn.teacy.ai.scoped")
public class CompiledFromQualificationTest {

    @Autowired
    @CompiledFrom(GreetingGraphComposer.class)
    private CompiledGraph compiledGraph;

    @Test
    void testCompiledFromInjection() {
        assertThat(compiledGraph).isNotNull();

        OverAllState state = compiledGraph.invoke(Map.of(GreetingGraphComposer.KEY_INPUT, "Shi Jie")).orElseThrow(AssertionError::new);
        assertThat(state.value(GreetingGraphComposer.KEY_OUTPUT))
                .isPresent()
                .get()
                .isInstanceOf(String.class)
                .isEqualTo("Hello, Shi Jie!");

    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestConfig {

    }

}
