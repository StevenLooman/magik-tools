package nl.ramsolutions.sw.magik.analysis.helpers;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

public class AssignmentExpressionNodeHelper {

  private final AstNode node;

  /**
   * Constructor.
   *
   * @param node Node to encapsulate.
   */
  public AssignmentExpressionNodeHelper(final AstNode node) {
    if (!node.is(MagikGrammar.ASSIGNMENT_EXPRESSION)) {
      throw new IllegalArgumentException();
    }

    this.node = node;
  }

  /**
   * Test if testNode is assigned to.
   *
   * @param testNode Node to test, an ATOM node.
   * @return True if the node is assigned to, false if not.
   */
  public boolean isAssignedTo(final AstNode testNode) {
    if (!testNode.is(MagikGrammar.ATOM)) {
      throw new IllegalArgumentException();
    }

    final AstNode rhsNode = this.node.getLastChild();
    final List<AstNode> lhsNodes = this.node.getChildren(MagikGrammar.values());
    lhsNodes.remove(rhsNode);
    return lhsNodes.contains(testNode);
  }
}
