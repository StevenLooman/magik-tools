package nl.ramsolutions.sw.magik.analysis.helpers;

import com.sonar.sslr.api.AstNode;
import java.util.ArrayList;
import java.util.List;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.utils.Triple;

public class BinaryExpressionNodeHelper {

  private final AstNode node;

  /**
   * Constructor.
   *
   * @param node Node to encapsulate.
   */
  public BinaryExpressionNodeHelper(final AstNode node) {
    if (!node.is(
        // TODO: Combined assignments.
        MagikGrammar.OR_EXPRESSION,
        MagikGrammar.XOR_EXPRESSION,
        MagikGrammar.AND_EXPRESSION,
        MagikGrammar.EQUALITY_EXPRESSION,
        MagikGrammar.RELATIONAL_EXPRESSION,
        MagikGrammar.ADDITIVE_EXPRESSION,
        MagikGrammar.MULTIPLICATIVE_EXPRESSION,
        MagikGrammar.EXPONENTIAL_EXPRESSION)) {
      throw new IllegalArgumentException();
    }

    this.node = node;
  }

  public AstNode getLhsNode() {
    return this.node.getFirstChild();
  }

  /**
   * Get triplets of LHS operator or previous operator node, operator node, RHS operator node.
   *
   * @return Triplets of LHS operator or previous operator node, operator node, RHS operator node.
   */
  public List<Triple<AstNode, AstNode, AstNode>> getTriplets() {
    final List<Triple<AstNode, AstNode, AstNode>> entries = new ArrayList<>();

    final List<AstNode> chainNodes = node.getChildren();
    for (int i = 0; i < chainNodes.size() - 2; i += 2) {
      final AstNode leftNode = i == 0 ? chainNodes.get(i) : chainNodes.get(i - 1);
      final AstNode operatorNode = chainNodes.get(i + 1);
      final AstNode rightNode = chainNodes.get(i + 2);

      final Triple<AstNode, AstNode, AstNode> triplet =
          new Triple<>(leftNode, operatorNode, rightNode);
      entries.add(triplet);
    }

    return entries;
  }
}
