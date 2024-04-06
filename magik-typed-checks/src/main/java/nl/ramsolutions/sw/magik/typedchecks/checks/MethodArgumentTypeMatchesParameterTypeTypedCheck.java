package nl.ramsolutions.sw.magik.typedchecks.checks;

import com.sonar.sslr.api.AstNode;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ParameterDefinition;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;
import org.sonar.check.Rule;

/** Check if argument types match parameter types. */
@Rule(key = MethodArgumentTypeMatchesParameterTypeTypedCheck.CHECK_KEY)
public class MethodArgumentTypeMatchesParameterTypeTypedCheck extends MagikTypedCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "MethodArgumentTypeMatchesParameterType";

  private static final String MESSAGE = "Argument type (%s) does not match parameter type (%s)";

  @SuppressWarnings("java:S3776")
  @Override
  protected void walkPostMethodInvocation(final AstNode node) {
    // Ensure there are arguments to check.
    final AstNode argumentsNode = node.getFirstChild(MagikGrammar.ARGUMENTS);
    if (argumentsNode == null) {
      return;
    }

    // Get type.
    final TypeStringResolver resolver = this.getTypeStringResolver();
    final TypeString typeStrInvokedOn = this.getTypeInvokedOn(node);
    final ExemplarDefinition exemplarDefinition = resolver.getExemplarDefinition(typeStrInvokedOn);
    if (exemplarDefinition == null) {
      // Cannot give any useful information, so abort.
      return;
    }

    // Get types for arguments.
    final LocalTypeReasonerState reasonerState = this.getTypeReasonerState();
    final List<AstNode> argumentNodes = argumentsNode.getChildren(MagikGrammar.ARGUMENT);
    final List<ExpressionResultString> argumentTypes =
        argumentNodes.stream()
            .map(argumentNode -> argumentNode.getFirstChild(MagikGrammar.EXPRESSION))
            .map(reasonerState::getNodeType)
            .toList();

    // Get methods.
    final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(node);
    final String methodName = helper.getMethodName();
    final Collection<MethodDefinition> methodDefs =
        resolver.getMethodDefinitions(typeStrInvokedOn, methodName);
    for (final MethodDefinition method : methodDefs) {
      final List<ParameterDefinition> parameterDefs = method.getParameters();
      if (parameterDefs.isEmpty()) {
        continue;
      }

      final List<TypeString> parameterTypes =
          parameterDefs.stream()
              .sequential()
              .filter(
                  parameterDef -> parameterDef.getModifier() != ParameterDefinition.Modifier.GATHER)
              .map(
                  parameterDef -> {
                    final TypeString paramTypeStr = parameterDef.getTypeName();
                    if (parameterDef.getModifier() == ParameterDefinition.Modifier.OPTIONAL) {
                      return TypeString.combine(paramTypeStr, TypeString.SW_UNSET);
                    }

                    return paramTypeStr;
                  })
              .toList();

      // Test parameter type against argument type.
      final int size = Math.min(parameterTypes.size(), argumentTypes.size());
      IntStream.range(0, size)
          .forEach(
              index -> {
                final TypeString parameterTypeStr = parameterTypes.get(index);
                // Don't test undefined types.
                if (parameterTypeStr.isUndefined()
                    || parameterTypeStr.isCombined()
                        && parameterTypeStr.getCombinedTypes().contains(TypeString.UNDEFINED)) {
                  return;
                }

                final TypeString argumentTypeStr =
                    argumentTypes.get(index).get(0, TypeString.UNDEFINED);
                if (argumentTypeStr.isUndefined()) {
                  // Don't test undefined arguments.
                  return;
                }

                if (!resolver.isKindOf(argumentTypeStr, parameterTypeStr)) {
                  final AstNode argumentNode = argumentNodes.get(index);
                  final String message =
                      String.format(
                          MESSAGE,
                          argumentTypeStr.getFullString(),
                          parameterTypeStr.getFullString());
                  this.addIssue(argumentNode, message);
                }
              });
    }
  }
}
