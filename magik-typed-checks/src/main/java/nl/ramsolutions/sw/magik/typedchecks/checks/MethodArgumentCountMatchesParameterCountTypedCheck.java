package nl.ramsolutions.sw.magik.typedchecks.checks;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.Method;
import nl.ramsolutions.sw.magik.analysis.typing.types.Parameter;
import nl.ramsolutions.sw.magik.analysis.typing.types.UndefinedType;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.api.MagikKeyword;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;
import org.sonar.check.Rule;

/** Check if argument-count for method invocation matches. */
@Rule(key = MethodArgumentCountMatchesParameterCountTypedCheck.CHECK_KEY)
public class MethodArgumentCountMatchesParameterCountTypedCheck extends MagikTypedCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "MethodArgumentCountMatchesParameterCount";

  private static final String MESSAGE = "Not enough arguments for method: %s";

  @Override
  protected void walkPostMethodInvocation(final AstNode node) {
    // Ensure there are arguments to check.
    final AstNode argumentsNode = node.getFirstChild(MagikGrammar.ARGUMENTS);
    if (argumentsNode == null) {
      return;
    }

    // Don't bother checking scatter.
    final boolean anyScatter =
        argumentsNode.getChildren(MagikGrammar.ARGUMENT).stream()
            .anyMatch(
                argumentNode -> {
                  AstNode unaryExprNode =
                      AstQuery.getFirstChildFromChain(
                          node, MagikGrammar.EXPRESSION, MagikGrammar.UNARY_EXPRESSION);
                  String tokenValue = unaryExprNode != null ? unaryExprNode.getTokenValue() : null;
                  return tokenValue != null
                      && tokenValue.equalsIgnoreCase(MagikKeyword.SCATTER.getValue());
                });
    if (anyScatter) {
      return;
    }

    // Get type.
    final AbstractType calledType = this.getTypeInvokedOn(node);
    if (calledType == UndefinedType.INSTANCE) {
      // Cannot give any useful information, so abort.
      return;
    }

    // Get methods.
    final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(node);
    final String methodName = helper.getMethodName();
    for (final Method method : calledType.getMethods(methodName)) {
      final List<Parameter> parameters = method.getParameters();
      if (parameters.isEmpty()) {
        continue;
      }

      // Match arguments against method.parameters.
      final List<AstNode> argumentNodes = argumentsNode.getChildren(MagikGrammar.ARGUMENT);
      final List<Parameter> checkedParameters =
          method.getParameters().stream()
              .filter(parameter -> parameter.is(Parameter.Modifier.NONE))
              .toList();
      if (checkedParameters.size() > argumentNodes.size()) {
        final String message = String.format(MESSAGE, methodName);
        this.addIssue(node, message);
      }
    }
  }
}
