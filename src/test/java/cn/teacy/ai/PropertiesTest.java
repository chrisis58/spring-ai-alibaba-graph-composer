package cn.teacy.ai;

import cn.teacy.ai.properties.SaaGraphComposerProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class PropertiesTest {

    @Test
    @DisplayName("SaaGraphComposerProperties should be able to initialize with no-args constructor")
    void testInitWithNoArgsConstructor() {
        SaaGraphComposerProperties properties = new SaaGraphComposerProperties();

        assertThat(properties.isEnabled()).isTrue();

        properties.setEnabled(false);
        assertThat(properties.isEnabled()).isFalse();
    }

}
