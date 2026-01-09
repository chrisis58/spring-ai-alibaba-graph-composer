package cn.teacy.ai;

import cn.teacy.ai.annotation.*;
import cn.teacy.ai.core.ReflectiveGraphCompiler;
import cn.teacy.ai.exception.GraphDefinitionException;
import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class ReflectiveGraphCompilerErrorTest {

    private ReflectiveGraphCompiler builder;

    @BeforeEach
    void setUp() {
        builder = new ReflectiveGraphCompiler();
    }

    @Test
    @DisplayName("Should throw exception when @GraphComposer is missing")
    void throwExceptionMissingAnnotation() {
        assertThatThrownBy(() -> builder.compile(new Object()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("@GraphComposer");
    }

    @Test
    @DisplayName("Should throw exception for invalid Node Action type")
    void throwExceptionInvalidNodeType() {
        InvalidNodeTypeComposer composer = new InvalidNodeTypeComposer();
        assertThatThrownBy(() -> builder.compile(composer))
                .isInstanceOf(GraphDefinitionException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Must be one of");
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
        assertThatThrownBy(() -> builder.compile(composer))
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

    @Test
    @DisplayName("Should throw exception for null Node Action")
    void throwExceptionNullNodeAction() {
        NullNodeComposer composer = new NullNodeComposer();
        assertThatThrownBy(() -> builder.compile(composer))
                .isInstanceOf(GraphDefinitionException.class)
                .hasCauseInstanceOf(IllegalStateException.class)
                .satisfies(e -> {
                    assertThat(e.getCause()).hasMessageContaining("is null");
                });
    }

    @GraphComposer
    static class NullNodeComposer {
        @GraphNode(id = "node1")
        final NodeAction nodeA = null;
    }

    @Test
    @DisplayName("Should throw exception for non-String Graph Key")
    void throwExceptionNonStringGraphKey() {
        NonStringKeyComposer composer = new NonStringKeyComposer();
        assertThatThrownBy(() -> builder.compile(composer))
                .isInstanceOf(GraphDefinitionException.class)
                .hasMessageContaining("must be String");
    }

    @GraphComposer
    static class NonStringKeyComposer {
        @GraphKey
        static final Object KEY_INVALID = new Object();
    }

    @Test
    @DisplayName("Should throw exception for non-static Graph Key")
    void throwExceptionNonStaticGraphKey() {
        NonStaticKeyComposer composer = new NonStaticKeyComposer();
        assertThatThrownBy(() -> builder.compile(composer))
                .isInstanceOf(GraphDefinitionException.class)
                .hasMessageContaining("must be 'static'");
    }

    @GraphComposer
    static class NonStaticKeyComposer {
        @GraphKey
        final String KEY_INSTANCE = "instanceKey";
    }

    @Test
    @DisplayName("Should throw exception for non-final Graph Key")
    void throwExceptionNonFinalGraphKey() {
        NonFinalKeyComposer composer = new NonFinalKeyComposer();
        assertThatThrownBy(() -> builder.compile(composer))
                .isInstanceOf(GraphDefinitionException.class)
                .hasMessageContaining("must be 'final'");
    }

    @GraphComposer
    static class NonFinalKeyComposer {
        @GraphKey
        static String KEY_NON_FINAL = "nonFinalKey";
    }

    @Test
    @DisplayName("Should throw exception for null Edge Action")
    void throwExceptionNullEdgeAction() {
        NullRoutingComposer composer = new NullRoutingComposer();
        assertThatThrownBy(() -> builder.compile(composer))
                .isInstanceOf(GraphDefinitionException.class)
                .hasMessageContaining("is null");
    }

    @GraphComposer
    static class NullRoutingComposer {
        @ConditionalEdge(source = StateGraph.START, mappings = {"toEnd", StateGraph.END})
        final EdgeAction nullRouting = null;
    }

    @Test
    @DisplayName("Should throw exception for unsupported Edge Action type")
    void throwExceptionUnsupportedEdgeType() {
        UnsupportedEdgeTypeComposer composer = new UnsupportedEdgeTypeComposer();
        assertThatThrownBy(() -> builder.compile(composer))
                .isInstanceOf(GraphDefinitionException.class)
                .hasMessageContaining("is not supported. Must be one of");
    }

    @GraphComposer
    static class UnsupportedEdgeTypeComposer {
        @ConditionalEdge(source = StateGraph.START, mappings = {"toEnd", StateGraph.END})
        final String invalidRouting = "I am not an EdgeAction";
    }

    @Test
    @DisplayName("Should throw exception for null CompileConfig")
    void throwExceptionNullCompileConfig() {
        NullCompileConfigGraphComposer composer = new NullCompileConfigGraphComposer();
        assertThatThrownBy(() -> builder.compile(composer))
                .isInstanceOf(GraphDefinitionException.class)
                .hasMessageContaining("must not be null");
    }

    @GraphComposer
    static class NullCompileConfigGraphComposer {

        @GraphCompileConfig
        final CompileConfig nullConfig = null;
    }

    @Test
    @DisplayName("Should throw exception for unsupported CompileConfig type")
    void throwExceptionUnsupportedCompileConfigType() {
        NotSupportTypeCompileConfigGraphComposer composer = new NotSupportTypeCompileConfigGraphComposer();
        assertThatThrownBy(() -> builder.compile(composer))
                .isInstanceOf(GraphDefinitionException.class)
                .hasMessageContaining("must be of type");

        NotSupportSupplierTypeCompileConfigGraphComposer composerB = new NotSupportSupplierTypeCompileConfigGraphComposer();
        assertThatThrownBy(() -> builder.compile(composerB))
                .isInstanceOf(GraphDefinitionException.class)
                .hasMessageContaining("returned null or an invalid type");
    }

    @GraphComposer
    static class NotSupportTypeCompileConfigGraphComposer {

        @GraphCompileConfig
        final Object invalidConfig = new Object();
    }

    @GraphComposer
    static class NotSupportSupplierTypeCompileConfigGraphComposer {

        @GraphCompileConfig
        final Supplier<Object> invalidConfig = Object::new;
    }

    @Test
    @DisplayName("Should throw exception for invalid Edge mapping")
    void throwExceptionInvalidEdgeMapping() {
        InvalidRouteMappingGraphComposer composer = new InvalidRouteMappingGraphComposer();
        assertThatThrownBy(() -> builder.compile(composer))
                .isInstanceOf(GraphDefinitionException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class)
                .satisfies(e -> {
                    assertThat(e.getCause()).hasMessageContaining("Mappings must be pairs in field");
                });
    }

    @GraphComposer
    static class InvalidRouteMappingGraphComposer {

        @ConditionalEdge(source = StateGraph.START, mappings = {"onlyOneSide"})
        final EdgeAction edgeWithInvalidMapping = (state) -> StateGraph.END;

    }

    @Test
    @DisplayName("Should throw exception for duplicate Graph Keys")
    void throwExceptionDuplicateGraphKeys() {
        DuplicateKeyGraphComposer composer = new DuplicateKeyGraphComposer();
        assertThatThrownBy(() -> builder.compile(composer))
                .isInstanceOf(GraphDefinitionException.class)
                .hasMessageContaining("Duplicate Graph Key");
    }

    @GraphComposer
    static class DuplicateKeyGraphComposer {

        private static final String DUPLICATE_KEY = "duplicateKey";

        @GraphKey
        private static final String KEY_1 = DUPLICATE_KEY;

        @GraphKey
        private static final String KEY_2 = DUPLICATE_KEY;

    }

    @GraphComposer
    static class DuplicateNodeComposer {
        @GraphNode(id = "node1")
        final NodeAction nodeA = (state) -> Map.of();

        @GraphNode(id = "node1")
        final NodeAction nodeB = (state) -> Map.of();
    }

    @Test
    @DisplayName("Should throw exception for duplicated Conditional Edge definitions")
    void throwExceptionDuplicatedConditionalEdges() {
        DuplicatedConditionEdgeGraphComposer composer = new DuplicatedConditionEdgeGraphComposer();
        assertThatThrownBy(() -> builder.compile(composer))
                .isInstanceOf(GraphDefinitionException.class)
                .hasMessageContaining("already exist");
    }

    @GraphComposer
    static class DuplicatedConditionEdgeGraphComposer {
        @ConditionalEdge(source = StateGraph.START, mappings = {"toEnd", StateGraph.END})
        final EdgeAction edge1 = (state) -> StateGraph.END;

        @ConditionalEdge(source = StateGraph.START, mappings = {"toEnd", StateGraph.END})
        final EdgeAction edge2 = (state) -> StateGraph.END;
    }

    @Test
    @DisplayName("Should throw exception for duplicated CompileConfig definitions")
    void throwExceptionDuplicatedCompileConfig() {
        DuplicatedCompileConfigGraphComposer composer = new DuplicatedCompileConfigGraphComposer();
        assertThatThrownBy(() -> builder.compile(composer))
                .isInstanceOf(GraphDefinitionException.class)
                .hasCauseInstanceOf(IllegalStateException.class)
                .satisfies(e -> {
                    assertThat(e.getCause()).hasMessageContaining("Multiple @GraphCompileConfig fields found");
                });
    }

    @GraphComposer
    static class DuplicatedCompileConfigGraphComposer {
        @GraphCompileConfig
        final CompileConfig configA = CompileConfig.builder().build();

        @GraphCompileConfig
        final Supplier<CompileConfig> configB = () -> CompileConfig.builder().build();
    }

}