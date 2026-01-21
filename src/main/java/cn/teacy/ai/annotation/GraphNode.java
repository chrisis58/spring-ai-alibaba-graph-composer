package cn.teacy.ai.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a field as a node in the graph.
 * Support field types are:
 * <ul>
 *     <li>{@link com.alibaba.cloud.ai.graph.action.NodeAction}</li>
 *     <li>{@link com.alibaba.cloud.ai.graph.action.NodeActionWithConfig}</li>
 *     <li>{@link com.alibaba.cloud.ai.graph.action.AsyncNodeAction}</li>
 *     <li>{@link com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig}</li>
 *     <li>{@link com.alibaba.cloud.ai.graph.CompiledGraph}</li>
 * </ul>
 */
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

    /**
     * Description of what this node represents.
     */
    String description() default "";

}
