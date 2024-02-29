package nl.ramsolutions.sw.magik.analysis.typing.reasoner;

import com.sonar.sslr.api.AstNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.helpers.ArgumentsNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.ConditionResolver;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.restrictions.RestrictingConditionWalker;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.restrictions.TypeRestriction;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResult;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/**
 * Conditional body handler.
 *
 * <p>Sets tested types for variables in the body after a conditional expression, such as an: -
 * `_is`/`_isnt` `_unset`/`_maybe` (singleton types) - `_is`/`_isnt` <value> -
 * `kind_of?()`/`class_of?()` - `_not` <above>
 *
 * <p>Note that combinations (`_and`/`_or` etc) aren't supported, as reasoning/determining about the
 * types of variables in the body after a conditional expression with combinations of these is often
 * impossible. I.e., the condition expression `a.is_kind_of?(sw:integer) _andif a > 0` cannot be
 * determined, as `a` might be an `sw:integer`, just not positive.
 */
class ConditionalBodyHandler extends LocalTypeReasonerHandler {

  private static final String CONDITION = "condition";
  private static final String SW_CONDITION = "sw:condition";
  private static final String RAISE_CALL = "raise()";

  private final Map<AstNode, Set<TypeRestriction>> nodeRestrictions = new HashMap<>();
  private final Map<AstNode, Set<TypeRestriction>> returningNodeRestrictions = new HashMap<>();

  /**
   * Constructor.
   *
   * @param state Reasoner state.
   */
  ConditionalBodyHandler(final LocalTypeReasonerState state) {
    super(state);
  }

  /**
   * Handle a conditional expression.
   *
   * @param node CONDITIONAL_EXPRESSION node.
   */
  @SuppressWarnings("checkstyle:NestedIfDepth")
  void handleConditionalExpression(final AstNode node) {
    final AstNode parentNode = node.getParent();
    final AstNode bodyNode;
    if (parentNode.is(MagikGrammar.IF, MagikGrammar.ELIF)) {
      bodyNode = node.getNextSibling().getNextSibling();
    } else if (parentNode.is(MagikGrammar.WHILE)) {
      bodyNode = node.getNextSibling().getFirstChild(MagikGrammar.BODY);
    } else {
      throw new IllegalStateException("Expected parent node, got: " + parentNode);
    }

    // Handle conditional node.
    final Set<TypeRestriction> restrictions = this.handleConditionalBody(node, bodyNode);

    // See if there is a "terminating" statement, such as as `_return`, or raising an error
    // condition,
    // and save those.
    if (this.bodyContainsTerminatingStatement(bodyNode)) {
      this.returningNodeRestrictions.put(node, restrictions);
    }

    // Handle the else node, if available.
    final AstNode ifNode = parentNode.is(MagikGrammar.IF) ? parentNode : parentNode.getParent();
    final AstNode elseNode = ifNode.getFirstChild(MagikGrammar.ELSE);
    final AstNode lastElifNode = ifNode.getLastChild(MagikGrammar.ELIF);
    if (elseNode != null && (lastElifNode == null || lastElifNode == parentNode)) {
      this.handleElseNode(elseNode);
    }

    // Handle the upper body.
    if (lastElifNode == null || lastElifNode == parentNode) {
      // Test if else node contains a return/raise statement.
      if (elseNode != null) {
        final AstNode elseBodyNode = elseNode.getFirstChild(MagikGrammar.BODY);
        if (this.bodyContainsTerminatingStatement(elseBodyNode)) {
          // Find all previous restrictions from previous nodes, invert these, and store for the
          // else node.
          final List<AstNode> conditionalExpressionNodes =
              Stream.concat(
                      Stream.of(ifNode.getFirstChild(MagikGrammar.CONDITIONAL_EXPRESSION)),
                      ifNode.getChildren(MagikGrammar.ELIF).stream()
                          .map(
                              elifNode ->
                                  elifNode.getFirstChild(MagikGrammar.CONDITIONAL_EXPRESSION)))
                  .toList();
          final Set<TypeRestriction> allInvertedRestrictions =
              conditionalExpressionNodes.stream()
                  .map(this.nodeRestrictions::get)
                  .flatMap(Set::stream)
                  .map(TypeRestriction::not)
                  .collect(Collectors.toSet());

          this.returningNodeRestrictions.put(elseNode, allInvertedRestrictions);
        }
      }

      this.handleUpperBody(ifNode);
    }
  }

