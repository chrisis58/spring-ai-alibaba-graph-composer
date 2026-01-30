package cn.teacy.ai.example;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Collections;
import java.util.List;

import static cn.teacy.ai.example.agent.graph.ExtendExampleGraphComposer.KEY_LOGS;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
public class ExtendCompilerTest {

    @Autowired
    @Qualifier("extendExampleGraph")
    private CompiledGraph extendExampleGraph;

    @Test
    void testExtendGraph() {
        // #region snippet
        OverAllState state = extendExampleGraph.invoke(Collections.emptyMap()).orElseThrow(RuntimeException::new);
        Object object = state.value(KEY_LOGS).orElseThrow(RuntimeException::new);

        assertThat(object).isInstanceOf(List.class)
                .asInstanceOf(InstanceOfAssertFactories.list(String.class))
                .containsExactly(
                        "Executed node_a",
                        "Executed node_b",
                        "Executed node_c"
                );
        // #endregion snippet
    }

}
