package cn.teacy.ai.examples;

import cn.teacy.ai.examples.agent.graph.GreetingGraphWithAdaptorNodeComposer;
import cn.teacy.ai.examples.agent.graph.GreetingGraphWithBeanNodeComposer;
import cn.teacy.ai.examples.service.GreetingService;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
public class GreetingGraphTests {

    @Autowired
    private GreetingService greetingService;

    @Autowired
    @Qualifier("greetingGraphWithBeanNode")
    private CompiledGraph beanGraph;

    @Test
    void testGreetingGraph() {
        String name = "Alice";

        String input = GreetingGraphWithBeanNodeComposer.KEY_INPUT;
        String output = GreetingGraphWithBeanNodeComposer.KEY_OUTPUT;

        assertThat(beanGraph).isNotNull();
        Optional<OverAllState> invoked = beanGraph.invoke(Map.of(input, name));

        assertThat(invoked).isPresent();
        assert invoked.isPresent();

        OverAllState state = invoked.get();

        Optional<String> result = state.value(output, String.class);
        assertThat(result).isPresent()
                .get()
                .satisfies(s -> assertThat(s).isEqualTo(greetingService.greet(name)));

    }

    @Autowired
    @Qualifier("greetingGraphWithAdaptorNode")
    private CompiledGraph adaptorGraph;

    @Test
    void testGreetingGraphWithAdaptorNode() {
        String name = "Bob";

        String input = GreetingGraphWithAdaptorNodeComposer.KEY_INPUT;
        String output = GreetingGraphWithAdaptorNodeComposer.KEY_OUTPUT;

        assertThat(adaptorGraph).isNotNull();
        Optional<OverAllState> invoked = adaptorGraph.invoke(Map.of(input, name));

        assertThat(invoked).isPresent();
        assert invoked.isPresent();

        OverAllState state = invoked.get();

        Optional<String> result = state.value(output, String.class);
        assertThat(result).isPresent()
                .get()
                .satisfies(s -> assertThat(s).isEqualTo(greetingService.greet(name)));
    }


}