  private void handleUpperBody(final AstNode ifNode) {
    // Apply negated restrictions to upper body, from the if-node on. I.e., if there is a check if a
    // variable
    // is unset, and the body of the if statement does a `_return`, then the variable in current
    // body cannot
    // be `_unset`.
    final AstNode ifStatementNode = ifNode.getFirstAncestor(MagikGrammar.STATEMENT);
    final AstNode nextSiblingNode = ifStatementNode.getNextSibling();
    if (nextSiblingNode == null) {
      // No next sibling node, nothing to do.
      return;
    }

    final AstNode upperBodyNode = ifStatementNode.getFirstAncestor(MagikGrammar.BODY);
    final AstNode elseNode = ifNode.getFirstChild(MagikGrammar.ELSE);
    final List<AstNode> conditionalExpressionNodes =
        Stream.concat(
                Stream.ofNullable(elseNode),
                Stream.concat(
                    Stream.of(ifNode.getFirstChild(MagikGrammar.CONDITIONAL_EXPRESSION)),
                    ifNode.getChildren(MagikGrammar.ELIF).stream()
                        .map(
                            elifNode ->
                                elifNode.getFirstChild(MagikGrammar.CONDITIONAL_EXPRESSION))))
            .toList();
    this.returningNodeRestrictions.entrySet().stream()
        .filter(entry -> conditionalExpressionNodes.contains(entry.getKey()))
        .map(Map.Entry::getValue)
        .flatMap(Set::stream)
        .map(TypeRestriction::not)
        .map(TypeRestriction::getRestriction)
        .forEach(
            entry -> {
              final ScopeEntry scopeEntry = entry.getKey();
              final List<AstNode> usages =
                  this.getUsageInBody(scopeEntry, upperBodyNode).stream()
                      .filter(n -> n.getFromIndex() >= nextSiblingNode.getFromIndex())
                      .toList();
              final AbstractType restriction = entry.getValue();
              this.setNodeTypes(usages, restriction);
            });
  }

  private Set<TypeRestriction> handleConditionalBody(final AstNode node, final AstNode bodyNode) {
    // Apply all restrictions to nodes in body.
    final Set<TypeRestriction> restrictions = this.getTypeRestriction(node);
    restrictions.stream()
        .map(TypeRestriction::getRestriction)
        .forEach(
            entry -> {
              final ScopeEntry scopeEntry = entry.getKey();
              final List<AstNode> usages = this.getUsageInBody(scopeEntry, bodyNode);
              final AbstractType restriction = entry.getValue();
              this.setNodeTypes(usages, restriction);
            });

    this.nodeRestrictions.put(node, restrictions);
    return restrictions;
  }

  private boolean bodyContainsTerminatingStatement(final AstNode bodyNode) {
    final boolean doesReturn =
        bodyNode.getChildren(MagikGrammar.STATEMENT).stream()
            .map(AstNode::getFirstChild)
            .anyMatch(node -> node.is(MagikGrammar.RETURN_STATEMENT));
    final boolean raisesError =
        bodyNode.getChildren(MagikGrammar.STATEMENT).stream()
            .map(statementNode -> statementNode.getFirstChild(MagikGrammar.EXPRESSION_STATEMENT))
            .filter(Objects::nonNull)
            .map(expressionNode -> expressionNode.getFirstChild(MagikGrammar.EXPRESSION))
            .flatMap(
                expressionNode ->
                    expressionNode.getChildren(MagikGrammar.POSTFIX_EXPRESSION).stream())
            .anyMatch(
                node -> {
                  final AstNode invocationNode = node.getLastChild(MagikGrammar.METHOD_INVOCATION);
                  if (invocationNode == null) {
                    return false;
                  }

                  final MethodInvocationNodeHelper helper =
                      new MethodInvocationNodeHelper(invocationNode);
                  if (!helper.isMethodInvocationOf(CONDITION, RAISE_CALL)
                      && !helper.isMethodInvocationOf(SW_CONDITION, RAISE_CALL)) {
                    return false;
                  }

                  final AstNode argumentsNode =
                      invocationNode.getFirstChild(MagikGrammar.ARGUMENTS);
                  final ArgumentsNodeHelper argumentsHelper =
                      new ArgumentsNodeHelper(argumentsNode);
                  final AstNode argumentNode = argumentsHelper.getArgument(0, MagikGrammar.SYMBOL);
                  if (argumentNode == null) {
                    return false;
                  }

                  final String conditionName = argumentNode.getTokenValue().substring(1);
                  final IDefinitionKeeper definitionKeeper =
                      this.state.getMagikFile().getDefinitionKeeper();
                  final ConditionResolver conditionResolver =
                      new ConditionResolver(definitionKeeper);
                  return conditionResolver.conditionHasAncestor(conditionName, "error");
                });
    return doesReturn || raisesError;
  }

