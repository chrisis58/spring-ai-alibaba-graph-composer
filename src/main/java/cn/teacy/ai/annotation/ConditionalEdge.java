package cn.teacy.ai.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Depends on the method to determine the next edge in the graph.
 * Support method type:
 * <ul>
 * <li>(OverallState) -> String</li>
 * <li>(OverallState) -> Command</li>
 * <li>(OverallState, RunnableConfig) -> String</li>
 * <li>(OverallState, RunnableConfig) -> Command</li>
 * </ul>
 * @see cn.teacy.ai.core.ReflectiveGraphBuilder#scanConditionalEdges
 * @see com.alibaba.cloud.ai.graph.action.AsyncCommandAction
 * @see com.alibaba.cloud.ai.graph.action.AsyncEdgeAction
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConditionalEdge {

    /**
     * The source node id.
     */
    String source();

    /**
     * The mappings of the conditions to the next node ids.
     */
    String[] mappings();

}
