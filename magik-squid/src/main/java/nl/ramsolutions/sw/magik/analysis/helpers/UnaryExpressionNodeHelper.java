package nl.ramsolutions.sw.magik.analysis.helpers;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

public class UnaryExpressionNodeHelper {

  private final AstNode node;

  /**
   * Constructor.
   *
   * @param node Node to encapsulate.
   */
  public UnaryExpressionNodeHelper(final AstNode node) {
    if (!node.is(MagikGrammar.UNARY_EXPRESSION)) {
      throw new IllegalArgumentException();
    }

    this.node = node;
  }

  public String getOperator() {
    return this.node.getTokenValue();
  }

  public AstNode getReceiverNode() {
    return this.node.getChildren().get(1);
  }
}
