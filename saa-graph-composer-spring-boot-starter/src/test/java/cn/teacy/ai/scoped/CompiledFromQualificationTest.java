package cn.teacy.ai.scoped;

import cn.teacy.ai.annotation.CompiledFrom;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = {
                CompiledFromQualificationTest.TestConfig.class,
                GreetingGraphComposer.class,
                OtherGraphComposer.class
        },
        properties = "spring.ai.graph-composer.base-packages=cn.teacy.ai.scoped"
)
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

    @Autowired
    @CompiledFrom(OtherGraphComposer.class)
    private CompiledGraph otherCompiledGraph;

    @Test
    void testCompiledFromOtherGraph() {
        assertThat(otherCompiledGraph).isNotNull();

        OverAllState state = otherCompiledGraph.invoke(Collections.emptyMap()).orElseThrow(AssertionError::new);
        assertThat(state.value(OtherGraphComposer.KEY_OUTPUT))
                .isPresent()
                .get()
                .isInstanceOf(String.class)
                .isEqualTo("other graph output");
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestConfig {

    }

}
