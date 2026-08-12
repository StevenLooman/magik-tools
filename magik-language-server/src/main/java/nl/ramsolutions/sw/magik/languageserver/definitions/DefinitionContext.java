package nl.ramsolutions.sw.magik.languageserver.definitions;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.OpenedFile;

/**
 * Shared, pre-computed context passed to each {@link DefinitionModule}.
 *
 * @param <T> Type of {@link OpenedFile} definitions are provided for.
 * @param file The file definitions are provided for.
 * @param positionNode The node at the position definitions were requested for.
 */
public record DefinitionContext<T extends OpenedFile>(T file, AstNode positionNode) {}
