package nl.ramsolutions.sw.magik.analysis.definitions.parsers;

import com.sonar.sslr.api.AstNode;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.definitions.BinaryOperatorUsage;
import nl.ramsolutions.sw.magik.analysis.definitions.ConditionUsage;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalUsage;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodUsage;
import nl.ramsolutions.sw.magik.analysis.definitions.SlotUsage;
import nl.ramsolutions.sw.magik.analysis.helpers.ArgumentsNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.BinaryExpressionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.PackageNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.UnaryExpressionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeBuilderVisitor;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.api.MagikKeyword;
import nl.ramsolutions.sw.magik.api.MagikOperator;

/** Method Definition usages parser. */
public class MethodDefinitionUsageParser {

  private static final Map<String, String> UNARY_OPERATOR_METHODS =
      Map.of(
          MagikOperator.NOT.getValue(), "not",
          MagikKeyword.NOT.getValue(), "not",
          MagikOperator.MINUS.getValue(), "negated",
          MagikOperator.PLUS.getValue(), "unary_plus",
          MagikKeyword.SCATTER.getValue(), "for_scatter()");

  private static final String CONDITION = "condition";
  private static final String SW_CONDITION = "sw:condition";
  private static final String NEW_CALL = "new()";
  private static final String RAISE_CALL = "raise()";

  private final AstNode node;

  /**
   * Constructor.
   *
   * @param node Method definition node.
   */
  public MethodDefinitionUsageParser(final AstNode node) {
    if (node.isNot(MagikGrammar.METHOD_DEFINITION)) {
      throw new IllegalArgumentException();
    }

    this.node = node;
  }

  /**
   * Get the used globals.
   *
   * @return Used globals.
   */
  public List<GlobalUsage> getUsedGlobals() {
    final ScopeBuilderVisitor scopeBuilderVisitor = new ScopeBuilderVisitor();
    scopeBuilderVisitor.createGlobalScope(this.node);
    scopeBuilderVisitor.walkAst(this.node);
    final GlobalScope globalScope = scopeBuilderVisitor.getGlobalScope();
    final AstNode bodyNode = this.node.getFirstChild(MagikGrammar.BODY);
    final Scope bodyScope = globalScope.getScopeForNode(bodyNode);
    Objects.requireNonNull(bodyScope);

    final PackageNodeHelper packageNodeHelper = new PackageNodeHelper(node);
    final String currentPakkage = packageNodeHelper.getCurrentPackage();
    return bodyScope.getSelfAndDescendantScopes().stream()
        .flatMap(scope -> scope.getScopeEntriesInScope().stream())
        .filter(scopeEntry -> scopeEntry.isType(ScopeEntry.Type.GLOBAL, ScopeEntry.Type.DYNAMIC))
        .map(
            scopeEntry -> {
              final String identifier = scopeEntry.getIdentifier();
              final TypeString ref = TypeString.ofIdentifier(identifier, currentPakkage);
              final URI uri = this.node.getToken().getURI();
              final AstNode definitionNode = scopeEntry.getDefinitionNode();
              final Location location = new Location(uri, definitionNode);
              final Location validLocation = Location.validLocation(location);
              // Now you might "see" the reference `user:char16_vector`,
              // or (from) any other package which is a child of `sw`.
              // This will most likely be indexed invalidly.
              // Though, we might be able to resolve it during the query itself.
              // TODO: Shouldn't this be multiple? I.e., one per `scopeEntry.getUsages()`.
              return new GlobalUsage(ref, validLocation, definitionNode);
            })
        .filter(Objects::nonNull)
        .toList();
  }

  /**
   * Get the used methods.
   *
   * @return Used methods.
   */
  public List<MethodUsage> getUsedMethods() {
    return this.node.getDescendants(MagikGrammar.METHOD_INVOCATION).stream()
        .map(
            methodInvocationNode -> {
              final MethodInvocationNodeHelper helper =
                  new MethodInvocationNodeHelper(methodInvocationNode);
              // TODO: This can only get the TypeString of method invocations on globals,
              // as this doesn't do any deep reasoning.
              final String methodName = helper.getMethodName();
              final URI uri = this.node.getToken().getURI();
              final Location location = new Location(uri, methodInvocationNode);
              final Location validLocation = Location.validLocation(location);
              return new MethodUsage(
                  TypeString.UNDEFINED, methodName, validLocation, methodInvocationNode);
            })
        .toList();
  }

  /**
   * Get the used slots.
   *
   * @return Used slots.
   */
  public List<SlotUsage> getUsedSlots() {
    final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(this.node);
    final TypeString typeName = helper.getExemplarTypeString();
    return this.node.getDescendants(MagikGrammar.SLOT).stream()
        .map(
            slotNode -> {
              final String slotName =
                  slotNode.getFirstChild(MagikGrammar.IDENTIFIER).getTokenValue();
              final URI uri = this.node.getToken().getURI();
              final Location location = new Location(uri, slotNode);
              final Location validLocation = Location.validLocation(location);
              return new SlotUsage(typeName, slotName, validLocation, slotNode);
            })
        .toList();
  }

