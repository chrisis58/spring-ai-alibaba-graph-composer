package cn.teacy.ai.example.agent.config;

import cn.teacy.ai.example.agent.extend.annotation.GraphEdge;
import cn.teacy.ai.example.agent.extend.interfaces.GraphModule;
import cn.teacy.ai.annotation.GraphNode;
import cn.teacy.ai.core.GraphCompiler;
import cn.teacy.ai.core.SpringReflectiveGraphCompiler;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.Map;

import static cn.teacy.ai.constants.ComposerConfigConstants.GRAPH_COMPILER_BEAN_NAME;

// #region overrideCompiler
@Configuration
public class GraphComposerConfig {

    @Bean(GRAPH_COMPILER_BEAN_NAME)
    public GraphCompiler graphCompiler(ApplicationContext applicationContext) {
        // #endregion overrideCompiler
        return new SpringReflectiveGraphCompiler(applicationContext) {

            // #region extendGraphNode
            @Override
            protected void handleGraphNode(CompileContext context, Field field, GraphNode anno) {
                // 1. 获取字段值
                String nodeId = StringUtils.hasText(anno.id()) ? anno.id() : field.getName();

                ReflectionUtils.makeAccessible(field);
                Object instance = ReflectionUtils.getField(field, context.composerInstance());

                if (instance == null) {
                    instance = resolveMissingField(field, nodeId);
                }

                if (instance == null) {
                    throw new IllegalStateException("GraphNode field '" + field.getName() + "' is null. Please initialize it.");
                }

                // 2. 识别 GraphModule 接口
                if (instance instanceof GraphModule module) {
                    context.registerOperation(builder -> {
                        Map<String, NodeAction> nodes = module.namedNodes();
                        if (nodes == null || nodes.isEmpty()) {
                            return;
                        }

                        // 3.1 批量注册模块内的所有节点
                        for (Map.Entry<String, NodeAction> entry : nodes.entrySet()) {
                            // 如果 action 类型复杂，可以使用 UnifyUtils 进行转换
                            builder.addNode(entry.getKey(), AsyncNodeAction.node_async(entry.getValue()));
                        }

                        // 3.2 处理模块入口连线
                        if (anno.isStart() && StringUtils.hasText(anno.id())) {
                            builder.addEdge(StateGraph.START, anno.id());
                        }

                    }, "register GraphModule nodes from field '%s'", field.getName());

                    // 4. 处理完毕，阻断父类逻辑
                    return;
                }

                // 5. 非模块类型，务必回退给父类处理标准类型
                super.handleGraphNode(context, field, anno);
            }
            // #endregion extendGraphNode

            // #region extendOtherField
            @Override
            protected void handleOtherField(CompileContext context, Field field) {
                // 1. 检查字段是否包含自定义注解
                if (field.isAnnotationPresent(GraphEdge.class)) {
                    handleCustomGraphEdge(context, field, field.getAnnotation(GraphEdge.class));
                    return; // 处理完毕，阻断父类逻辑
                }

                // 2. 对于不认识的字段，必须调用父类（父类会记录 Debug 日志）
                super.handleOtherField(context, field);
            }

            private void handleCustomGraphEdge(CompileContext context, Field field, GraphEdge anno) {
                // 1. 校验字段类型 (约定字段值必须是目标节点ID)
                if (field.getType() != String.class) {
                    throw new IllegalArgumentException(
                            String.format("Field '%s' annotated with @GraphEdge must be of type String.", field.getName()));
                }

                ReflectionUtils.makeAccessible(field);

                // 2. 获取字段值（即 Target Node ID）
                Object value = ReflectionUtils.getField(field, context.composerInstance());
                String targetId = (String) value;

                if (!StringUtils.hasText(targetId)) {
                    throw new IllegalArgumentException(
                            String.format("Field '%s' must provide a valid target node ID.", field.getName()));
                }

                String sourceId = anno.source();

                // 3. 核心步骤：向 Context 添加操作指令
                // 这些指令会在 compile() 的最终阶段执行
                context.registerOperation(builder -> {
                    builder.addEdge(sourceId, targetId);
                }, "add custom edge from '%s' to '%s' (field: %s)", sourceId, targetId, field.getName());
            }
            // #endregion extendOtherField
        };
    }

}
