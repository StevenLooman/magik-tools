package nl.ramsolutions.sw.magik.analysis.helpers;

import com.sonar.sslr.api.AstNode;
import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.api.MagikOperator;
import nl.ramsolutions.sw.magik.api.MagikPunctuator;

/** Helper for METHOD_INVOCATION nodes. */
public class MethodInvocationNodeHelper {

  private final AstNode node;

  /**
   * Constructor.
   *
   * @param node Node to encapsulate.
   */
  public MethodInvocationNodeHelper(final AstNode node) {
    if (!node.is(MagikGrammar.METHOD_INVOCATION)) {
      throw new IllegalArgumentException();
    }

    this.node = node;
  }

  /**
   * Test if encapsulated node is a method invication with expected name.
   *
   * @param methodName Expected name.
   * @return True if method invocation is of given name, false otherwise.
   */
  public boolean isMethodInvocationOf(final String methodName) {
    return this.getMethodName().equals(methodName);
  }

  /**
   * Test if encapsulated node is a method invication with expected receiver name and method name.
   *
   * @param receiverIdentifier Receiver identifier.
   * @param methodName Method name.
   * @return True if method invocation is of given receiver name and method name, false otherwise.
   */
  public boolean isMethodInvocationOf(final String receiverIdentifier, final String methodName) {
    final AstNode receiverNode = this.getReceiverNode();
    if (receiverNode == null || !receiverNode.is(MagikGrammar.ATOM)) {
      return false;
    }

    final String identifier = receiverNode.getTokenValue();
    if (!identifier.equalsIgnoreCase(receiverIdentifier)) {
      return false;
    }

    return this.isMethodInvocationOf(methodName);
  }

  /**
   * Get the node the method is being invoked on.
   *
   * @return Receiver node.
   */
  public AstNode getReceiverNode() {
    return this.node.getPreviousSibling();
  }

  /**
   * Get invoked method name.
   *
   * @return Invoked method name.
   */
  public String getMethodName() {
    // Get arguments
    final AstNode argumentsNode = node.getFirstChild(MagikGrammar.ARGUMENTS);
    final List<AstNode> argumentNodes =
        argumentsNode != null
            ? argumentsNode.getChildren(MagikGrammar.ARGUMENT)
            : Collections.emptyList();

    // Construct name.
    String methodName = "";
    final AstNode identifierNode = node.getFirstChild(MagikGrammar.IDENTIFIER);
    methodName += identifierNode != null ? identifierNode.getTokenValue() : "";
    if (argumentsNode != null) {
      if (this.anyChildTokenIs(argumentsNode, MagikPunctuator.SQUARE_L)) {
        methodName += "[";
        int commaCount = Math.max(argumentNodes.size() - 1, 0);
        methodName += ",".repeat(commaCount);
        methodName += "]";
      }
      if (this.anyChildTokenIs(argumentsNode, MagikPunctuator.PAREN_L)) {
        methodName += MagikPunctuator.PAREN_L.getValue() + MagikPunctuator.PAREN_R.getValue();
      }
    }
    if (this.anyChildTokenIs(node, MagikOperator.CHEVRON)) {
      methodName += MagikOperator.CHEVRON.getValue();
    }
    if (this.anyChildTokenIs(node, MagikOperator.BOOT_CHEVRON)) {
      methodName += MagikOperator.BOOT_CHEVRON.getValue();
    }

    return methodName;
  }

  /**
   * Get ARGUMENTS -> ARGUMENT -> EXPRESSION nodes.
   *
   * @return
   */
  public List<AstNode> getArgumentExpressionNodes() {
    final AstNode argumentsNode = this.node.getFirstChild(MagikGrammar.ARGUMENTS);
    if (argumentsNode == null) {
      return Collections.emptyList();
    }

    return argumentsNode.getChildren(MagikGrammar.ARGUMENT).stream()
        .map(AstNode::getFirstChild)
        .toList();
  }

  private boolean anyChildTokenIs(final AstNode parentNode, final MagikOperator magikOperator) {
    return parentNode.getChildren().stream()
        .filter(childNode -> childNode.isNot(MagikGrammar.values()))
        .map(AstNode::getTokenValue)
        .anyMatch(tokenValue -> tokenValue.equalsIgnoreCase(magikOperator.getValue()));
  }

  private boolean anyChildTokenIs(final AstNode parentNode, final MagikPunctuator magikPunctuator) {
    return parentNode.getChildren().stream()
        .filter(childNode -> childNode.isNot(MagikGrammar.values()))
        .map(AstNode::getTokenValue)
        .anyMatch(tokenValue -> tokenValue.equalsIgnoreCase(magikPunctuator.getValue()));
  }
}
