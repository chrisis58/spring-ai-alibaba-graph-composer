package cn.teacy.ai.annotation;

import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Mark a filed as a graph key.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GraphKey {

    /**
     * The identifier for the graph key. It will determine in this order:
     * <ul>
     *     <li>value of the annotation</li>
     *     <li>static value</li>
     *     <li>field name</li>
     * </ul>
     */
    String value() default "";

    /**
     * The strategy to handle the key when there is a conflict.
     */
    Class<? extends KeyStrategy> strategy() default ReplaceStrategy.class;

    /**
     * Whether this key is for internal use only. (Mark Only)
     */
    boolean internal() default false;

}
