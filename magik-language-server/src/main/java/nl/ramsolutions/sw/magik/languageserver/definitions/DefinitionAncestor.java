package nl.ramsolutions.sw.magik.languageserver.definitions;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/**
 * Shared ancestor gate for the Magik definition modules. The constructs they handle are mutually
 * exclusive, so exactly one module's guard can match the nearest ancestor found here.
 */
final class DefinitionAncestor {

  private DefinitionAncestor() {}

  @CheckForNull
  static AstNode nearest(final AstNode positionNode) {
    return positionNode.getFirstAncestor(
        MagikGrammar.METHOD_INVOCATION,
        MagikGrammar.ATOM,
        MagikGrammar.CONDITION_NAME,
        MagikGrammar.SLOT);
  }
}
