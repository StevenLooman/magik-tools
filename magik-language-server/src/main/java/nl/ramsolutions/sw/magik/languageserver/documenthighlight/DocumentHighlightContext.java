package nl.ramsolutions.sw.magik.languageserver.documenthighlight;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.magik.MagikTypedFile;

/**
 * Shared, pre-computed context passed to each {@link DocumentHighlightModule}.
 *
 * @param magikFile The Magik file document highlights are provided for.
 * @param identifierNode The identifier node at the position document highlights were requested for.
 */
public record DocumentHighlightContext(MagikTypedFile magikFile, AstNode identifierNode) {}
