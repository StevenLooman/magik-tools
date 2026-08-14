package nl.ramsolutions.sw.magik.languageserver.typehierarchy;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.magik.MagikTypedFile;

/**
 * Shared, pre-computed context passed to each {@link TypeHierarchyModule}.
 *
 * @param file The file the type hierarchy is provided for.
 * @param positionNode The identifier node at the position the type hierarchy was requested for.
 */
public record TypeHierarchyContext(MagikTypedFile file, AstNode positionNode) {}
