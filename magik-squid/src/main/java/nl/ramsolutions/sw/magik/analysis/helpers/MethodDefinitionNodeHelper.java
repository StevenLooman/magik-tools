package nl.ramsolutions.sw.magik.analysis.helpers;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.api.MagikKeyword;
import nl.ramsolutions.sw.magik.api.MagikOperator;
import nl.ramsolutions.sw.magik.api.MagikPunctuator;

/** Helper for METHOD_DEFINITION nodes. */
public class MethodDefinitionNodeHelper {

  private static final Set<String> ABSTRACT_CONDITIONS = Set.of("subclass_should_implement");
  private static final String CONDITION = "condition";
  private static final String SW_CONDITION = "sw:condition";
  private static final String RAISE_CALL = "raise()";
  private static final String NAME_KEY = ":name";

  private final AstNode node;

  /**
   * Constructor.
   *
   * @param node Node to encapsulate.
   */
  public MethodDefinitionNodeHelper(final AstNode node) {
    if (!node.is(MagikGrammar.METHOD_DEFINITION)) {
      throw new IllegalArgumentException();
    }

    this.node = node;
  }

  /**
   * Get name of method.
   *
   * @return Method name.
   */
  public String getMethodName() {
    final AstNode parametersNode = this.node.getFirstChild(MagikGrammar.PARAMETERS);
    final List<AstNode> parameterNodes =
        parametersNode != null
            ? parametersNode.getChildren(MagikGrammar.PARAMETER)
            : Collections.emptyList();

    final AstNode methodNameNode = this.node.getFirstChild(MagikGrammar.METHOD_NAME);
    final StringBuilder builder = new StringBuilder();
    if (methodNameNode != null) {
      final String tokenValue = methodNameNode.getTokenValue();
      builder.append(tokenValue);
    }
    if (parametersNode != null) {
      if (AstQuery.anyChildTokenIs(parametersNode, MagikPunctuator.SQUARE_L)) {
        builder.append("[");
        final int commaCount = parameterNodes.size() - 1;
        final String repeatedCommas = ",".repeat(commaCount);
        builder.append(repeatedCommas);
        builder.append("]");
      }
      if (AstQuery.anyChildTokenIs(parametersNode, MagikPunctuator.PAREN_L)) {
        builder.append("()");
      }
    }
    if (AstQuery.anyChildTokenIs(this.node, MagikOperator.CHEVRON)) {
      builder.append(MagikOperator.CHEVRON.getValue());
    }
    if (AstQuery.anyChildTokenIs(this.node, MagikOperator.BOOT_CHEVRON)) {
      builder.append(MagikOperator.BOOT_CHEVRON.getValue());
    }

    return builder.toString();
  }

  /**
   * Get the method name, without the parentheses and assignment operator.
   *
   * @return
   */
  public String getMethodNameIdentifier() {
    final AstNode methodNameNode = this.node.getFirstChild(MagikGrammar.METHOD_NAME);
    return methodNameNode.getTokenValue();
  }

  /**
   * Get exemplar + method name.
   *
   * @return Exemplar + method name.
   */
  public String getExemplarMethodName() {
    final TypeString exemplarName = this.getExemplarTypeString();
    final String methodName = this.getMethodName();
    if (methodName.startsWith("[")) {
      return exemplarName.getIdentifier() + methodName;
    }

    return exemplarName.getIdentifier() + "." + methodName;
  }

  /**
   * Get {@link TypeString} of the exemplar the method is defined on.
   *
   * @return {@link TypeString} to exemplar.
   */
  public TypeString getExemplarTypeString() {
    final AstNode exemplarNameNode = this.node.getFirstChild(MagikGrammar.EXEMPLAR_NAME);
    if (exemplarNameNode == null) {
      // Handle malformed method definitions without an exemplar name
      return TypeString.UNDEFINED;
    }

    final PackageNodeHelper packageHelper = new PackageNodeHelper(this.node);
    final String pakkage = packageHelper.getCurrentPackage();
    final String exemplarName = exemplarNameNode.getTokenValue();
    return TypeString.ofIdentifier(exemplarName, pakkage);
  }

  /**
   * Get package + exemplar + method name.
   *
   * @return Package + exemplar + method name.
   */
  public String getFullExemplarMethodName() {
    final PackageNodeHelper packageHelper = new PackageNodeHelper(this.node);
    final String pakkageName = packageHelper.getCurrentPackage();
    return pakkageName + ":" + this.getExemplarMethodName();
  }

