package cn.teacy.ai.scoped;

import cn.teacy.ai.annotation.CompiledFrom;
import cn.teacy.ai.annotation.EnableGraphComposer;
import cn.teacy.ai.annotation.GraphComposer;
import cn.teacy.ai.core.GraphCompiler;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ComposerQualificationTest {

    @GraphComposer(targetBeanName = "alphaGraph")
    static class AlphaGraphComposer {}

    @GraphComposer(targetBeanName = "betaGraph")
    static class BetaGraphComposer {}

    static class TestService {
        final CompiledGraph alphaGraph;
        final CompiledGraph betaGraph;

        public TestService(
                @CompiledFrom(AlphaGraphComposer.class) CompiledGraph alphaGraph,
                @CompiledFrom(BetaGraphComposer.class) CompiledGraph betaGraph
        ) {
            this.alphaGraph = alphaGraph;
            this.betaGraph = betaGraph;
        }
    }

    @Configuration
    @EnableGraphComposer
    static class TestConfig {

        @Bean("graphCompiler")
        public GraphCompiler graphCompiler() {
            GraphCompiler mockCompiler = mock(GraphCompiler.class);
            when(mockCompiler.compile(any())).thenAnswer(invocation ->
                mock(CompiledGraph.class)
            );
            return mockCompiler;
        }

    }

    @Test
    void testQualifierInjection() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

        context.register(TestConfig.class);
        context.register(AlphaGraphComposer.class);
        context.register(BetaGraphComposer.class);
        context.register(TestService.class);

        context.refresh();

        TestService service = context.getBean(TestService.class);

        assertThat(service).isNotNull();
        assertThat(service.alphaGraph).isNotNull();
        assertThat(service.betaGraph).isNotNull();

        Object alphaBean = context.getBean("alphaGraph");
        Object betaBean = context.getBean("betaGraph");

        assertThat(service.alphaGraph).isNotNull();
        assertThat(service.betaGraph).isNotNull();

        assertThat(alphaBean).isEqualTo(service.alphaGraph);
        assertThat(betaBean).isEqualTo(service.betaGraph);

        BeanDefinition alphaDef = context.getBeanDefinition("alphaGraph");

        assertThat(alphaDef).isInstanceOf(AbstractBeanDefinition.class);

        AbstractBeanDefinition alphaAbd = (AbstractBeanDefinition) alphaDef;
        assertThat(alphaAbd.getQualifiers())
                .asInstanceOf(InstanceOfAssertFactories.set(AutowireCandidateQualifier.class))
                .anyMatch(q ->
                        CompiledFrom.class.getName().equals(q.getTypeName()) &&
                                AlphaGraphComposer.class.equals(q.getAttribute("value")));


        BeanDefinition betaDef = context.getBeanDefinition("betaGraph");
        assertThat(betaDef).isInstanceOf(AbstractBeanDefinition.class);

        AbstractBeanDefinition betaAbd = (AbstractBeanDefinition) betaDef;
        assertThat(betaAbd.getQualifiers())
                .asInstanceOf(InstanceOfAssertFactories.set(AutowireCandidateQualifier.class))
                .anyMatch(q ->
                        CompiledFrom.class.getName().equals(q.getTypeName()) &&
                                BetaGraphComposer.class.equals(q.getAttribute("value")));

    }


}
