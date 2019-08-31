package org.stevenlooman.sw.magik.checks;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import org.sonar.check.Rule;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.api.MagikGrammar;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;

@Rule(key = SizeZeroEmptyCheck.CHECK_KEY)
public class SizeZeroEmptyCheck extends MagikCheck {

  private static final String MESSAGE = "Use 'empty?' instead of 'size = 0'.";
  public static final String CHECK_KEY = "SizeZeroEmpty";

  @Override
  public boolean isTemplatedCheck() {
    return false;
  }

  @Override
  public List<AstNodeType> subscribedTo() {
    return Arrays.asList(
        MagikGrammar.EQUALITY_EXPRESSION);
  }

  @Override
  public void visitNode(AstNode node) {
    AstNode leftHandSide = node.getFirstChild();
    AstNode rightHandSide = node.getLastChild();
    if (hasEqOperator(node)
        && (isMethodInvocationSize(leftHandSide) && isNumberZero(rightHandSide)
            || isMethodInvocationSize(rightHandSide) && isNumberZero(leftHandSide))) {
      addIssue(MESSAGE, node);
    }
  }

  private boolean hasEqOperator(AstNode node) {
    AstNode operatorNode = node.getChildren().get(1);
    return operatorNode.getTokenValue().equals("=")
           || operatorNode.getTokenValue().equals(">=")
           || operatorNode.getTokenValue().equals("_is");
  }

  private boolean isMethodInvocationSize(AstNode node) {
    AstNode methodInvocationNode = getLastDescendant(node, MagikGrammar.METHOD_INVOCATION);
    if (methodInvocationNode == null) {
      return false;
    }

    AstNode identifierNode = methodInvocationNode.getFirstDescendant(MagikGrammar.IDENTIFIER);
    return identifierNode != null
        && identifierNode.getTokenValue().equals("size");
  }

  private boolean isNumberZero(AstNode node) {
    AstNode numberNode = node.getFirstDescendant(MagikGrammar.NUMBER);
    return numberNode != null
           && numberNode.getTokenValue().equals("0");
  }

  @Nullable
  private AstNode getLastDescendant(AstNode node, AstNodeType nodeType) {
    List<AstNode> children = node.getChildren();
    Collections.reverse(children);
    Iterator<AstNode> childrenIter = children.iterator();

    AstNode childNode;
    do {
      if (!childrenIter.hasNext()) {
        return null;
      }

      AstNode child = (AstNode)childrenIter.next();
      if (child.is(nodeType)) {
        return child;
      }

      childNode = getLastDescendant(child, nodeType);
    } while (childNode == null);

    return null;
  }
}
