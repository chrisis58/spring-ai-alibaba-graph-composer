package cn.teacy.ai.autoconfigure;


import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "spring.ai.graph-composer")
public class SaaGraphComposerProperties {

    /**
     * Whether to enable Graph Composer Auto-Configuration.
     * <p>
     * Defaults to {@code true}.
     *
     * @see SaaGraphComposerAutoConfiguration
     */
    private boolean enabled = true;

    /**
     * Whether to enable Auto-Compilation of Graph Composers.
     * <p>
     * If enabled, the framework will scan for {@code @GraphComposer} beans and automatically
     * register corresponding {@code CompiledGraph} beans.
     * <p>
     * Defaults to {@code true}.
     */
    private boolean autoCompiler = true;

    /**
     * Base packages to scan for {@code @GraphComposer} annotated classes.
     * <p>
     * If not specified, the scanning will occur from the package of the class that declares
     * the {@code @EnableGraphComposer} annotation or the main application class.
     *
     * @since 0.3.1
     */
    private List<String> basePackages = new ArrayList<>();

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

    public List<String> getBasePackages() {
        return basePackages;
    }

    public void setBasePackages(List<String> basePackages) {
        this.basePackages = basePackages;
    }

}
