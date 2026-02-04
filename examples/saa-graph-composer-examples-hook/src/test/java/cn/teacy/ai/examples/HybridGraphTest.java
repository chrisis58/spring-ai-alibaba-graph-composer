package cn.teacy.ai.examples;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
public class HybridGraphTest {

    @Autowired
    @Qualifier("hybridGraph")
    private CompiledGraph hybridGraph;

    @Test
    void testHybridGraph() {
        OverAllState state = hybridGraph.invoke(Map.of("input", "test")).orElseThrow(AssertionError::new);
        Object output = state.value("result").orElseThrow(AssertionError::new);
        assertThat(output).isEqualTo("test-processed");

    }

}
