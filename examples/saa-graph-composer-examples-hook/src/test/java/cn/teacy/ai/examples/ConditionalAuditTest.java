package cn.teacy.ai.examples;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static cn.teacy.ai.examples.agent.graph.ConditionalAuditGraphComposer.KEY_VALUE;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
public class ConditionalAuditTest {

    // #region enableAudit
    @Nested
    @TestPropertySource(properties = "app.audit.enabled=true")
    class EnabledConfig {

        @Autowired
        @Qualifier("conditionalAuditGraph")
        CompiledGraph conditionalAuditGraph;

        @Test
        void testAuditIsOn() {
            OverAllState state = conditionalAuditGraph.invoke(Map.of()).orElseThrow(AssertionError::new);
            Object value = state.value(KEY_VALUE).orElseThrow(AssertionError::new);
            assertThat(value).isEqualTo("{(__VALUE__)}");
        }
    }
    // #endregion enableAudit

    // #region disableAudit
    @Nested
    @TestPropertySource(properties = "app.audit.enabled=false")
    class DisabledConfig {

        @Autowired
        @Qualifier("conditionalAuditGraph")
        CompiledGraph conditionalAuditGraph;

        @Test
        void testAuditIsOff() {
            OverAllState state = conditionalAuditGraph.invoke(Map.of()).orElseThrow(AssertionError::new);
            Object value = state.value(KEY_VALUE).orElseThrow(AssertionError::new);
            assertThat(value).isEqualTo("(__VALUE__)");
        }
    }
    // #endregion disableAudit

}
