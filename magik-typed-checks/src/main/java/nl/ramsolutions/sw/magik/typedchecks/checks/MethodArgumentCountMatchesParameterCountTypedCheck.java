package nl.ramsolutions.sw.magik.typedchecks.checks;

import com.sonar.sslr.api.AstNode;
import java.util.Collection;
import java.util.List;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ParameterDefinition;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
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
    final TypeString calledTypeStr = this.getTypeInvokedOn(node);
    if (calledTypeStr.isUndefined()) {
      // Cannot give any useful information, so abort.
      return;
    }

    // Get methods.
    final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(node);
    final String methodName = helper.getMethodName();
    final TypeStringResolver resolver = this.getTypeStringResolver();
    final Collection<MethodDefinition> methodDefs =
        resolver.getMethodDefinitions(calledTypeStr, methodName);
    for (final MethodDefinition methodDef : methodDefs) {
      final List<ParameterDefinition> parameterDefs = methodDef.getParameters();
      if (parameterDefs.isEmpty()) {
        continue;
      }

      // Match arguments against method.parameters.
      final List<AstNode> argumentNodes = argumentsNode.getChildren(MagikGrammar.ARGUMENT);
      final List<ParameterDefinition> checkedParameterDefs =
          parameterDefs.stream()
              .filter(parameter -> parameter.getModifier() == ParameterDefinition.Modifier.NONE)
              .toList();
      if (checkedParameterDefs.size() > argumentNodes.size()) {
        final String message = String.format(MESSAGE, methodName);
        this.addIssue(node, message);
      }
    }
  }
}