  /**
   * Get the used conditions.
   *
   * @return Used conditions.
   */
  public List<ConditionUsage> getUsedConditions() {
    final URI uri = this.node.getToken().getURI();
    final Stream<ConditionUsage> handledConditions =
        this.node.getDescendants(MagikGrammar.CONDITION_NAME).stream()
            .map(
                conditionNameNode -> {
                  final String conditionName = conditionNameNode.getTokenValue();
                  final Location location = new Location(uri, conditionNameNode);
                  final Location validLocation = Location.validLocation(location);
                  return new ConditionUsage(conditionName, validLocation, conditionNameNode);
                });
    final Stream<ConditionUsage> raisedConditions =
        this.node.getDescendants(MagikGrammar.METHOD_INVOCATION).stream()
            .map(
                invocationNode -> {
                  final MethodInvocationNodeHelper helper =
                      new MethodInvocationNodeHelper(invocationNode);
                  if (!helper.isMethodInvocationOf(CONDITION, RAISE_CALL)
                      && !helper.isMethodInvocationOf(SW_CONDITION, RAISE_CALL)
                      && !helper.isMethodInvocationOf(CONDITION, NEW_CALL)
                      && !helper.isMethodInvocationOf(SW_CONDITION, NEW_CALL)) {
                    return null;
                  }

                  final AstNode argumentsNode =
                      invocationNode.getFirstChild(MagikGrammar.ARGUMENTS);
                  final ArgumentsNodeHelper argumentsHelper =
                      new ArgumentsNodeHelper(argumentsNode);
                  final AstNode argumentNode = argumentsHelper.getArgument(0, MagikGrammar.SYMBOL);
                  if (argumentNode == null) {
                    return null;
                  }

                  final String conditionName = argumentNode.getTokenValue().substring(1);
                  final Location location = new Location(uri, argumentsNode);
                  final Location validLocation = Location.validLocation(location);
                  // TODO: Set location to identifier node.
                  return new ConditionUsage(conditionName, validLocation, invocationNode);
                })
            .filter(Objects::nonNull);
    return Stream.concat(handledConditions, raisedConditions).toList();
  }

  /**
   * Get the used unary operators.
   *
   * @return Used unary operators.
   */
  public List<MethodUsage> getUsedUnaryOperators() {
    return this.node.getDescendants(MagikGrammar.UNARY_EXPRESSION).stream()
        .filter(
            unaryExpressionNode -> !unaryExpressionNode.hasDirectChildren(MagikKeyword.ALLRESULTS))
        .map(
            unaryExpressionNode -> {
              final UnaryExpressionNodeHelper helper =
                  new UnaryExpressionNodeHelper(unaryExpressionNode);
              final String operator = helper.getOperator();
              final String methodName =
                  MethodDefinitionUsageParser.UNARY_OPERATOR_METHODS.get(operator);
              final Location location = new Location(unaryExpressionNode);
              final Location validLocation = Location.validLocation(location);
              return new MethodUsage(
                  TypeString.UNDEFINED, methodName, validLocation, unaryExpressionNode);
            })
        .toList();
  }

  /**
   * Get the used binary operators.
   *
   * @return Used binary operators.
   */
  public List<BinaryOperatorUsage> getUsedBinaryOperators() {
    return this.node
        .getDescendants(
            // TODO: Combined assignments.
            MagikGrammar.OR_EXPRESSION,
            MagikGrammar.XOR_EXPRESSION,
            MagikGrammar.AND_EXPRESSION,
            MagikGrammar.EQUALITY_EXPRESSION,
            MagikGrammar.RELATIONAL_EXPRESSION,
            MagikGrammar.ADDITIVE_EXPRESSION,
            MagikGrammar.MULTIPLICATIVE_EXPRESSION,
            MagikGrammar.EXPONENTIAL_EXPRESSION)
        .stream()
        .map(
            binaryExpressionNode -> {
              final BinaryExpressionNodeHelper helper =
                  new BinaryExpressionNodeHelper(binaryExpressionNode);
              return helper.getTriplets().stream()
                  .map(
                      triplet -> {
                        final AstNode operatorNode = triplet.getMiddle();
                        final String operator = operatorNode.getTokenValue().toLowerCase();
                        final Location location = new Location(binaryExpressionNode);
                        final Location validLocation = Location.validLocation(location);
                        return new BinaryOperatorUsage(
                            TypeString.UNDEFINED,
                            TypeString.UNDEFINED,
                            operator,
                            validLocation,
                            binaryExpressionNode);
                      });
            })
        .flatMap(stream -> stream)
        .toList();
  }
}
