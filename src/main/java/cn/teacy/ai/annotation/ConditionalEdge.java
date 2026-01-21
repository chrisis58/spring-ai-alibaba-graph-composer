package cn.teacy.ai.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Mark a field as a conditional edge in the graph.
 * Support field types are:
 * <ul>
 *     <li>{@link com.alibaba.cloud.ai.graph.action.EdgeAction}</li>
 *     <li>{@link com.alibaba.cloud.ai.graph.action.AsyncEdgeAction}</li>
 *     <li>{@link com.alibaba.cloud.ai.graph.action.CommandAction}</li>
 *     <li>{@link com.alibaba.cloud.ai.graph.action.AsyncCommandAction}</li>
 * </ul>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConditionalEdge {

    /**
     * The source node id.
     */
    String source();

    /**
     * The full mappings of conditions (keys) to next node ids (values).
     * <p>
     * The array length must be even. Format: {@code { "key1", "node1", "key2", "node2" }}.
     * Use this when the edgeAction's return value differs from the target node ID.
     * </p>
     */
    String[] mappings() default {};

    /**
     * A simplified list of target node IDs for direct routing.
     * <p>
     * Use this property when the routing key returned by the edgeAction is identical
     * to the target node ID. This reduces boilerplate by avoiding redundant mapping pairs.
     * </p>
     * <p>
     * <b>Behavior:</b><br>
     * Each element {@code "NODE_ID"} in this array is treated equivalently to a
     * mapping pair {@code "NODE_ID", "NODE_ID"} in {@link #mappings()}.
     * </p>
     * <p>
     * <b>Example:</b>
     * <pre>{@code
     * // Instead of verbose mappings:
     * mappings = { "NEXT_STEP", "NEXT_STEP", "END", "END" }
     *
     * // You can use simplified routes:
     * routes = { "NEXT_STEP", "END" }
     * }</pre>
     * </p>
     *
     * @see #mappings()
     * @since 0.2.2
     */
    String[] routes() default {};

    /**
     * Description of what this edge represents.
     *
     * @since 0.2.2
     */
    String description() default "";

}
