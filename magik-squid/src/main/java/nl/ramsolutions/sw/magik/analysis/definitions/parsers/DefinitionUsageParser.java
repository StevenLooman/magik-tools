package nl.ramsolutions.sw.magik.analysis.definitions.parsers;

import com.sonar.sslr.api.AstNode;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikFile;
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
import nl.ramsolutions.sw.magik.analysis.helpers.UnaryOperatorHelper;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/** Definition usages parser for methods and procedures. */
public class DefinitionUsageParser {

  private static final String CONDITION = "condition";
  private static final String SW_CONDITION = "sw:condition";
  private static final String NEW_CALL = "new()";
  private static final String RAISE_CALL = "raise()";

  private final MagikFile magikFile;
  private final AstNode node;

  /**
   * Constructor.
   *
   * @param node Method or procedure definition node.
   */
  public DefinitionUsageParser(final MagikFile magikFile, final AstNode node) {
    if (node.isNot(MagikGrammar.METHOD_DEFINITION, MagikGrammar.PROCEDURE_DEFINITION)) {
      throw new IllegalArgumentException();
    }

    this.magikFile = magikFile;
    this.node = node;
  }

  /**
   * Get the used globals.
   *
   * @return Used globals.
   */
  public List<GlobalUsage> getUsedGlobals() {
    final GlobalScope globalScope = this.magikFile.getGlobalScope();
    final AstNode bodyNode = this.node.getFirstChild(MagikGrammar.BODY);
    final Scope bodyScope = globalScope.getScopeForNode(bodyNode);
    Objects.requireNonNull(bodyScope);

    final PackageNodeHelper packageNodeHelper = new PackageNodeHelper(this.node);
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
              // This can only get the TypeString of method invocations on globals,
              // as this doesn't do any deep reasoning.
              final TypeString ref = TypeString.UNDEFINED;
              final String methodName = helper.getMethodName();
              final URI uri = this.node.getToken().getURI();
              final Location location = new Location(uri, methodInvocationNode);
              final Location validLocation = Location.validLocation(location);
              final MethodUsage methodUsage =
                  new MethodUsage(ref, methodName, validLocation, methodInvocationNode);
              return methodUsage;
            })
        .toList();
  }

  /**
   * Get the used slots.
   *
   * @return Used slots.
   */
  public List<SlotUsage> getUsedSlots() {
    final TypeString typeName =
        this.node.is(MagikGrammar.METHOD_DEFINITION)
            ? new MethodDefinitionNodeHelper(this.node).getExemplarTypeString()
            : TypeString.UNDEFINED;
    return this.node.getDescendants(MagikGrammar.SLOT).stream()
        .filter(slotNode -> slotNode.getFirstChild(MagikGrammar.IDENTIFIER) != null)
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
    final URI uri = this.node.getToken().getURI();
    final List<AstNode> unaryExpressionNodes =
        this.node.getDescendants(MagikGrammar.UNARY_EXPRESSION);
    final List<MethodUsage> usages = new ArrayList<>();
    for (final AstNode unaryExpressionNode : unaryExpressionNodes) {
      // `_allresults` has no method equivalent; an unmapped operator is not a usage.
      final UnaryOperatorHelper helper = new UnaryOperatorHelper(unaryExpressionNode);
      final String methodName = helper.getUnaryOperatorMethod();
      if (methodName == null) {
        continue;
      }

      final Location location = new Location(uri, unaryExpressionNode);
      final Location validLocation = Location.validLocation(location);
      final MethodUsage usage =
          new MethodUsage(TypeString.UNDEFINED, methodName, validLocation, unaryExpressionNode);
      usages.add(usage);
    }

    return usages;
  }

  /**
   * Get the used binary operators.
   *
   * @return Used binary operators.
   */
  public List<BinaryOperatorUsage> getUsedBinaryOperators() {
    final URI uri = this.node.getToken().getURI();
    return Stream.concat(this.getBinaryExpressionOperators(uri), this.getAugmentedOperators(uri))
        .toList();
  }

  /** Augmented assignments (`x +<< y`) use the binary operator before the chevron. */
  private Stream<BinaryOperatorUsage> getAugmentedOperators(final URI uri) {
    return this.node.getDescendants(MagikGrammar.AUGMENTED_ASSIGNMENT_EXPRESSION).stream()
        .map(
            augmentedNode -> {
              // Children: <lhs>, OPERATOR (e.g. `+`), CHEVRON, <rhs>.
              final AstNode operatorNode = augmentedNode.getChildren().get(1);
              final String operator = operatorNode.getTokenValue().toLowerCase();
              final Location location = new Location(uri, augmentedNode);
              final Location validLocation = Location.validLocation(location);
              return new BinaryOperatorUsage(
                  TypeString.UNDEFINED,
                  TypeString.UNDEFINED,
                  operator,
                  validLocation,
                  augmentedNode);
            });
  }

  private Stream<BinaryOperatorUsage> getBinaryExpressionOperators(final URI uri) {
    return this.node
        .getDescendants(
            MagikGrammar.OR_EXPRESSION,
            MagikGrammar.XOR_EXPRESSION,
            MagikGrammar.AND_EXPRESSION,
            MagikGrammar.EQUALITY_EXPRESSION,
            MagikGrammar.RELATIONAL_EXPRESSION,
            MagikGrammar.ADDITIVE_EXPRESSION,
            MagikGrammar.MULTIPLICATIVE_EXPRESSION,
            MagikGrammar.EXPONENTIAL_EXPRESSION)
        .stream()
        .flatMap(
            binaryExpressionNode -> {
              final BinaryExpressionNodeHelper helper =
                  new BinaryExpressionNodeHelper(binaryExpressionNode);
              return helper.getTriplets().stream()
                  .map(
                      triplet -> {
                        final AstNode operatorNode = triplet.getMiddle();
                        final String operator = operatorNode.getTokenValue().toLowerCase();
                        final Location location = new Location(uri, binaryExpressionNode);
                        final Location validLocation = Location.validLocation(location);
                        return new BinaryOperatorUsage(
                            TypeString.UNDEFINED,
                            TypeString.UNDEFINED,
                            operator,
                            validLocation,
                            binaryExpressionNode);
                      });
            });
  }
}
