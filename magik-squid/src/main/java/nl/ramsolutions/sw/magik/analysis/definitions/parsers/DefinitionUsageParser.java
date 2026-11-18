package nl.ramsolutions.sw.magik.analysis.definitions.parsers;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.MagikAnalysisSettings;
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
  private static final AstNodeType[] BINARY_EXPRESSIONS = {
    MagikGrammar.OR_EXPRESSION,
    MagikGrammar.XOR_EXPRESSION,
    MagikGrammar.AND_EXPRESSION,
    MagikGrammar.EQUALITY_EXPRESSION,
    MagikGrammar.RELATIONAL_EXPRESSION,
    MagikGrammar.ADDITIVE_EXPRESSION,
    MagikGrammar.MULTIPLICATIVE_EXPRESSION,
    MagikGrammar.EXPONENTIAL_EXPRESSION,
  };

  /**
   * The usages of code that is no method or procedure.
   *
   * @param methods Methods used, unary operators included.
   * @param globals Globals used.
   * @param conditions Conditions used.
   * @param binaryOperators Binary operators used, augmented assignments included.
   */
  public record Usages(
      List<MethodUsage> methods,
      List<GlobalUsage> globals,
      List<ConditionUsage> conditions,
      List<BinaryOperatorUsage> binaryOperators) {}

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
    final URI uri = this.node.getToken().getURI();
    return this.node.getDescendants(MagikGrammar.METHOD_INVOCATION).stream()
        .map(
            methodInvocationNode ->
                DefinitionUsageParser.createMethodUsage(uri, methodInvocationNode))
        .toList();
  }

  /**
   * Create the usage of a method invocation.
   *
   * @param uri URI of the file holding the node.
   * @param methodInvocationNode {@code METHOD_INVOCATION} node.
   * @return Method usage.
   */
  private static MethodUsage createMethodUsage(final URI uri, final AstNode methodInvocationNode) {
    final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(methodInvocationNode);
    // This can only get the TypeString of method invocations on globals,
    // as this doesn't do any deep reasoning.
    final TypeString ref = TypeString.UNDEFINED;
    final String methodName = helper.getMethodName();
    final Location location = new Location(uri, methodInvocationNode);
    final Location validLocation = Location.validLocation(location);
    return new MethodUsage(ref, methodName, validLocation, methodInvocationNode);
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
                conditionNameNode ->
                    DefinitionUsageParser.createHandledConditionUsage(uri, conditionNameNode));
    final Stream<ConditionUsage> raisedConditions =
        this.node.getDescendants(MagikGrammar.METHOD_INVOCATION).stream()
            .map(
                invocationNode ->
                    DefinitionUsageParser.createRaisedConditionUsage(uri, invocationNode))
            .filter(Objects::nonNull);
    return Stream.concat(handledConditions, raisedConditions).toList();
  }

  /**
   * Create the usage of the condition a {@code _when} handles.
   *
   * @param uri URI of the file holding the node.
   * @param conditionNameNode {@code CONDITION_NAME} node.
   * @return Condition usage.
   */
  private static ConditionUsage createHandledConditionUsage(
      final URI uri, final AstNode conditionNameNode) {
    final String conditionName = conditionNameNode.getTokenValue();
    final Location location = new Location(uri, conditionNameNode);
    final Location validLocation = Location.validLocation(location);
    return new ConditionUsage(conditionName, validLocation, conditionNameNode);
  }

  /**
   * Create the usage of the condition a {@code condition.raise()} or {@code condition.new()} names.
   *
   * @param uri URI of the file holding the node.
   * @param invocationNode {@code METHOD_INVOCATION} node.
   * @return Condition usage, or {@code null} when the invocation names no condition.
   */
  @CheckForNull
  private static ConditionUsage createRaisedConditionUsage(
      final URI uri, final AstNode invocationNode) {
    final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(invocationNode);
    if (!helper.isMethodInvocationOf(CONDITION, RAISE_CALL)
        && !helper.isMethodInvocationOf(SW_CONDITION, RAISE_CALL)
        && !helper.isMethodInvocationOf(CONDITION, NEW_CALL)
        && !helper.isMethodInvocationOf(SW_CONDITION, NEW_CALL)) {
      return null;
    }

    final AstNode argumentsNode = invocationNode.getFirstChild(MagikGrammar.ARGUMENTS);
    final ArgumentsNodeHelper argumentsHelper = new ArgumentsNodeHelper(argumentsNode);
    final AstNode argumentNode = argumentsHelper.getArgument(0, MagikGrammar.SYMBOL);
    if (argumentNode == null) {
      return null;
    }

    final String conditionName = argumentNode.getTokenValue().substring(1);
    final Location location = new Location(uri, argumentsNode);
    final Location validLocation = Location.validLocation(location);
    // TODO: Set location to identifier node.
    return new ConditionUsage(conditionName, validLocation, invocationNode);
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
      final MethodUsage usage =
          DefinitionUsageParser.createUnaryOperatorUsage(uri, unaryExpressionNode);
      if (usage != null) {
        usages.add(usage);
      }
    }

    return usages;
  }

  /**
   * Create the usage of the method a unary operator invokes.
   *
   * @param uri URI of the file holding the node.
   * @param unaryExpressionNode {@code UNARY_EXPRESSION} node.
   * @return Method usage, or {@code null} for an operator without a method.
   */
  @CheckForNull
  private static MethodUsage createUnaryOperatorUsage(
      final URI uri, final AstNode unaryExpressionNode) {
    // `_allresults` has no method equivalent; an unmapped operator is not a usage.
    final UnaryOperatorHelper helper = new UnaryOperatorHelper(unaryExpressionNode);
    final String methodName = helper.getUnaryOperatorMethod();
    if (methodName == null) {
      return null;
    }

    final Location location = new Location(uri, unaryExpressionNode);
    final Location validLocation = Location.validLocation(location);
    return new MethodUsage(TypeString.UNDEFINED, methodName, validLocation, unaryExpressionNode);
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
            augmentedNode ->
                DefinitionUsageParser.createAugmentedOperatorUsage(uri, augmentedNode));
  }

  /**
   * Create the usage of the operator of an augmented assignment.
   *
   * @param uri URI of the file holding the node.
   * @param augmentedNode {@code AUGMENTED_ASSIGNMENT_EXPRESSION} node.
   * @return Binary operator usage.
   */
  private static BinaryOperatorUsage createAugmentedOperatorUsage(
      final URI uri, final AstNode augmentedNode) {
    // Children: <lhs>, OPERATOR (e.g. `+`), CHEVRON, <rhs>.
    final AstNode operatorNode = augmentedNode.getChildren().get(1);
    final String operator = operatorNode.getTokenValue().toLowerCase();
    final Location location = new Location(uri, augmentedNode);
    final Location validLocation = Location.validLocation(location);
    return new BinaryOperatorUsage(
        TypeString.UNDEFINED, TypeString.UNDEFINED, operator, validLocation, augmentedNode);
  }

  private Stream<BinaryOperatorUsage> getBinaryExpressionOperators(final URI uri) {
    return this.node.getDescendants(BINARY_EXPRESSIONS).stream()
        .flatMap(
            binaryExpressionNode ->
                DefinitionUsageParser.createBinaryOperatorUsages(uri, binaryExpressionNode)
                    .stream());
  }

  /**
   * Create the usages of the operators of a binary expression, one per operator.
   *
   * @param uri URI of the file holding the node.
   * @param binaryExpressionNode Binary expression node.
   * @return Binary operator usages.
   */
  private static List<BinaryOperatorUsage> createBinaryOperatorUsages(
      final URI uri, final AstNode binaryExpressionNode) {
    final BinaryExpressionNodeHelper helper = new BinaryExpressionNodeHelper(binaryExpressionNode);
    final Location location = new Location(uri, binaryExpressionNode);
    final Location validLocation = Location.validLocation(location);
    return helper.getTriplets().stream()
        .map(
            triplet -> {
              final AstNode operatorNode = triplet.getMiddle();
              final String operator = operatorNode.getTokenValue().toLowerCase();
              return new BinaryOperatorUsage(
                  TypeString.UNDEFINED,
                  TypeString.UNDEFINED,
                  operator,
                  validLocation,
                  binaryExpressionNode);
            })
        .toList();
  }

  /**
   * Collect the usages under {@code rootNodes} that the file's settings index, in one walk that
   * skips the subtrees {@code skipped} matches. Method invocations precede unary operators, handled
   * conditions precede raised ones, binary expressions precede augmented assignments.
   *
   * @param magikFile The file holding the nodes.
   * @param rootNodes The nodes to collect under.
   * @param skipped Whether a node's subtree belongs to another definition.
   * @return The usages.
   */
  public static Usages collectUsages(
      final MagikFile magikFile, final List<AstNode> rootNodes, final Predicate<AstNode> skipped) {
    final List<AstNode> nodes = DefinitionUsageParser.collectNodes(rootNodes, skipped);
    final MagikToolsProperties properties = magikFile.getProperties();
    final MagikAnalysisSettings settings = new MagikAnalysisSettings(properties);
    final URI uri = magikFile.getUri();
    final Stream<MethodUsage> invoked =
        settings.getTypingIndexMethodUsages()
            ? DefinitionUsageParser.selectNodes(nodes, MagikGrammar.METHOD_INVOCATION)
                .map(node -> DefinitionUsageParser.createMethodUsage(uri, node))
            : Stream.empty();
    final Stream<MethodUsage> unary =
        settings.getTypingIndexUnaryOperatorUsages()
            ? DefinitionUsageParser.selectNodes(nodes, MagikGrammar.UNARY_EXPRESSION)
                .map(node -> DefinitionUsageParser.createUnaryOperatorUsage(uri, node))
                .filter(Objects::nonNull)
            : Stream.empty();
    final List<GlobalUsage> globals =
        settings.getTypingIndexGlobalUsages()
            ? DefinitionUsageParser.selectNodes(nodes, MagikGrammar.IDENTIFIER)
                .map(node -> DefinitionUsageParser.createGlobalUsage(magikFile, node))
                .filter(Objects::nonNull)
                .toList()
            : List.of();
    final List<ConditionUsage> conditions =
        settings.getTypingIndexConditionUsages()
            ? DefinitionUsageParser.collectConditionUsages(uri, nodes)
            : List.of();
    final List<BinaryOperatorUsage> binaryOperators =
        settings.getTypingIndexBinaryOperatorUsages()
            ? DefinitionUsageParser.collectBinaryOperatorUsages(uri, nodes)
            : List.of();
    final List<MethodUsage> methods = Stream.concat(invoked, unary).toList();
    return new Usages(methods, globals, conditions, binaryOperators);
  }

  private static List<ConditionUsage> collectConditionUsages(
      final URI uri, final List<AstNode> nodes) {
    final Stream<ConditionUsage> handled =
        DefinitionUsageParser.selectNodes(nodes, MagikGrammar.CONDITION_NAME)
            .map(node -> DefinitionUsageParser.createHandledConditionUsage(uri, node));
    final Stream<ConditionUsage> raised =
        DefinitionUsageParser.selectNodes(nodes, MagikGrammar.METHOD_INVOCATION)
            .map(node -> DefinitionUsageParser.createRaisedConditionUsage(uri, node))
            .filter(Objects::nonNull);
    return Stream.concat(handled, raised).toList();
  }

  private static List<BinaryOperatorUsage> collectBinaryOperatorUsages(
      final URI uri, final List<AstNode> nodes) {
    final Stream<BinaryOperatorUsage> expressions =
        DefinitionUsageParser.selectNodes(nodes, BINARY_EXPRESSIONS)
            .map(node -> DefinitionUsageParser.createBinaryOperatorUsages(uri, node))
            .flatMap(List::stream);
    final Stream<BinaryOperatorUsage> augmented =
        DefinitionUsageParser.selectNodes(nodes, MagikGrammar.AUGMENTED_ASSIGNMENT_EXPRESSION)
            .map(node -> DefinitionUsageParser.createAugmentedOperatorUsage(uri, node));
    return Stream.concat(expressions, augmented).toList();
  }

  /** The usage of the global or dynamic an identifier reads, if it reads one. */
  @CheckForNull
  private static GlobalUsage createGlobalUsage(
      final MagikFile magikFile, final AstNode identifierNode) {
    // A method or condition name is no variable, though a global may share its name.
    final AstNode parentNode = identifierNode.getParent();
    final GlobalScope globalScope = magikFile.getGlobalScope();
    final Scope scope = globalScope.getScopeForNode(identifierNode);
    if (!parentNode.is(MagikGrammar.ATOM) || scope == null) {
      return null;
    }

    final ScopeEntry scopeEntry = scope.getScopeEntry(identifierNode);
    if (scopeEntry == null || !scopeEntry.isType(ScopeEntry.Type.GLOBAL, ScopeEntry.Type.DYNAMIC)) {
      return null;
    }

    final PackageNodeHelper packageHelper = new PackageNodeHelper(identifierNode);
    final String currentPakkage = packageHelper.getCurrentPackage();
    final String identifier = identifierNode.getTokenValue();
    final TypeString ref = TypeString.ofIdentifier(identifier, currentPakkage);
    final URI uri = magikFile.getUri();
    final Location location = new Location(uri, identifierNode);
    final Location validLocation = Location.validLocation(location);
    return new GlobalUsage(ref, validLocation, identifierNode);
  }

  private static Stream<AstNode> selectNodes(
      final List<AstNode> nodes, final AstNodeType... types) {
    return nodes.stream().filter(node -> node.is(types));
  }

  /**
   * Collect the nodes under {@code rootNodes}, roots included, in document order, skipping the
   * subtrees {@code skipped} matches.
   *
   * @param rootNodes The nodes to collect under.
   * @param skipped Whether a node's subtree is skipped.
   * @return The nodes.
   */
  static List<AstNode> collectNodes(
      final List<AstNode> rootNodes, final Predicate<AstNode> skipped) {
    final List<AstNode> found = new ArrayList<>();
    final Deque<AstNode> pending = new ArrayDeque<>(rootNodes);
    while (!pending.isEmpty()) {
      final AstNode node = pending.pollFirst();
      if (skipped.test(node)) {
        continue;
      }

      found.add(node);
      final List<AstNode> children = node.getChildren();
      for (int index = children.size() - 1; index >= 0; --index) {
        pending.addFirst(children.get(index));
      }
    }

    return found;
  }
}
