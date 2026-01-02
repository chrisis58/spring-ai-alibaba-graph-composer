package cn.teacy.ai.core;

import cn.teacy.ai.annotation.GraphComposer;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

@Component
public class GraphAutoRegistrar implements SmartInitializingSingleton, ApplicationContextAware {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(GraphAutoRegistrar.class);

    private ApplicationContext applicationContext;
    private final IGraphBuilder graphBuilder;

    public GraphAutoRegistrar(IGraphBuilder graphBuilder) {
        this.graphBuilder = graphBuilder;
    }

    @Override
    public void setApplicationContext(@Nonnull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (!(applicationContext instanceof ConfigurableListableBeanFactory beanFactory)) {
            log.warn("ApplicationContext is not configurable. Graph auto-registration skipped.");
            return;
        }

        Map<String, Object> composers = applicationContext.getBeansWithAnnotation(GraphComposer.class);

        for (Map.Entry<String, Object> entry : composers.entrySet()) {
            String composerBeanName = entry.getKey();
            Object composerBean = entry.getValue();

            registerSingleGraph(beanFactory, composerBean, composerBeanName);
        }
    }

    private void registerSingleGraph(ConfigurableListableBeanFactory beanFactory, Object composerBean, String composerBeanName) {
        Class<?> clazz = composerBean.getClass();
        GraphComposer anno = clazz.getAnnotation(GraphComposer.class);

        if (!anno.autoRegister()) {
            log.debug("Skipping auto-registration of graph composer {}", composerBeanName);
            return;
        }

        String targetBeanName = anno.targetBeanName();
        if (!StringUtils.hasText(targetBeanName)) {
            targetBeanName = composerBeanName + "Compiled";
        }

        if (beanFactory.containsSingleton(targetBeanName)) {
            log.warn("Skipping Graph registration: Bean '{}' already exists. Check for naming collisions.", targetBeanName);
            return;
        }

        try {
            log.info("Building CompiledGraph for composer: {}", composerBeanName);

            CompiledGraph graph = graphBuilder.build(composerBean);

            beanFactory.registerSingleton(targetBeanName, graph);

            log.info("Registered CompiledGraph Bean: '{}'", targetBeanName);

        } catch (Exception e) {
            log.error("Failed to build/register graph for '{}'", composerBeanName, e);
            throw new RuntimeException("Graph registration failed", e);
        }
    }
}