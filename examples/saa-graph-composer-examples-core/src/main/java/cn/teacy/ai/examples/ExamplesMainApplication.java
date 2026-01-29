package cn.teacy.ai.examples;

import cn.teacy.ai.examples.config.ExampleConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Map;

import static cn.teacy.ai.examples.agent.GreetingGraphComposer.KEY_INPUT;
import static cn.teacy.ai.examples.agent.GreetingGraphComposer.KEY_OUTPUT;

public class ExamplesMainApplication {

    public static void main(String[] args) {

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ExampleConfig.class)) {

            // #region snippet
            CompiledGraph graph = context.getBean(CompiledGraph.class);
            graph.invoke(Map.of(KEY_INPUT, "alice")).ifPresent(it ->
                    System.out.println(it.value(KEY_OUTPUT, "world"))
            );
            // #endregion snippet

            context.registerShutdownHook();
        } catch (Exception e) {
            System.err.println("Examples Main Application Stopped");
            e.printStackTrace();
            System.exit(1);
        }

    }
}
