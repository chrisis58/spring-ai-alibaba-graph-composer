package cn.teacy.ai.annotation;

import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Mark a field as a graph key.
 * Support field type:
 * <ul>
 *     <li>{@link java.lang.String}</li>
 * </ul>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GraphKey {

    /**
     * The strategy to handle the key when there is a conflict.
     */
    Class<? extends KeyStrategy> strategy() default ReplaceStrategy.class;

    /**
     * Description of what this key represents.
     */
    String description() default "";

    /**
     * Whether this key is for internal use only. (Mark Only)
     */
    boolean internal() default false;

}
