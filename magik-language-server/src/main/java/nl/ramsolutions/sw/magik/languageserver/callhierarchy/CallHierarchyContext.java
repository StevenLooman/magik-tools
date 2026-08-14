package nl.ramsolutions.sw.magik.languageserver.callhierarchy;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.magik.MagikTypedFile;

/**
 * Shared, pre-computed context passed to each {@link CallHierarchyModule}.
 *
 * @param magikFile The Magik file call hierarchy items are provided for.
 * @param wantedNode The {@code METHOD_DEFINITION} or {@code PROCEDURE_DEFINITION} node surrounding
 *     the position call hierarchy items were requested for.
 */
public record CallHierarchyContext(MagikTypedFile magikFile, AstNode wantedNode) {}