  /**
   * Get parameters + nodes.
   *
   * @return Map with parameters + PARAMETER nodes.
   */
  public Map<String, AstNode> getParameterNodes() {
    return Stream.concat(
            this.node.getChildren(MagikGrammar.PARAMETERS).stream()
                .flatMap(
                    parametersNode -> parametersNode.getChildren(MagikGrammar.PARAMETER).stream()),
            this.node.getChildren(MagikGrammar.ASSIGNMENT_PARAMETER).stream())
        .filter(parameterNode -> parameterNode.getFirstDescendant(MagikGrammar.IDENTIFIER) != null)
        .collect(
            Collectors.toMap(
                parameterNode ->
                    parameterNode.getFirstDescendant(MagikGrammar.IDENTIFIER).getTokenValue(),
                parameterNode -> parameterNode,
                (a, b) -> a));
  }

  private Collection<AstNode> getMethodModifiers() {
    final AstNode modifiersNode = this.node.getFirstChild(MagikGrammar.METHOD_MODIFIERS);
    if (modifiersNode == null) {
      return Collections.emptySet();
    }

    return modifiersNode.getChildren();
  }

  /**
   * Test if method is abstract: declared `_abstract`, or abstract by convention (see {@link
   * #isAbstractByConvention()}).
   *
   * @return True if method is abstract.
   */
  public boolean isAbstractMethod() {
    return this.hasAbstractModifier() || this.isAbstractByConvention();
  }

  private boolean hasAbstractModifier() {
    final String modifier = MagikKeyword.ABSTRACT.getValue();
    return this.getMethodModifiers().stream()
        .anyMatch(modifierNode -> modifierNode.getTokenValue().equalsIgnoreCase(modifier));
  }

  /**
   * Test if method is abstract by convention: its body only raises {@code
   * :subclass_should_implement}.
   *
   * @return True if method is abstract by convention.
   */
  public boolean isAbstractByConvention() {
    return this.raisesOnly(MethodDefinitionNodeHelper.ABSTRACT_CONDITIONS);
  }

  /**
   * Test if the body of the method is exactly {@code condition.raise(:<name>)}, for one of the
   * given condition names, optionally followed by a bare {@code _return} or {@code _return _unset},
   * or is exactly {@code _return condition.raise(:<name>)}.
   *
   * @param conditionNames Names of the raised conditions to recognise, without the colon.
   * @return True if the body only raises one of the conditions.
   */
  public boolean raisesOnly(final Set<String> conditionNames) {
    final AstNode bodyNode = this.node.getFirstChild(MagikGrammar.BODY);
    if (bodyNode == null) {
      return false;
    }

    final List<AstNode> statementNodes = bodyNode.getChildren(MagikGrammar.STATEMENT);
    final int statementCount = statementNodes.size();
    if (statementCount == 0 || statementCount > 2) {
      return false;
    }

    final AstNode firstStatementNode = statementNodes.get(0);
    if (statementCount == 1
        && MethodDefinitionNodeHelper.isReturnOfRaise(firstStatementNode, conditionNames)) {
      return true;
    }

    final AstNode expressionNode =
        AstQuery.getOnlyFromChain(
            firstStatementNode, MagikGrammar.EXPRESSION_STATEMENT, MagikGrammar.EXPRESSION);
    if (expressionNode == null
        || !MethodDefinitionNodeHelper.isRaiseOf(expressionNode, conditionNames)) {
      return false;
    }

    if (statementCount == 1) {
      return true;
    }

    final AstNode secondStatementNode = statementNodes.get(1);
    return MethodDefinitionNodeHelper.isReturnNothing(secondStatementNode);
  }

  private static boolean isReturnOfRaise(
      final AstNode statementNode, final Set<String> conditionNames) {
    final AstNode tupleNode =
        AstQuery.getFirstChildFromChain(
            statementNode, MagikGrammar.RETURN_STATEMENT, MagikGrammar.TUPLE);
    if (tupleNode == null) {
      return false;
    }

    final AstNode expressionNode = AstQuery.getOnlyFromChain(tupleNode, MagikGrammar.EXPRESSION);
    return expressionNode != null
        && MethodDefinitionNodeHelper.isRaiseOf(expressionNode, conditionNames);
  }

  private static boolean isRaiseOf(final AstNode expressionNode, final Set<String> conditionNames) {
    final AstNode postfixExpressionNode =
        AstQuery.getOnlyFromChain(expressionNode, MagikGrammar.POSTFIX_EXPRESSION);
    if (postfixExpressionNode == null) {
      return false;
    }

    final List<AstNode> invocationNodes =
        postfixExpressionNode.getChildren(MagikGrammar.METHOD_INVOCATION);
    if (invocationNodes.size() != 1) {
      return false;
    }

    final AstNode invocationNode = invocationNodes.get(0);
    final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(invocationNode);
    if (!helper.isMethodInvocationOf(CONDITION, RAISE_CALL)
        && !helper.isMethodInvocationOf(SW_CONDITION, RAISE_CALL)) {
      return false;
    }

    final AstNode argumentsNode = invocationNode.getFirstChild(MagikGrammar.ARGUMENTS);
    final ArgumentsNodeHelper argumentsHelper = new ArgumentsNodeHelper(argumentsNode);
    final AstNode symbolNode = argumentsHelper.getArgument(0, MagikGrammar.SYMBOL);
    if (symbolNode == null) {
      return false;
    }

    final String symbol = symbolNode.getTokenValue();
    final String conditionName = symbol.substring(1);
    return conditionNames.contains(conditionName)
        && !MethodDefinitionNodeHelper.namesCaller(argumentsNode);
  }

