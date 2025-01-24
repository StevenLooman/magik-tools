package nl.ramsolutions.sw.magik.analysis.helpers;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/** Helper for SLOT nodes. */
public class SlotNodeHelper {

  private final AstNode node;

  /**
   * Constructor.
   *
   * @param node Node to encapsulate.
   */
  public SlotNodeHelper(final AstNode node) {
    if (!node.is(MagikGrammar.SLOT)) {
      throw new IllegalArgumentException();
    }

    this.node = node;
  }

  public String getSlotName() {
    final AstNode identifierNode = node.getFirstDescendant(MagikGrammar.IDENTIFIER);
    return identifierNode.getTokenValue();
  }

  /**
   * Get the exemplar type string of the method.
   *
   * @return The exemplar {@link TypeString} of the method.
   */
  public TypeString getMethodExemplarTypeString() {
    final AstNode methodDefinitionNode = this.node.getFirstAncestor(MagikGrammar.METHOD_DEFINITION);
    final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(methodDefinitionNode);
    return helper.getExemplarTypeString();
  }
}
