package cn.teacy.ai.examples.agent.service;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class GreetingGraphService {

    private final CompiledGraph adaptorGraph;

    public GreetingGraphService(
            @Qualifier("greetingGraphWithAdaptorNode") CompiledGraph adaptorGraph
    ) {
        this.adaptorGraph = adaptorGraph;
    }

    public String greet(String name) {
        return adaptorGraph.invoke(Map.of(
                "name", name
        )).flatMap(state -> state.value("result", String.class))
                .orElse("Greeting failed.");
    }

}