  /**
   * Handle else node.
   *
   * @param node ELSE node.
   */
  private void handleElseNode(final AstNode node) {
    // Find all previous restrictions from previous nodes.
    final AstNode ifNode = node.getParent();
    final List<AstNode> conditionalExpressionNodes =
        Stream.concat(
                Stream.of(ifNode.getFirstChild(MagikGrammar.CONDITIONAL_EXPRESSION)),
                ifNode.getChildren(MagikGrammar.ELIF).stream()
                    .map(elifNode -> elifNode.getFirstChild(MagikGrammar.CONDITIONAL_EXPRESSION)))
            .toList();
    final Set<TypeRestriction> allRestrictions =
        conditionalExpressionNodes.stream()
            .map(this.nodeRestrictions::get)
            .flatMap(Set::stream)
            .collect(Collectors.toSet());

    // Combine and invert all previous restrictions.
    final Map<ScopeEntry, AbstractType> restrictions =
        this.invertAndCombineRestrictions(allRestrictions);

    // Apply to body.
    final AstNode bodyNode = node.getFirstChild(MagikGrammar.BODY);
    restrictions
        .entrySet()
        .forEach(
            entry -> {
              final ScopeEntry scopeEntry = entry.getKey();
              final List<AstNode> usages = this.getUsageInBody(scopeEntry, bodyNode);
              final AbstractType restriction = entry.getValue();
              this.setNodeTypes(usages, restriction);
            });
  }

  /**
   * Get the top {@link TypeRestriction} for a conditional expression.
   *
   * @param conditionNode CONDITIONAL_EXPRESSION node.
   * @return Top {@link TypeRestriction}.
   */
  private Set<TypeRestriction> getTypeRestriction(final AstNode conditionNode) {
    final GlobalScope globalScope = this.state.getMagikFile().getGlobalScope();
    final RestrictingConditionWalker walker =
        new RestrictingConditionWalker(this.state, globalScope);
    walker.walkAst(conditionNode);
    return walker.getTypeRestriction();
  }

  private Map<ScopeEntry, AbstractType> invertAndCombineRestrictions(
      final Set<TypeRestriction> allRestrictions) {
    // Invert restrictions and combine grouped by scope entry.
    // Intersect all restrictions for each scope entry.
    return allRestrictions.stream()
        .map(TypeRestriction::not)
        .map(TypeRestriction::getRestriction)
        .collect(
            Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, AbstractType::intersection));
  }

  private List<AstNode> getUsageInBody(final ScopeEntry scopeEntry, final AstNode bodyNode) {
    final List<AstNode> upToAssignmentNodes = new ArrayList<>();
    final List<AstNode> atomNodesInBodyNode = bodyNode.getDescendants(MagikGrammar.ATOM);
    for (final AstNode usageNode : scopeEntry.getUsages()) { // NOSONAR
      if (!atomNodesInBodyNode.contains(usageNode)) {
        continue;
      }

      final AstNode parentUsageNode = usageNode.getParent();
      if (parentUsageNode.is(MagikGrammar.ASSIGNMENT_EXPRESSION)) {
        final AstNode lastChildNode = parentUsageNode.getLastChild();
        if (usageNode != lastChildNode) {
          // Isn't right part of assignment, so it is assigned to.
          // Cannot reason further from here.
          break;
        }
      }

      upToAssignmentNodes.add(usageNode);
    }

    return upToAssignmentNodes;
  }

  private void setNodeTypes(final List<AstNode> usageNodes, final AbstractType restrictedType) {
    usageNodes.forEach(
        usageNode -> {
          final ExpressionResult restrictedResult = new ExpressionResult(restrictedType);
          final AstNode assignNode = usageNode.getFirstChild();
          this.assignAtom(assignNode, restrictedResult);
        });
  }
}
