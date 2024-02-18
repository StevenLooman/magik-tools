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
import nl.ramsolutions.sw.magik.analysis.helpers.ProcedureInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.GenericHelper;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.AliasType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResult;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.types.Method;
import nl.ramsolutions.sw.magik.analysis.typing.types.Parameter;
import nl.ramsolutions.sw.magik.analysis.typing.types.ProcedureInstance;
import nl.ramsolutions.sw.magik.analysis.typing.types.SelfType;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;

/** Invocation handler. */
class InvocationHandler extends LocalTypeReasonerHandler {

  /**
   * Constructor.
   *
   * @param state Reasoner state.
   */
  InvocationHandler(final LocalTypeReasonerState state) {
    super(state);
  }

  /**
   * Handle method invocation.
   *
   * @param node METHOD_INVOCATION node.
   */
  void handleMethodInvocation(final AstNode node) {
    final AbstractType unsetType = this.typeKeeper.getType(TypeString.SW_UNSET);

    // Get called type for method.
    final AstNode calledNode = node.getPreviousSibling();
    final ExpressionResult calledResult = this.state.getNodeType(calledNode);
    final AbstractType originalCalledType = calledResult.get(0, unsetType);
    final AbstractType methodOwnerType = this.getMethodOwnerType(node);
    final AbstractType calledType =
        calledResult.substituteType(SelfType.INSTANCE, methodOwnerType).get(0, unsetType);

    // Perform method call and store iterator result(s).
    final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(node);
    final String methodName = helper.getMethodName();
    final Collection<Method> methods = calledType.getMethods(methodName);
    ExpressionResult callResult = null;
    ExpressionResult iterResult = null;
    if (methods.isEmpty()) {
      // Method not found, we cannot known what the results will be.
      callResult = ExpressionResult.UNDEFINED;
      iterResult = ExpressionResult.UNDEFINED;
    } else {
      final List<AstNode> argumentExpressionNodes = helper.getArgumentExpressionNodes();
      final List<TypeString> argumentTypeStrs =
          argumentExpressionNodes.stream()
              .map(exprNode -> this.state.getNodeType(exprNode).get(0, unsetType))
              .map(AbstractType::getTypeString)
              .collect(Collectors.toList());
      for (final Method method : methods) {
        // Handle call result.
        ExpressionResultString methodCallResultStr = method.getCallResult();
        final ExpressionResultString processedMethodCallResultStr =
            this.processExpressionResult(
                originalCalledType, calledType, method, methodCallResultStr, argumentTypeStrs);
        final ExpressionResult processedMethodCallResult =
            this.typeReader.parseExpressionResultString(processedMethodCallResultStr);
        callResult = new ExpressionResult(processedMethodCallResult, callResult, unsetType);

        // Handle iter result.
        final ExpressionResultString methodIterResultStr = method.getLoopbodyResult();
        final ExpressionResultString processedMethodIterResultStr =
            this.processExpressionResult(
                originalCalledType, calledType, method, methodIterResultStr, argumentTypeStrs);
        final ExpressionResult processedMethodIterResult =
            this.typeReader.parseExpressionResultString(processedMethodIterResultStr);
        iterResult = new ExpressionResult(processedMethodIterResult, iterResult, unsetType);
      }
    }

    // Store it!
    Objects.requireNonNull(callResult);
    Objects.requireNonNull(iterResult);
    this.state.setNodeType(node, callResult);
    this.state.setNodeIterType(node, iterResult);
  }

