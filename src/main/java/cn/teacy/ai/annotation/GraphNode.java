package cn.teacy.ai.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Component
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface GraphNode {

    /**
     * The node id, defaults to the field name if not specified.
     */
    String id() default "";

    /**
     * The next node class in the graph.
     */
    String[] next() default {};

    /**
     * Whether this node is the start node
     */
    boolean isStart() default false;

}
