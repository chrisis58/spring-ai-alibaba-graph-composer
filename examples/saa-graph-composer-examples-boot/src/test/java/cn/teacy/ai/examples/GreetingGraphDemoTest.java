package cn.teacy.ai.examples;

import cn.teacy.ai.examples.agent.graph.GreetingGraphWithAdaptorNodeComposer;
import cn.teacy.ai.examples.agent.graph.GreetingGraphWithBeanNodeComposer;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

@SpringBootTest
public class GreetingGraphDemoTest {

    // region beanRef
    @Autowired
    @Qualifier("greetingGraphWithBeanNode")
    // 0.3.1+
    // @CompiledFrom(GreetingGraphWithBeanNodeComposer.class)
    private CompiledGraph greetingGraphWithBeanNode;

    @Test
    void greetingAlice() {
        String name = "Alice";

        String input = GreetingGraphWithBeanNodeComposer.KEY_INPUT;
        String output = GreetingGraphWithBeanNodeComposer.KEY_OUTPUT;

        OverAllState state = greetingGraphWithBeanNode.invoke(Map.of(
                input, name
        )).orElseThrow();

        System.out.println(state.value(output).orElse("无结果"));
        // Hello, Alice! Welcome to SAA Graph Composer.
    }
    // endregion beanRef

    // region adaptor
    @Autowired
    @Qualifier("greetingGraphWithAdaptorNode")
    // 0.3.1+
    // @CompiledFrom(GreetingGraphWithAdaptorNodeComposer.class)
    private CompiledGraph greetingGraphWithAdaptorNode;

    @Test
    void greetingBob() {
        String name = "Bob";

        String input = GreetingGraphWithAdaptorNodeComposer.KEY_INPUT;
        String output = GreetingGraphWithAdaptorNodeComposer.KEY_OUTPUT;

        OverAllState state = greetingGraphWithAdaptorNode.invoke(Map.of(
                input, name
        )).orElseThrow();

        System.out.println(state.value(output).orElse("无结果"));
        // Hello, Bob! Welcome to SAA Graph Composer.
    }
    // endregion adaptor

}
