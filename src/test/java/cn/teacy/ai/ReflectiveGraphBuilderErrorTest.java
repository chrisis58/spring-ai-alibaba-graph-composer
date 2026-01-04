package cn.teacy.ai;

import cn.teacy.ai.annotation.GraphComposer;
import cn.teacy.ai.annotation.GraphNode;
import cn.teacy.ai.core.ReflectiveGraphBuilder;
import cn.teacy.ai.exception.GraphDefinitionException;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class ReflectiveGraphBuilderErrorTest {

    private ReflectiveGraphBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new ReflectiveGraphBuilder();
    }

    @Test
    @DisplayName("Should throw exception when @GraphComposer is missing")
    void throwExceptionMissingAnnotation() {
        assertThatThrownBy(() -> builder.build(new Object()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("@GraphComposer");
    }

    @Test
    @DisplayName("Should throw exception for invalid Node Action type")
    void throwExceptionInvalidNodeType() {
        InvalidNodeTypeComposer composer = new InvalidNodeTypeComposer();
        assertThatThrownBy(() -> builder.build(composer))
                .isInstanceOf(GraphDefinitionException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class)
                .satisfies(e -> {
                    assertThat(e.getCause()).hasMessageContaining("must be instance of");
                });
    }

    @GraphComposer
    static class InvalidNodeTypeComposer {
        @GraphNode(id = "node1")
        final String invalidNode = "I am not an action";
    }

    @Test
    @DisplayName("Should throw exception for duplicate Node IDs")
    void throwExceptionDuplicateNodeIds() {
        DuplicateNodeIdComposer composer = new DuplicateNodeIdComposer();
        assertThatThrownBy(() -> builder.build(composer))
                .isInstanceOf(GraphDefinitionException.class)
                .hasMessageContaining("already exist");
    }

    @GraphComposer
    static class DuplicateNodeIdComposer {
        @GraphNode(id = "node1")
        final NodeAction nodeA = (state) -> Map.of();

        @GraphNode(id = "node1")
        final NodeAction nodeB = (state) -> Map.of();
    }
}