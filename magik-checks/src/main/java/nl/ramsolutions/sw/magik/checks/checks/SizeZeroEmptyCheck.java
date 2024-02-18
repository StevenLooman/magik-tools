package nl.ramsolutions.sw.magik.checks.checks;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import org.sonar.check.Rule;

/** Check if `.size = 0` was used, where `.empty?` should be used. */
@Rule(key = SizeZeroEmptyCheck.CHECK_KEY)
public class SizeZeroEmptyCheck extends MagikCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "SizeZeroEmpty";

  private static final String MESSAGE = "Use 'empty?' instead of 'size = 0'.";

  @Override
  protected void walkPreEqualityExpression(final AstNode node) {
    final AstNode leftHandSide = node.getFirstChild();
    final AstNode rightHandSide = node.getLastChild();
    if (hasEqOperator(node)
        && (isMethodInvocationSize(leftHandSide) && isNumberZero(rightHandSide)
            || isMethodInvocationSize(rightHandSide) && isNumberZero(leftHandSide))) {
      this.addIssue(node, MESSAGE);
    }
  }

  private boolean hasEqOperator(final AstNode node) {
    final AstNode operatorNode = node.getChildren().get(1);
    return operatorNode.getTokenValue().equals("=")
        || operatorNode.getTokenValue().equals(">=")
        || operatorNode.getTokenValue().equals("_is");
  }

  private boolean isMethodInvocationSize(final AstNode node) {
    final AstNode methodInvocationNode = getLastDescendant(node, MagikGrammar.METHOD_INVOCATION);
    if (methodInvocationNode == null) {
      return false;
    }

    final AstNode identifierNode = methodInvocationNode.getFirstDescendant(MagikGrammar.IDENTIFIER);
    return identifierNode != null && identifierNode.getTokenValue().equals("size");
  }

  private boolean isNumberZero(final AstNode node) {
    final AstNode numberNode = node.getFirstDescendant(MagikGrammar.NUMBER);
    return numberNode != null && numberNode.getTokenValue().equals("0");
  }

  @CheckForNull
  private AstNode getLastDescendant(final AstNode node, final AstNodeType nodeType) {
    final List<AstNode> children = new ArrayList<>(node.getChildren());
    Collections.reverse(children);
    final Iterator<AstNode> childrenIter = children.iterator();

    AstNode childNode;
    do {
      if (!childrenIter.hasNext()) {
        return null;
      }

      final AstNode child = childrenIter.next();
      if (child.is(nodeType)) {
        return child;
      }

      childNode = getLastDescendant(child, nodeType);
    } while (childNode == null);

    return null;
  }
}
