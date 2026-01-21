package cn.teacy.ai.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a field as the supplier of Graph Compile Config.
 * Support field types are:
 * <ul>
 *     <li>{@link com.alibaba.cloud.ai.graph.CompileConfig}</li>
 *     <li>{@link java.util.function.Supplier<com.alibaba.cloud.ai.graph.CompileConfig>}</li>
 *     <li>{@link cn.teacy.ai.interfaces.CompileConfigSupplier}</li>
 * </ul>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GraphCompileConfig {

    /**
     * Description of what this config represents.
     *
     * @since 0.2.2
     */
    String description() default "";

}
