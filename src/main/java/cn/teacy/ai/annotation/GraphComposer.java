package cn.teacy.ai.annotation;

import org.springframework.stereotype.Component;
import java.lang.annotation.*;

/**
 * Mark class as a Graph Composer
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface GraphComposer {

    /**
     * Identifier for the composer
     */
    String id() default "";

    String description() default "";

    /**
     * Whether to auto register the generated {@link com.alibaba.cloud.ai.graph.CompiledGraph} Bean
     */
    boolean autoRegister() default true;

    /**
     * The target bean name for the generated {@link com.alibaba.cloud.ai.graph.CompiledGraph} Bean
     */
    String targetBeanName() default "";
}