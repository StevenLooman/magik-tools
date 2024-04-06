package nl.ramsolutions.sw.magik.analysis.typing.reasoner;

import com.sonar.sslr.api.AstNode;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ParameterDefinition;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.ProcedureInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.GenericHelper;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
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
    // Get called type for method.
    final AstNode calledNode = node.getPreviousSibling();
    final ExpressionResultString calledResult = this.state.getNodeType(calledNode);
    final TypeString originalCalledTypeStr = calledResult.get(0, TypeString.SW_UNSET);
    final TypeString methodOwnerTypeStr = this.getMethodOwnerType(node);
    final TypeString calledTypeStr =
        calledResult
            .substituteType(TypeString.SELF, methodOwnerTypeStr)
            .get(0, TypeString.SW_UNSET);

    // Perform method call and store iterator result(s).
    final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(node);
    final String methodName = helper.getMethodName();
    final Collection<MethodDefinition> methodDefs =
        this.typeResolver.getMethodDefinitions(calledTypeStr, methodName);
    ExpressionResultString callResult = null;
    ExpressionResultString iterResult = null;
    if (methodDefs.isEmpty()) {
      // Method not found, we cannot known what the results will be.
      callResult = ExpressionResultString.UNDEFINED;
      iterResult = ExpressionResultString.UNDEFINED;
    } else {
      final List<AstNode> argumentExpressionNodes = helper.getArgumentExpressionNodes();
      final List<TypeString> argumentTypeStrs =
          argumentExpressionNodes.stream()
              .map(exprNode -> this.state.getNodeType(exprNode).get(0, TypeString.SW_UNSET))
              .toList();
      for (final MethodDefinition methodDef : methodDefs) {
        // Handle call result.
        ExpressionResultString methodCallResultStr = methodDef.getReturnTypes();
        final ExpressionResultString processedMethodCallResultStr =
            this.processExpressionResultString(
                originalCalledTypeStr,
                calledTypeStr,
                methodDef,
                methodCallResultStr,
                argumentTypeStrs);
        callResult = new ExpressionResultString(processedMethodCallResultStr, callResult);

        // Handle iter result.
        final ExpressionResultString methodIterResultStr = methodDef.getLoopTypes();
        final ExpressionResultString processedMethodIterResultStr =
            this.processExpressionResultString(
                originalCalledTypeStr,
                calledTypeStr,
                methodDef,
                methodIterResultStr,
                argumentTypeStrs);
        iterResult = new ExpressionResultString(processedMethodIterResultStr, iterResult);
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
    // TODO: Handle sw:obj/sw:prototype.

    // Get called type for invocation.
    final AstNode calledNode = node.getPreviousSibling();
    final ExpressionResultString calledNodeResult = this.state.getNodeType(calledNode);
    final TypeString originalCalledTypeStr = calledNodeResult.get(0, TypeString.SW_UNSET);
    final TypeString calledTypeStr =
        originalCalledTypeStr == TypeString.SELF
            ? TypeString.SW_PROCEDURE
            : this.typeResolver.resolve(originalCalledTypeStr).stream()
                .map(typeStringDef -> typeStringDef.getTypeString())
                .findAny()
                .orElse(TypeString.UNDEFINED);

    // Perform procedure call.
    ExpressionResultString callResult = null;
    ExpressionResultString iterResult = null;
    if (calledTypeStr.equals(TypeString.SW_PROCEDURE)) {
      // final TypeStringDefinition typeStringDef = this.state.getTypeStringDefinition(calledNode);
      // // TODO: What to do?
      final MethodDefinition invokeDef =
          this.typeResolver.getMethodDefinitions(calledTypeStr, "invoke()").stream()
              .findAny()
              .orElse(null);

      // Figure argument types.
      final ProcedureInvocationNodeHelper helper = new ProcedureInvocationNodeHelper(node);
      final List<AstNode> argumentExpressionNodes = helper.getArgumentExpressionNodes();
      final List<TypeString> argumentTypeStrs =
          argumentExpressionNodes.stream()
              .map(exprNode -> this.state.getNodeType(exprNode).get(0, TypeString.SW_UNSET))
              .toList();

      // Handle call result.
      ExpressionResultString methodCallResultStr = invokeDef.getReturnTypes();
      final ExpressionResultString processedMethodCallResultStr =
          this.processExpressionResultString(
              originalCalledTypeStr,
              calledTypeStr,
              invokeDef,
              methodCallResultStr,
              argumentTypeStrs);
      callResult = new ExpressionResultString(processedMethodCallResultStr, callResult);

      // Handle iter result.
      final ExpressionResultString methodIterResultStr = invokeDef.getLoopTypes();
      final ExpressionResultString processedMethodIterResultStr =
          this.processExpressionResultString(
              originalCalledTypeStr,
              calledTypeStr,
              invokeDef,
              methodIterResultStr,
              argumentTypeStrs);
      iterResult = new ExpressionResultString(processedMethodIterResultStr, iterResult);
    }

    // If nothing, then undefined.
    if (callResult == null) {
      callResult = ExpressionResultString.UNDEFINED;
    }
    if (iterResult == null) {
      iterResult = ExpressionResultString.UNDEFINED;
    }

    // Store it!
    this.state.setNodeType(node, callResult);
    this.state.setNodeIterType(node, iterResult);
  }

  private ExpressionResultString substituteParameterRefs(
      final MethodDefinition methodDef,
      final List<TypeString> argumentTypes,
      final ExpressionResultString resultString) {
    final List<ParameterDefinition> parameters = methodDef.getParameters();
    final Map<TypeString, TypeString> paramRefArgTypeRefMap =
        IntStream.range(0, parameters.size())
            .mapToObj(
                i -> {
                  final ParameterDefinition paramDef = methodDef.getParameters().get(i);
                  final String paramName = paramDef.getName();
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

  private ExpressionResultString processExpressionResultString(
      final TypeString originalCalledTypeStr,
      final TypeString calledTypeStr,
      final MethodDefinition methodDef,
      final ExpressionResultString expressionResultString,
      final List<TypeString> argumentTypeStrs) {
    ExpressionResultString newExpressionResultString = expressionResultString;

    // Substitute generics.
    final GenericHelper genericHelper = new GenericHelper(calledTypeStr);
    newExpressionResultString = genericHelper.substituteGenerics(newExpressionResultString);

    // Substitute self.
    newExpressionResultString =
        originalCalledTypeStr != TypeString.SELF
            ? newExpressionResultString.substituteType(TypeString.SELF, calledTypeStr)
            : newExpressionResultString;

    // Substitute parameters.
    newExpressionResultString =
        this.substituteParameterRefs(methodDef, argumentTypeStrs, newExpressionResultString);

    return newExpressionResultString;
  }
}
