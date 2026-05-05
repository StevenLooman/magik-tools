package nl.ramsolutions.sw.checks.magiktyped;

import com.sonar.sslr.api.AstNode;
import java.util.Collection;
import java.util.List;
import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ParameterDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ProcedureDefinition;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.ProcedureInvocationDefinitionHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.ProcedureInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.api.MagikKeyword;
import org.sonar.check.Rule;

/** Check if argument-count for invocation matches. */
@Rule(key = InvocationArgumentCountMatchesParameterCountTypedCheck.CHECK_KEY)
public class InvocationArgumentCountMatchesParameterCountTypedCheck extends MagikTypedCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "InvocationArgumentCountMatchesParameterCount";

  private static final String MESSAGE = "Not enough arguments for invocation: %s";

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
        resolver.getRespondingMethodDefinitions(calledTypeStr, methodName);
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
        final String message = MESSAGE.formatted(calledTypeStr.getFullString() + "." + methodName);
        this.addIssue(node, message);
      }
    }
  }

  @Override
  protected void walkPostProcedureInvocation(final AstNode node) {
    // Ensure there are arguments to check.
    final AstNode argumentsNode = node.getFirstChild(MagikGrammar.ARGUMENTS);
    if (argumentsNode == null) {
      return;
    }

    // Get type.
    final TypeString calledTypeStr =
        ProcedureInvocationDefinitionHelper.getProcedureTypeInvokedOn(
            node, this.getTypeReasonerState(), this.getTypeStringResolver());

    // Get procedures.
    final ProcedureInvocationNodeHelper helper = new ProcedureInvocationNodeHelper(node);
    final String invokedIdentifier = helper.getInvokedIdentifier();
    final Collection<ProcedureDefinition> procedureDefs =
        ProcedureInvocationDefinitionHelper.getRespondingProcedureDefinitions(
            node, this.getTypeReasonerState(), this.getTypeStringResolver(), this.getMagikFile());
    if (procedureDefs.isEmpty()) {
      // Cannot give any useful information, so abort.
      return;
    }
    for (final ProcedureDefinition procedureDef : procedureDefs) {
      final List<ParameterDefinition> parameterDefs = procedureDef.getParameters();
      if (parameterDefs.isEmpty()) {
        continue;
      }

      // Match arguments against procedure parameters.
      final List<AstNode> argumentNodes = argumentsNode.getChildren(MagikGrammar.ARGUMENT);
      final List<ParameterDefinition> checkedParameterDefs =
          parameterDefs.stream()
              .filter(parameter -> parameter.getModifier() == ParameterDefinition.Modifier.NONE)
              .toList();
      if (checkedParameterDefs.size() > argumentNodes.size()) {
        final String baseInvocationName =
            calledTypeStr.isUndefined() ? "procedure" : calledTypeStr.getFullString();
        final String invocationName =
            invokedIdentifier != null
                ? baseInvocationName + "." + invokedIdentifier
                : baseInvocationName;
        final String message = MESSAGE.formatted(invocationName);
        this.addIssue(node, message);
      }
    }
  }
}
