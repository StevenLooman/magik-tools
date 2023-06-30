package nl.ramsolutions.sw.magik.typedchecks.checks;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import java.util.stream.IntStream;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.LocalTypeReasoner;
import nl.ramsolutions.sw.magik.analysis.typing.TypeMatcher;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.CombinedType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResult;
import nl.ramsolutions.sw.magik.analysis.typing.types.Method;
import nl.ramsolutions.sw.magik.analysis.typing.types.Parameter;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.types.UndefinedType;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;

/**
 * Check if argument types match parameter types.
 */
public class MethodArgumentParameterTypedCheck extends MagikTypedCheck {

    private static final String MESSAGE = "Argument type (%s) does not match parameter type (%s)";

    @Override
    protected void walkPostMethodInvocation(final AstNode node) {
        // Ensure there are arguments to check.
        final AstNode argumentsNode = node.getFirstChild(MagikGrammar.ARGUMENTS);
        if (argumentsNode == null) {
            return;
        }

        // Get type.
        final AbstractType calledType = this.getTypeInvokedOn(node);
        if (calledType == UndefinedType.INSTANCE) {
            // Cannot give any useful information, so abort.
            return;
        }

        // Get types for arguments.
        final LocalTypeReasoner reasoner = this.getReasoner();
        final List<AstNode> argumentNodes = argumentsNode.getChildren(MagikGrammar.ARGUMENT).stream()
            .toList();
        final List<ExpressionResult> argumentTypes = argumentNodes.stream()
            .map(argumentNode -> argumentNode.getFirstChild(MagikGrammar.EXPRESSION))
            .map(reasoner::getNodeType)
            .toList();

        // Get methods.
        final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(node);
        final String methodName = helper.getMethodName();
        final AbstractType unsetType = this.getTypeKeeper().getType(TypeString.ofIdentifier("unset", "sw"));
        for (final Method method : calledType.getMethods(methodName)) {
            final List<Parameter> parameters = method.getParameters();
            if (parameters.isEmpty()) {
                continue;
            }

            final List<AbstractType> parameterTypes = method.getParameters().stream()
                .filter(parameter ->
                    parameter.is(Parameter.Modifier.NONE)
                    || parameter.is(Parameter.Modifier.OPTIONAL))  // Don't check gather.
                .map(parameter -> {
                    final TypeString paramTypeString = parameter.getType();
                    final AbstractType type = this.getTypeKeeper().getType(paramTypeString);
                    if (parameter.is(Parameter.Modifier.OPTIONAL)) {
                        return CombinedType.combine(type, unsetType);
                    }
                    return type;
                })
                .toList();

            // Test parameter type against argument type.
            final int size = Math.min(parameterTypes.size(), argumentTypes.size());
            IntStream.range(0, size)
                .forEach(index -> {
                    final AbstractType parameterType = parameterTypes.get(index);
                    if (parameterType == UndefinedType.INSTANCE) {
                        return;
                    }

                    final AbstractType argumentType = argumentTypes.get(index).get(0, UndefinedType.INSTANCE);
                    if (!TypeMatcher.typeMatches(argumentType, parameterType)) {
                        final AstNode argumentNode = argumentNodes.get(index);
                        final String message =
                            String.format(MESSAGE, argumentType.getFullName(), parameterType.getFullName());
                        this.addIssue(argumentNode, message);
                    }
                });
        }

    }

}
