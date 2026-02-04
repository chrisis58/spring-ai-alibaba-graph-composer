package cn.teacy.ai;

import cn.teacy.ai.autoconfigure.SaaGraphComposerProperties;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class PropertiesTest {

    @Test
    @DisplayName("SaaGraphComposerProperties should be able to initialize with no-args constructor")
    void testInitWithNoArgsConstructor() {
        SaaGraphComposerProperties properties = new SaaGraphComposerProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.isAutoCompiler()).isTrue();
        assertThat(properties.getBasePackages().isEmpty()).isTrue();

        properties.setEnabled(false);
        assertThat(properties.isEnabled()).isFalse();

        properties.setAutoCompiler(false);
        assertThat(properties.isAutoCompiler()).isFalse();

        properties.setBasePackages(List.of("cn.teacy.ai.tests"));
        assertThat(properties.getBasePackages())
                .asInstanceOf(InstanceOfAssertFactories.list(String.class))
                .containsExactly("cn.teacy.ai.tests");
    }

}
