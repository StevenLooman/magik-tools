package nl.ramsolutions.sw.magik.analysis.typing.reasoner;

import com.sonar.sslr.api.AstNode;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import nl.ramsolutions.sw.magik.analysis.definitions.ITypeStringDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ParameterDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ProcedureDefinition;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.ProcedureInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.GenericHelper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;

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
        final ExpressionResultString callResultStr = methodDef.getReturnTypes();
        final ExpressionResultString processedCallResultStr =
            this.processExpressionResultString(
                originalCalledTypeStr,
                calledTypeStr,
                methodDef.getParameters(),
                callResultStr,
                argumentTypeStrs);
        callResult = new ExpressionResultString(processedCallResultStr, callResult);

        // Handle iter result.
        final ExpressionResultString iterResultStr = methodDef.getLoopTypes();
        final ExpressionResultString processedIterResultStr =
            this.processExpressionResultString(
                originalCalledTypeStr,
                calledTypeStr,
                methodDef.getParameters(),
                iterResultStr,
                argumentTypeStrs);
        iterResult = new ExpressionResultString(processedIterResultStr, iterResult);
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
                .map(ITypeStringDefinition::getTypeString)
                .findAny()
                .orElse(TypeString.UNDEFINED);
    final Collection<ITypeStringDefinition> typeDefs = this.typeResolver.resolve(calledTypeStr);

    // Perform procedure call.
    ExpressionResultString callResult = null;
    ExpressionResultString iterResult = null;
    for (final ITypeStringDefinition typeDef : typeDefs) {
      if (!ProcedureDefinition.class.isInstance(typeDef)) {
        continue;
      }

      final ProcedureDefinition procDef = (ProcedureDefinition) typeDef;

      // Figure argument types.
      final ProcedureInvocationNodeHelper helper = new ProcedureInvocationNodeHelper(node);
      final List<AstNode> argumentExpressionNodes = helper.getArgumentExpressionNodes();
      final List<TypeString> argumentTypeStrs =
          argumentExpressionNodes.stream()
              .map(exprNode -> this.state.getNodeType(exprNode).get(0, TypeString.SW_UNSET))
              .toList();

      // Handle call result.
      final ExpressionResultString callResultStr = procDef.getReturnTypes();
      final ExpressionResultString processedCallResultStr =
          this.processExpressionResultString(
              originalCalledTypeStr,
              calledTypeStr,
              procDef.getParameters(),
              callResultStr,
              argumentTypeStrs);
      callResult = new ExpressionResultString(processedCallResultStr, callResult);

      // Handle iter result.
      final ExpressionResultString iterResultStr = procDef.getLoopTypes();
      final ExpressionResultString processedIterResultStr =
          this.processExpressionResultString(
              originalCalledTypeStr,
              calledTypeStr,
              procDef.getParameters(),
              iterResultStr,
              argumentTypeStrs);
      iterResult = new ExpressionResultString(processedIterResultStr, iterResult);
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
      final List<ParameterDefinition> paramDefs,
      final List<TypeString> argumentTypes,
      final ExpressionResultString resultString) {
    final Map<TypeString, TypeString> paramRefArgTypeRefMap =
        IntStream.range(0, paramDefs.size())
            .mapToObj(
                i -> {
                  final ParameterDefinition paramDef = paramDefs.get(i);
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
      final List<ParameterDefinition> paramDefs,
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
        this.substituteParameterRefs(paramDefs, argumentTypeStrs, newExpressionResultString);

    return newExpressionResultString;
  }
}
