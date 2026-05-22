package nl.ramsolutions.sw.magik.analysis.typing.reasoner;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ParameterDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ProcedureDefinition;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.ProcedureInvocationDefinitionHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.ProcedureInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.GenericHelper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

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
            .substituteType(TypeString.PRIVATE, methodOwnerTypeStr)
            .get(0, TypeString.SW_UNSET);

    // Store the method definition(s) on the node.
    final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(node);
    final String methodName = helper.getMethodName();
    final Collection<MethodDefinition> methodDefs =
        calledTypeStr.getCombinedTypes().stream()
            .map(typeStr -> this.typeResolver.getRespondingMethodDefinitions(typeStr, methodName))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());

    // Perform method call and store iterator result(s).
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
      // For assignment-methods (`[]<<` / `foo<<`), capture the RHS type so a
      // `_parameter(<param>)` ref in the declared return/loop types can be substituted.
      final TypeString assignmentArgType = this.extractAssignmentArgType(node);
      for (final MethodDefinition methodDef : methodDefs) {
        final ParameterDefinition assignmentParamDef =
            assignmentArgType != null ? methodDef.getAssignmentParameter() : null;
        // Handle call result.
        final ExpressionResultString callResultStr = methodDef.getReturnTypes();
        final ExpressionResultString processedCallResultStr =
            this.processExpressionResultString(
                originalCalledTypeStr,
                calledTypeStr,
                methodDef.getParameters(),
                assignmentParamDef,
                callResultStr,
                argumentTypeStrs,
                assignmentParamDef != null ? assignmentArgType : null);
        callResult = new ExpressionResultString(processedCallResultStr, callResult);

        // Handle iter result.
        final ExpressionResultString iterResultStr = methodDef.getLoopTypes();
        final ExpressionResultString processedIterResultStr =
            this.processExpressionResultString(
                originalCalledTypeStr,
                calledTypeStr,
                methodDef.getParameters(),
                assignmentParamDef,
                iterResultStr,
                argumentTypeStrs,
                assignmentParamDef != null ? assignmentArgType : null);
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
   * Extract the RHS expression type for an assignment-method invocation, or {@code null} if the
   * node is not an assignment-method invocation.
   */
  @Nullable
  private TypeString extractAssignmentArgType(final AstNode node) {
    final AstNode assignmentArgumentNode = node.getFirstChild(MagikGrammar.ASSIGNMENT_ARGUMENT);
    if (assignmentArgumentNode == null) {
      return null;
    }
    final AstNode argNode = assignmentArgumentNode.getFirstChild(MagikGrammar.ARGUMENT);
    final AstNode exprNode =
        argNode != null ? argNode.getFirstChild(MagikGrammar.EXPRESSION) : null;
    return exprNode != null
        ? this.state.getNodeType(exprNode).get(0, TypeString.SW_UNSET)
        : TypeString.SW_UNSET;
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
        ProcedureInvocationDefinitionHelper.getProcedureTypeInvokedOn(
            node, this.state, this.typeResolver);
    final Collection<ProcedureDefinition> procedureDefs =
        ProcedureInvocationDefinitionHelper.getRespondingProcedureDefinitions(
            node, this.state, this.typeResolver, this.state.getMagikFile());

    // Perform procedure call.
    ExpressionResultString callResult = null;
    ExpressionResultString iterResult = null;
    for (final ProcedureDefinition procDef : procedureDefs) {

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
              null,
              callResultStr,
              argumentTypeStrs,
              null);
      callResult = new ExpressionResultString(processedCallResultStr, callResult);

      // Handle iter result.
      final ExpressionResultString iterResultStr = procDef.getLoopTypes();
      final ExpressionResultString processedIterResultStr =
          this.processExpressionResultString(
              originalCalledTypeStr,
              calledTypeStr,
              procDef.getParameters(),
              null,
              iterResultStr,
              argumentTypeStrs,
              null);
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

  /**
   * Substitute parameter references. {@code assignmentParamDef} and {@code assignmentArgType} are
   * always both {@code null} or both non-{@code null}.
   */
  private ExpressionResultString substituteParameterRefs(
      final List<ParameterDefinition> paramDefs,
      final @Nullable ParameterDefinition assignmentParamDef,
      final List<TypeString> argumentTypes,
      final @Nullable TypeString assignmentArgType,
      final ExpressionResultString resultString) {
    final Map<TypeString, TypeString> paramRefArgTypeRefMap =
        new HashMap<>(
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
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

    if (assignmentParamDef != null) {
      final TypeString assignParamRef = TypeString.ofParameterRef(assignmentParamDef.getName());
      paramRefArgTypeRefMap.put(assignParamRef, assignmentArgType);
    }

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
      final @Nullable ParameterDefinition assignmentParamDef,
      final ExpressionResultString expressionResultString,
      final List<TypeString> argumentTypeStrs,
      final @Nullable TypeString assignmentArgType) {
    ExpressionResultString newExpressionResultString = expressionResultString;

    // Substitute generics.
    final GenericHelper genericHelper = new GenericHelper(calledTypeStr);
    newExpressionResultString = genericHelper.substituteGenerics(newExpressionResultString);

    // Substitute self.
    newExpressionResultString =
        originalCalledTypeStr != TypeString.SELF
            ? newExpressionResultString
                .substituteType(TypeString.SELF, calledTypeStr)
                .substituteType(TypeString.PRIVATE, calledTypeStr)
            : newExpressionResultString;

    // Substitute parameters (including assignment parameter when present).
    newExpressionResultString =
        this.substituteParameterRefs(
            paramDefs,
            assignmentParamDef,
            argumentTypeStrs,
            assignmentArgType,
            newExpressionResultString);

    return newExpressionResultString;
  }
}
