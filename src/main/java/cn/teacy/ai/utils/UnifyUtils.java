package cn.teacy.ai.utils;

import com.alibaba.cloud.ai.graph.action.*;
import jakarta.annotation.Nonnull;

import java.util.Objects;

public class UnifyUtils {

    private UnifyUtils() {}

    @Nonnull
    public static AsyncCommandAction getUnifiedCommandAction(@Nonnull Object val) {
        Objects.requireNonNull(val, "Action value cannot be null");

        if (val instanceof AsyncCommandAction action) {
            return action;
        } else if (val instanceof CommandAction action) {
            return AsyncCommandAction.node_async(action);
        } else if (val instanceof AsyncEdgeAction action) {
            return AsyncCommandAction.of(action);
        } else if (val instanceof EdgeAction action) {
            return AsyncCommandAction.of(AsyncEdgeAction.edge_async(action));
        }

        throw new IllegalArgumentException("Unexpected type for Action: " + val.getClass());
    }

    @Nonnull
    public static AsyncNodeActionWithConfig getUnifiedNodeAction(@Nonnull Object val) {
        Objects.requireNonNull(val, "Action value cannot be null");

        if (val instanceof AsyncNodeActionWithConfig action) {
            return action;
        } else if (val instanceof NodeActionWithConfig action) {
            return AsyncNodeActionWithConfig.node_async(action);
        } else if (val instanceof AsyncNodeAction action) {
            return AsyncNodeActionWithConfig.of(action);
        } else if (val instanceof NodeAction action) {
            return AsyncNodeActionWithConfig.of(AsyncNodeAction.node_async(action));
        }

        throw new IllegalArgumentException("Unexpected type for Action: " + val.getClass());
    }

}
