package cn.teacy.ai.core;

import cn.teacy.ai.annotation.EnableGraphComposer;
import cn.teacy.ai.annotation.GraphComposer;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static cn.teacy.ai.constants.ComposerConfigConstants.GRAPH_BUILDER_BEAN_NAME;

public class GraphAutoRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware {

    private final BeanNameGenerator beanNameGenerator = AnnotationBeanNameGenerator.INSTANCE;

    private ResourceLoader resourceLoader;

    @Override
    public void setResourceLoader(@Nonnull ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void registerBeanDefinitions(@Nonnull AnnotationMetadata importingClassMetadata, @Nonnull BeanDefinitionRegistry registry) {
        Set<String> basePackages = getBasePackages(importingClassMetadata);

        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.setResourceLoader(resourceLoader);
        scanner.addIncludeFilter(new AnnotationTypeFilter(GraphComposer.class));

        for (String basePackage : basePackages) {
            Set<BeanDefinition> candidateComponents = scanner.findCandidateComponents(basePackage);

            for (BeanDefinition candidate : candidateComponents) {
                registerCompiledGraphBean(registry, candidate);
            }
        }
    }

    private void registerCompiledGraphBean(BeanDefinitionRegistry registry, BeanDefinition composerBeanDefinition) {
        String composerBeanName = beanNameGenerator.generateBeanName(composerBeanDefinition, registry);

        if (!registry.containsBeanDefinition(composerBeanName)) {
            registry.registerBeanDefinition(composerBeanName, composerBeanDefinition);
        }

        String targetBeanName = null;
        if (composerBeanDefinition instanceof AnnotatedBeanDefinition annotatedDef) {
            Map<String, Object> attributes = annotatedDef.getMetadata()
                    .getAnnotationAttributes(GraphComposer.class.getName());

            if (attributes != null) {
                targetBeanName = (String) attributes.get("targetBeanName");
            }
        }

        if (!StringUtils.hasText(targetBeanName)) {
            if (composerBeanName.endsWith("Composer")) {
                // "logAnalyseGraphComposer" -> "logAnalyseGraph"
                targetBeanName = composerBeanName.substring(0, composerBeanName.length() - "Composer".length());
            } else {
                // "someOtherName" -> "someOtherNameCompiled"
                targetBeanName = composerBeanName + "Compiled";
            }
        }

        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(CompiledGraph.class);
        builder.setFactoryMethodOnBean("compile", GRAPH_BUILDER_BEAN_NAME);

        builder.addConstructorArgReference(composerBeanName);

        registry.registerBeanDefinition(targetBeanName, builder.getBeanDefinition());
    }

    private Set<String> getBasePackages(AnnotationMetadata importingClassMetadata) {
        Set<String> packages = new HashSet<>();

        Map<String, Object> map = importingClassMetadata.getAnnotationAttributes(EnableGraphComposer.class.getName());
        AnnotationAttributes attributes = AnnotationAttributes.fromMap(map);

        if (attributes != null) {
            String[] basePackages = attributes.getStringArray("basePackages");
            for (String pkg : basePackages) {
                if (StringUtils.hasText(pkg)) {
                    packages.add(pkg);
                }
            }
        }

        if (packages.isEmpty()) {
            packages.add(ClassUtils.getPackageName(importingClassMetadata.getClassName()));
        }

        return packages;
    }
}