package nl.ramsolutions.sw.magik.languageserver.documenthighlight;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/**
 * Shared ancestor gate for the stage-2 (fallback) document highlight modules. The constructs they
 * handle are mutually exclusive, so exactly one module's guard can match the nearest ancestor found
 * here.
 */
final class DocumentHighlightAncestor {

  private DocumentHighlightAncestor() {}

  @CheckForNull
  static AstNode nearest(final AstNode identifierNode) {
    return identifierNode.getFirstAncestor(
        MagikGrammar.METHOD_NAME,
        MagikGrammar.EXEMPLAR_NAME,
        MagikGrammar.CONDITION_NAME,
        MagikGrammar.SLOT);
  }
}
