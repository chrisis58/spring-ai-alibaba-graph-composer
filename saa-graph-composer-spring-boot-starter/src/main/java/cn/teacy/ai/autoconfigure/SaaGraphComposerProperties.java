package cn.teacy.ai.autoconfigure;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.ai.graph-composer")
public class SaaGraphComposerProperties {

    private boolean enabled = true;
    private boolean autoCompiler = true;

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isAutoCompiler() {
        return autoCompiler;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setAutoCompiler(boolean autoCompiler) {
        this.autoCompiler = autoCompiler;
    }

}
