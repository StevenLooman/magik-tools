package nl.ramsolutions.sw.magik.analysis.helpers;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/** Helper for FOR nodes. */
public class ForNodeHelper {

  private final AstNode node;

  /**
   * Constructor.
   *
   * @param node Node to encapsulate.
   */
  public ForNodeHelper(final AstNode node) {
    if (!node.is(MagikGrammar.FOR)) {
      throw new IllegalArgumentException();
    }

    this.node = node;
  }

  public List<AstNode> getLoopIdentifierNodes() {
    return AstQuery.getChildrenFromChain(
        this.node,
        MagikGrammar.FOR_VARIABLES,
        MagikGrammar.IDENTIFIERS_WITH_GATHER,
        MagikGrammar.IDENTIFIER);
  }
}