  /**
   * Handle procedure invocation node.
   *
   * @param node PROCEDURE_INVOCATION node.
   */
  void handleProcedureInvocation(final AstNode node) {
    final AbstractType unsetType = this.typeKeeper.getType(TypeString.SW_UNSET);

    // TODO: Handle sw:obj/sw:prototype.

    // Get called type for invocation.
    final AstNode calledNode = node.getPreviousSibling();
    final ExpressionResult calledNodeResult = this.state.getNodeType(calledNode);
    final AbstractType originalCalledType = calledNodeResult.get(0, unsetType);
    final AbstractType calledType =
        originalCalledType == SelfType.INSTANCE
            ? this.typeKeeper.getType(TypeString.SW_PROCEDURE)
            : originalCalledType instanceof AliasType // NOSONAR
                ? ((AliasType) originalCalledType).getAliasedType()
                : originalCalledType;

    // Perform procedure call.
    ExpressionResult callResult = null;
    ExpressionResult iterResult = null;
    if (calledType instanceof ProcedureInstance) {
      final ProcedureInstance procedureType = (ProcedureInstance) calledType;
      final Collection<Method> methods = procedureType.getMethods("invoke()");
      final Method method = methods.iterator().next();

      // Figure argument types.
      final ProcedureInvocationNodeHelper helper = new ProcedureInvocationNodeHelper(node);
      final List<AstNode> argumentExpressionNodes = helper.getArgumentExpressionNodes();
      final List<TypeString> argumentTypeStrs =
          argumentExpressionNodes.stream()
              .map(exprNode -> this.state.getNodeType(exprNode).get(0, unsetType))
              .map(AbstractType::getTypeString)
              .collect(Collectors.toList());

      // Handle call result.
      ExpressionResultString methodCallResultStr = method.getCallResult();
      final ExpressionResultString processedMethodCallResultStr =
          this.processExpressionResult(
              originalCalledType, calledType, method, methodCallResultStr, argumentTypeStrs);
      final ExpressionResult processedMethodCallResult =
          this.typeReader.parseExpressionResultString(processedMethodCallResultStr);
      callResult = new ExpressionResult(processedMethodCallResult, callResult, unsetType);

      // Handle iter result.
      final ExpressionResultString methodIterResultStr = method.getLoopbodyResult();
      final ExpressionResultString processedMethodIterResultStr =
          this.processExpressionResult(
              originalCalledType, calledType, method, methodIterResultStr, argumentTypeStrs);
      final ExpressionResult processedMethodIterResult =
          this.typeReader.parseExpressionResultString(processedMethodIterResultStr);
      iterResult = new ExpressionResult(processedMethodIterResult, iterResult, unsetType);
    }

    // If nothing, then undefined.
    if (callResult == null) {
      callResult = ExpressionResult.UNDEFINED;
    }
    if (iterResult == null) {
      iterResult = ExpressionResult.UNDEFINED;
    }

    // Store it!
    this.state.setNodeType(node, callResult);
    this.state.setNodeIterType(node, iterResult);
  }

  private ExpressionResultString substituteParameterRefs(
      final Method method,
      final List<TypeString> argumentTypes,
      final ExpressionResultString resultString) {
    final List<Parameter> parameters = method.getParameters();
    final Map<TypeString, TypeString> paramRefArgTypeRefMap =
        IntStream.range(0, parameters.size())
            .mapToObj(
                i -> {
                  final Parameter param = method.getParameters().get(i);
                  final String paramName = param.getName();
                  final TypeString paramRef = TypeString.ofParameterRef(paramName);
                  final TypeString argTypeRef =
                      i < argumentTypes.size() ? argumentTypes.get(i) : TypeString.SW_UNSET;
                  return new AbstractMap.SimpleEntry<>(paramRef, argTypeRef);
                })
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    ExpressionResultString newResultString = resultString;
    for (final Map.Entry<TypeString, TypeString> entry : paramRefArgTypeRefMap.entrySet()) {
      final TypeString paramRef = entry.getKey();
      final TypeString argTypeRef = entry.getValue();
      newResultString = newResultString.substituteType(paramRef, argTypeRef);
    }
    return newResultString;
  }

  private ExpressionResultString processExpressionResult(
      final AbstractType originalCalledType,
      final AbstractType calledType,
      final Method method,
      final ExpressionResultString expressionResultString,
      final List<TypeString> argumentTypeStrs) {
    ExpressionResultString newExpressionResultString = expressionResultString;

    // Substitute generics.
    final GenericHelper genericHelper = new GenericHelper(this.typeKeeper, calledType);
    newExpressionResultString = genericHelper.substituteGenerics(newExpressionResultString);

    // Substitute self.
    final TypeString calledTypeStr = calledType.getTypeString();
    newExpressionResultString =
        originalCalledType != SelfType.INSTANCE
            ? newExpressionResultString.substituteType(TypeString.SELF, calledTypeStr)
            : newExpressionResultString;

    // Substitute parameters.
    newExpressionResultString =
        this.substituteParameterRefs(method, argumentTypeStrs, newExpressionResultString);

    return newExpressionResultString;
  }
}
