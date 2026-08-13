package nl.ramsolutions.sw.magik.languageserver.references;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.OpenedFile;

/**
 * Shared, pre-computed context passed to each {@link ReferencesModule}.
 *
 * @param <T> Type of {@link OpenedFile} references are provided for.
 * @param file The file references are provided for.
 * @param positionNode The node at the position references were requested for.
 */
public record ReferencesContext<T extends OpenedFile>(T file, AstNode positionNode) {}
