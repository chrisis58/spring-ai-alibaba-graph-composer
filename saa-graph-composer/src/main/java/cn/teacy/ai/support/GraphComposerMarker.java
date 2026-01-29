package cn.teacy.ai.support;

/**
 * Marker class used to signal that the Graph Composer module has been manually enabled.
 * * <p>This class is automatically registered as a Bean when {@link cn.teacy.ai.annotation.EnableGraphComposer}
 * is used in an application. Its primary purpose is to serve as a "state signal" for
 * Spring Boot's conditional configuration.
 * * <p>The presence of this bean informs the Auto-Configuration system
 * to back off, ensuring that manual configuration takes precedence
 * and preventing duplicate registration of graph-related components.
 *
 * @since 0.3.0
 * @see cn.teacy.ai.annotation.EnableGraphComposer
 */
public final class GraphComposerMarker {
}