  // A helper raising on behalf of its caller passes the caller's name in: `:name, a_variable`.
  private static boolean namesCaller(final AstNode argumentsNode) {
    final List<AstNode> argumentNodes = argumentsNode.getChildren(MagikGrammar.ARGUMENT);
    for (int index = 0; index < argumentNodes.size() - 1; ++index) {
      final AstNode argumentNode = argumentNodes.get(index);
      final AstNode symbolNode =
          AstQuery.getOnlyFromChain(
              argumentNode, MagikGrammar.EXPRESSION, MagikGrammar.ATOM, MagikGrammar.SYMBOL);
      final AstNode valueNode = argumentNodes.get(index + 1);
      final AstNode identifierNode =
          AstQuery.getOnlyFromChain(
              valueNode, MagikGrammar.EXPRESSION, MagikGrammar.ATOM, MagikGrammar.IDENTIFIER);
      final String key = symbolNode != null ? symbolNode.getTokenValue() : null;
      if (MethodDefinitionNodeHelper.NAME_KEY.equals(key) && identifierNode != null) {
        return true;
      }
    }
    return false;
  }

  // The raise never returns, so a trailing `_return` / `_return _unset` is dead code.
  private static boolean isReturnNothing(final AstNode statementNode) {
    final AstNode returnNode = statementNode.getFirstChild(MagikGrammar.RETURN_STATEMENT);
    if (returnNode == null) {
      return false;
    }

    final AstNode tupleNode = returnNode.getFirstChild(MagikGrammar.TUPLE);
    if (tupleNode == null) {
      return true;
    }

    final String tupleValue = tupleNode.getTokenValue();
    final List<Token> tupleTokens = tupleNode.getTokens();
    final String unset = MagikKeyword.UNSET.getValue();
    return tupleTokens.size() == 1 && unset.equalsIgnoreCase(tupleValue);
  }

  /**
   * Test if method is a `_private` method.
   *
   * @return
   */
  public boolean isPrivateMethod() {
    final String modifier = MagikKeyword.PRIVATE.getValue();
    return this.getMethodModifiers().stream()
        .anyMatch(modifierNode -> modifierNode.getTokenValue().equalsIgnoreCase(modifier));
  }

  /**
   * Test if method is an `_iter` method.
   *
   * @return
   */
  public boolean isIterMethod() {
    final String modifier = MagikKeyword.ITER.getValue();
    return this.getMethodModifiers().stream()
        .anyMatch(modifierNode -> modifierNode.getTokenValue().equalsIgnoreCase(modifier));
  }

  /**
   * Test if method returns anything.
   *
   * @return
   */
  public boolean returnsAnything() {
    final List<AstNode> returnStatementNodes =
        this.node.getDescendants(MagikGrammar.RETURN_STATEMENT);
    final boolean hasReturn =
        returnStatementNodes.stream()
            .filter(
                statementNode ->
                    statementNode.getFirstAncestor(MagikGrammar.PROCEDURE_DEFINITION) == null)
            .anyMatch(statementNode -> statementNode.hasDescendant(MagikGrammar.TUPLE));

    final boolean hasEmit =
        this.node.getFirstChild(MagikGrammar.BODY).getChildren(MagikGrammar.STATEMENT).stream()
            .anyMatch(
                statementNode -> !statementNode.getChildren(MagikGrammar.EMIT_STATEMENT).isEmpty());

    return hasReturn || hasEmit;
  }

  /**
   * Test if method has a loopbody statement.
   *
   * @return
   */
  public boolean hasLoopbody() {
    return this.node.getDescendants(MagikGrammar.LOOPBODY).stream()
        .anyMatch(
            statementNode ->
                statementNode.getFirstAncestor(MagikGrammar.PROCEDURE_DEFINITION) == null);
  }

  /**
   * Get the node which identifies the name of the method. This is either the {@link
   * MagikGrammar.METHOD_NAME} node, or the {@link MagikGrammar.PARAMETERS_SQUARE} node.
   *
   * @return Node which identifies the method.
   */
  public AstNode getMethodNameNode() {
    final AstNode methodNameNode = this.node.getFirstChild(MagikGrammar.METHOD_NAME);
    if (methodNameNode == null) {
      return this.node.getChildren().stream()
          .filter(childNode -> childNode.isNot(MagikGrammar.values()))
          .findFirst()
          .orElseThrow();
    }

    return methodNameNode;
  }
}
