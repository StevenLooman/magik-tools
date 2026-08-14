package nl.ramsolutions.sw.magik.languageserver.references;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/**
 * Shared ancestor gate for the Magik references modules. The constructs they handle are mutually
 * exclusive, so exactly one module's guard can match the nearest ancestor found here.
 */
final class ReferencesAncestor {

  private ReferencesAncestor() {}

  @CheckForNull
  static AstNode nearest(final AstNode positionNode) {
    return positionNode.getFirstAncestor(
        MagikGrammar.METHOD_NAME,
        MagikGrammar.EXEMPLAR_NAME,
        MagikGrammar.CONDITION_NAME,
        MagikGrammar.SLOT,
        MagikGrammar.ATOM);
  }
}
