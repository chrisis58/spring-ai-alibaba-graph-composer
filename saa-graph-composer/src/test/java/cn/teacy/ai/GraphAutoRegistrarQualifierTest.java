package cn.teacy.ai;

import cn.teacy.ai.annotation.CompiledFrom;
import cn.teacy.ai.core.GraphAutoRegistrar;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class GraphAutoRegistrarQualifierTest {

    static class HackedGraphAutoRegistrar extends GraphAutoRegistrar {
        @Override
        public void addCompiledFromQualifier(AbstractBeanDefinition graphBeanDefinition, String composerClassName) {
            super.addCompiledFromQualifier(graphBeanDefinition, composerClassName);
        }
    }

    @Test
    void testAddQualifierSuccess() {
        HackedGraphAutoRegistrar registrar = new HackedGraphAutoRegistrar();
        AbstractBeanDefinition bd = BeanDefinitionBuilder.rootBeanDefinition("java.lang.Object").getBeanDefinition();
        String validClassName = "java.lang.String";

        registrar.addCompiledFromQualifier(bd, validClassName);

        assertThat(bd.getQualifiers())
                .hasSize(1)
                .first()
                .satisfies(q -> {
                    assertThat(q.getTypeName()).isEqualTo(CompiledFrom.class.getName());
                    assertThat(q.getAttribute("value")).isEqualTo(String.class);
                });
    }

    @Test
    void testAddQualifierClassNotFound() {
        HackedGraphAutoRegistrar registrar = new HackedGraphAutoRegistrar();
        AbstractBeanDefinition bd = BeanDefinitionBuilder.rootBeanDefinition("java.lang.Object").getBeanDefinition();

        registrar.addCompiledFromQualifier(bd, "com.non.existent.ClassXYZ");

        assertThat(bd.getQualifiers()).isEmpty();
    }

    @Test
    void testAddQualifierNullClassName() {
        HackedGraphAutoRegistrar registrar = new HackedGraphAutoRegistrar();
        AbstractBeanDefinition bd = BeanDefinitionBuilder.rootBeanDefinition("java.lang.Object").getBeanDefinition();

        registrar.addCompiledFromQualifier(bd, null);

        assertThat(bd.getQualifiers()).isEmpty();
    }

}
