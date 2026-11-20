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
   * Test if testNode is assigned to by this assignment, rather than by any assignment as {@link
   * #isAssignmentTarget(AstNode)} tests.
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

  /**
   * Test if {@code atomNode} is written: the target of a plain, augmented or multiple assignment.
   *
   * @param atomNode ATOM node.
   * @return True if the ATOM is assigned to.
   */
  public static boolean isAssignmentTarget(final AstNode atomNode) {
    final AstNode parentNode = atomNode.getParent();
    if (parentNode.is(
        MagikGrammar.ASSIGNMENT_EXPRESSION, MagikGrammar.AUGMENTED_ASSIGNMENT_EXPRESSION)) {
      final AstNode lastChildNode = parentNode.getLastChild();
      return atomNode != lastChildNode;
    }

    final AstNode grandParentNode = parentNode.getParent();
    return parentNode.is(MagikGrammar.EXPRESSION)
        && grandParentNode != null
        && grandParentNode.is(MagikGrammar.MULTIPLE_ASSIGNMENT_ASSIGNABLES);
  }
}
