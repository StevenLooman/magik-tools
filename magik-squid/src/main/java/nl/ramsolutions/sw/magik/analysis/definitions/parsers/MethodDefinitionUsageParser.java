package nl.ramsolutions.sw.magik.analysis.definitions.parsers;

import com.sonar.sslr.api.AstNode;
import java.net.URI;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.definitions.ConditionUsage;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalUsage;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodUsage;
import nl.ramsolutions.sw.magik.analysis.definitions.SlotUsage;
import nl.ramsolutions.sw.magik.analysis.helpers.ArgumentsNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.PackageNodeHelper;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeBuilderVisitor;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/** Method Definition usages parser. */
public class MethodDefinitionUsageParser {

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
  public Set<GlobalUsage> getUsedGlobals() {
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
              final Location location = new Location(uri, scopeEntry.getDefinitionNode());
              final Location validLocation = Location.validLocation(location);
              // TODO: The type should be resolved here, but we don't have a type resolver
              // yet.
              // Now you might "see" the ref user:char16_vector, or any other package which is
              // a child of `sw`.
              // This will most likely be indexed invalidly.
              // Though, we might be able to resolve it during the query itself.
              return new GlobalUsage(ref, validLocation);
            })
        .filter(Objects::nonNull)
        .collect(Collectors.toUnmodifiableSet());
  }

  /**
   * Get the used methods.
   *
   * @return Used methods.
   */
  public Set<MethodUsage> getUsedMethods() {
    return this.node.getDescendants(MagikGrammar.METHOD_INVOCATION).stream()
        .map(
            methodInvocationNode -> {
              final MethodInvocationNodeHelper helper =
                  new MethodInvocationNodeHelper(methodInvocationNode);
              final String methodName = helper.getMethodName();
              final URI uri = this.node.getToken().getURI();
              final Location location = new Location(uri, methodInvocationNode);
              final Location validLocation = Location.validLocation(location);
              return new MethodUsage(TypeString.UNDEFINED, methodName, validLocation);
            })
        .collect(Collectors.toUnmodifiableSet());
  }

  /**
   * Get the used slots.
   *
   * @return Used slots.
   */
  public Set<SlotUsage> getUsedSlots() {
    return this.node.getDescendants(MagikGrammar.SLOT).stream()
        .map(
            slotNode -> {
              final String slotName =
                  slotNode.getFirstChild(MagikGrammar.IDENTIFIER).getTokenValue();
              final URI uri = this.node.getToken().getURI();
              final Location location = new Location(uri, slotNode);
              final Location validLocation = Location.validLocation(location);
              return new SlotUsage(slotName, validLocation);
            })
        .collect(Collectors.toUnmodifiableSet());
  }

  /**
   * Get the used conditions.
   *
   * @return Used conditions.
   */
  public Set<ConditionUsage> getUsedConditions() {
    final URI uri = this.node.getToken().getURI();
    final Stream<ConditionUsage> handledConditions =
        this.node.getDescendants(MagikGrammar.CONDITION_NAME).stream()
            .map(
                conditionNameNode -> {
                  final String conditionName = conditionNameNode.getTokenValue();
                  final Location location = new Location(uri, conditionNameNode);
                  final Location validLocation = Location.validLocation(location);
                  return new ConditionUsage(conditionName, validLocation);
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
                  return new ConditionUsage(conditionName, validLocation);
                })
            .filter(Objects::nonNull);
    return Stream.concat(handledConditions, raisedConditions)
        .collect(Collectors.toUnmodifiableSet());
  }
}
