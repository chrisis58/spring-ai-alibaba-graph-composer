package cn.teacy.ai.examples;

import cn.teacy.ai.core.GraphCompiler;
import cn.teacy.ai.examples.agent.GreetingGraphComposer;
import cn.teacy.ai.examples.config.ManualExampleConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Map;

import static cn.teacy.ai.examples.agent.GreetingGraphComposer.KEY_INPUT;
import static cn.teacy.ai.examples.agent.GreetingGraphComposer.KEY_OUTPUT;

public class ManualExampleMainApplication {

    public static void main(String[] args) {


        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ManualExampleConfig.class)) {

            // #region snippet
            GraphCompiler compiler = context.getBean(GraphCompiler.class);
            GreetingGraphComposer composer = context.getBean(GreetingGraphComposer.class);

            CompiledGraph graph = compiler.compile(composer);
            graph.invoke(Map.of(KEY_INPUT, "bob")).ifPresent(it ->
                    System.out.println(it.value(KEY_OUTPUT, "world"))
            );
            // #endregion snippet

            context.registerShutdownHook();
        } catch (Exception e) {
            System.err.println("Manual Examples Main Application Stopped");
            e.printStackTrace();
            System.exit(1);
        }

    }

}
