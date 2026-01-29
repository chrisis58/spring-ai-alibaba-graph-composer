package cn.teacy.ai.annotation;

import cn.teacy.ai.core.GraphAutoRegistrar;
import cn.teacy.ai.support.GraphComposerMarker;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({GraphAutoRegistrar.class, GraphComposerMarker.class})
public @interface EnableGraphComposer {

    /**
     * Leave empty to scan the package of the annotated class.
     */
    String[] basePackages() default {};

}