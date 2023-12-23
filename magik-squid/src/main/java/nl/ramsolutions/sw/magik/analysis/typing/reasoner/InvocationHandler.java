package nl.ramsolutions.sw.magik.analysis.typing.reasoner;

import com.sonar.sslr.api.AstNode;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.AliasType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResult;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.types.Method;
import nl.ramsolutions.sw.magik.analysis.typing.types.Parameter;
import nl.ramsolutions.sw.magik.analysis.typing.types.ParameterReferenceType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ProcedureInstance;
import nl.ramsolutions.sw.magik.analysis.typing.types.SelfType;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;

/**
 * Invocation handler.
 */
class InvocationHandler extends LocalTypeReasonerHandler {

    private static final TypeString SW_PROCEDURE = TypeString.ofIdentifier("procedure", "sw");

    /**
     * Constructor.
     * @param state Reasoner state.
     */
    InvocationHandler(final LocalTypeReasonerState state) {
        super(state);
    }

    /**
     * Handle method invocation.
     * @param node METHOD_INVOCATION node.
     */
    void handleMethodInvocation(final AstNode node) {
        final AbstractType unsetType = this.typeKeeper.getType(TypeString.SW_UNSET);

        // Get called type for method.
        final AstNode calledNode = node.getPreviousSibling();
        final ExpressionResult calledResult = this.state.getNodeType(calledNode);
        final AbstractType originalCalledType = calledResult.get(0, unsetType);
        final AbstractType methodOwnerType = this.getMethodOwnerType(node);
        final AbstractType calledType = calledResult.
            substituteType(SelfType.INSTANCE, methodOwnerType).
            get(0, unsetType);

        // Perform method call and store iterator result(s).
        final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(node);
        final String methodName = helper.getMethodName();
        final Collection<Method> methods = calledType.getMethods(methodName);
        ExpressionResult callResult = null;
        ExpressionResult iterCallResult = null;
        if (methods.isEmpty()) {
            // Method not found, we cannot known what the results will be.
            callResult = ExpressionResult.UNDEFINED;
            iterCallResult = ExpressionResult.UNDEFINED;
        } else {
            final List<AstNode> argumentExpressionNodes = helper.getArgumentExpressionNodes();
            final List<AbstractType> argumentTypes = argumentExpressionNodes.stream()
                .map(exprNode -> this.state.getNodeType(exprNode).get(0, unsetType))
                .collect(Collectors.toList());
            for (final Method method : methods) {
                // Call result.
                final ExpressionResultString methodCallResultStr = method.getCallResult();
                final ExpressionResult methodCallResultBare =
                    this.typeReader.parseExpressionResultString(methodCallResultStr);
                ExpressionResult methodCallResult = originalCalledType != SelfType.INSTANCE
                    ? methodCallResultBare.substituteType(SelfType.INSTANCE, calledType)
                    : methodCallResultBare;

                // Substitute parameters.
                methodCallResult =
                    this.substituteParametersForMethodCallResult(method, argumentTypes, methodCallResult);

                // Merge result.
                callResult = new ExpressionResult(methodCallResult, callResult, unsetType);

                // Iterator result.
                final ExpressionResultString loopbodyResultStr = method.getLoopbodyResult();
                final ExpressionResult loopbodyResultBare =
                    this.typeReader.parseExpressionResultString(loopbodyResultStr);
                final ExpressionResult methodIterCallResult = originalCalledType != SelfType.INSTANCE
                    ? loopbodyResultBare.substituteType(SelfType.INSTANCE, calledType)
                    : loopbodyResultBare;
                iterCallResult = new ExpressionResult(methodIterCallResult, iterCallResult, unsetType);
            }
        }

        // Store it!
        Objects.requireNonNull(callResult);
        this.state.setNodeType(node, callResult);
        Objects.requireNonNull(iterCallResult);
        this.state.setNodeIterType(node, iterCallResult);
    }

    /**
     * Handle procedure invocation node.
     * @param node PROCEDURE_INVOCATION node.
     */
    void handleProcedureInvocation(final AstNode node) {
        final AbstractType unsetType = this.typeKeeper.getType(TypeString.SW_UNSET);

        // TODO: Handle sw:obj/sw:prototype.

        // Get called type for invocation.
        final AstNode calledNode = node.getPreviousSibling();
        final ExpressionResult calledNodeResult = this.state.getNodeType(calledNode);
        final AbstractType originalCalledNodeType = calledNodeResult.get(0, unsetType);
        AbstractType calledType = originalCalledNodeType;

        if (calledType == SelfType.INSTANCE) {
            // Replace self type with concrete type, need to know the method we call.
            calledType = this.typeKeeper.getType(SW_PROCEDURE);
        } else if (calledType instanceof AliasType) {
            final AliasType aliasTyped = (AliasType) calledType;
            calledType = aliasTyped.getAliasedType();
        }

        // Perform procedure call.
        ExpressionResult callResult = null;
        ExpressionResult iterCallResult = null;
        if (calledType instanceof ProcedureInstance) {
            final ProcedureInstance procedureType = (ProcedureInstance) calledType;
            final Collection<Method> methods = procedureType.getMethods("invoke()");
            final Method method = methods.stream().findAny().orElse(null);
            Objects.requireNonNull(method);

            final ExpressionResultString callResultStr = method.getCallResult();
            callResult = this.typeReader.parseExpressionResultString(callResultStr);

            final ExpressionResultString iterCallResultStr = method.getLoopbodyResult();
            iterCallResult = this.typeReader.parseExpressionResultString(iterCallResultStr);

            if (originalCalledNodeType == SelfType.INSTANCE) {
                callResult = callResult.substituteType(SelfType.INSTANCE, calledType);
                iterCallResult = iterCallResult.substituteType(SelfType.INSTANCE, calledType);
            }
        }

        // If nothing, then undefined.
        if (callResult == null) {
            callResult = ExpressionResult.UNDEFINED;
        }
        if (iterCallResult == null) {
            iterCallResult = ExpressionResult.UNDEFINED;
        }

        // Store it!
        this.state.setNodeType(node, callResult);
        this.state.setNodeIterType(node, iterCallResult);
    }

    private ExpressionResult substituteParametersForMethodCallResult(
            final Method method,
            final List<AbstractType> argumentTypes,
            final ExpressionResult methodCallResult) {
        final AbstractType unsetType = this.typeKeeper.getType(TypeString.SW_UNSET);

        ExpressionResult result = methodCallResult;
        final Map<ParameterReferenceType, AbstractType> paramRefTypeMap = IntStream
            .range(0, method.getParameters().size())
            .mapToObj(i -> {
                final Parameter param = method.getParameters().get(i);
                final String paramName = param.getName();
                final ParameterReferenceType paramRefType = new ParameterReferenceType(paramName);
                final AbstractType argType = i < argumentTypes.size()
                    ? argumentTypes.get(i)
                    : unsetType;  // TODO: What about gather parameters?
                return new AbstractMap.SimpleEntry<>(paramRefType, argType);
            })
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue));
        for (final Map.Entry<ParameterReferenceType, AbstractType> entry : paramRefTypeMap.entrySet()) {
            final ParameterReferenceType paramRefType = entry.getKey();
            final AbstractType argType = entry.getValue();
            result = result.substituteType(paramRefType, argType);
        }
        return result;
    }

}
