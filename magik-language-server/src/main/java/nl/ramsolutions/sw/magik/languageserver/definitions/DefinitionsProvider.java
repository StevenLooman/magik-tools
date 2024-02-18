package nl.ramsolutions.sw.magik.languageserver.definitions;

import com.sonar.sslr.api.AstNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.PackageNodeHelper;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.Condition;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResult;
import nl.ramsolutions.sw.magik.analysis.typing.types.Method;
import nl.ramsolutions.sw.magik.analysis.typing.types.SelfType;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.types.UndefinedType;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.eclipse.lsp4j.ServerCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Definitions provider. */
public class DefinitionsProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefinitionsProvider.class);

  /**
   * Set server capabilities.
   *
   * @param capabilities Server capabilities.
   */
  public void setCapabilities(final ServerCapabilities capabilities) {
    capabilities.setDefinitionProvider(true);
  }

  /**
   * Provide definitions.
   *
   * @param magikFile Magik file.
   * @param position Position.
   * @return Definitions.
   */
  public List<Location> provideDefinitions(
      final MagikTypedFile magikFile, final Position position) {
    // Parse magik.
    final AstNode node = magikFile.getTopNode();

    // Should always be on an identifier.
    final AstNode currentNode = AstQuery.nodeAt(node, position, MagikGrammar.IDENTIFIER);
    if (currentNode == null) {
      return Collections.emptyList();
    }

    final AstNode wantedNode =
        currentNode.getFirstAncestor(
            MagikGrammar.METHOD_INVOCATION,
            MagikGrammar.METHOD_DEFINITION,
            MagikGrammar.ATOM,
            MagikGrammar.CONDITION_NAME);
    LOGGER.trace("Wanted node: {}", wantedNode);
    if (wantedNode == null) {
      return Collections.emptyList();
    } else if (wantedNode.is(MagikGrammar.METHOD_INVOCATION)) {
      return this.definitionsForMethodInvocation(magikFile, currentNode);
    } else if (wantedNode.is(MagikGrammar.ATOM)
        && wantedNode.getFirstChild().is(MagikGrammar.IDENTIFIER)) {
      return this.definitionsForAtom(magikFile, currentNode);
    } else if (wantedNode.is(MagikGrammar.CONDITION_NAME)) {
      return this.definitionsForCondition(magikFile, currentNode);
    }

    // TODO: Slot definitions.

    return Collections.emptyList();
  }

  private List<Location> definitionsForCondition(
      final MagikTypedFile magikFile, final AstNode wantedNode) {
    final ITypeKeeper typeKeeper = magikFile.getTypeKeeper();
    final String conditionName = wantedNode.getTokenValue();
    final Condition condition = typeKeeper.getCondition(conditionName);
    if (condition == null) {
      return Collections.emptyList();
    }

    final Location conditionLocation = condition.getLocation();
    return List.of(Location.validLocation(conditionLocation));
  }

  private List<Location> definitionsForAtom(
      final MagikTypedFile magikFile, final AstNode wantedNode) {
    final Scope scope = magikFile.getGlobalScope().getScopeForNode(wantedNode);
    Objects.requireNonNull(scope);
    final String identifier = wantedNode.getTokenValue();
    final ScopeEntry scopeEntry = scope.getScopeEntry(identifier);
    if (scopeEntry == null) {
      return Collections.emptyList();
    }

    if (scopeEntry.isType(
        ScopeEntry.Type.DEFINITION,
        ScopeEntry.Type.LOCAL,
        ScopeEntry.Type.IMPORT,
        ScopeEntry.Type.CONSTANT,
        ScopeEntry.Type.PARAMETER)) {
      final AstNode definitionNode = scopeEntry.getDefinitionNode();
      final Location definitionLocation = new Location(magikFile.getUri(), definitionNode);
      return List.of(definitionLocation);
    }

    // Assume type.
    final PackageNodeHelper packageHelper = new PackageNodeHelper(wantedNode);
    final String pakkage = packageHelper.getCurrentPackage();
    final TypeString typeString = TypeString.ofIdentifier(identifier, pakkage);
    final ITypeKeeper typeKeeper = magikFile.getTypeKeeper();
    final AbstractType type = typeKeeper.getType(typeString);
    if (type == UndefinedType.INSTANCE) {
      return Collections.emptyList();
    }

    final Location typeLocation = type.getLocation();
    return List.of(Location.validLocation(typeLocation));
  }

  @SuppressWarnings("checkstyle:NestedIfDepth")
  private List<Location> definitionsForMethodInvocation(
      final MagikTypedFile magikFile, final AstNode wantedNode) {
    final AstNode methodInvocationNode =
        wantedNode.getFirstAncestor(MagikGrammar.METHOD_INVOCATION);
    final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(methodInvocationNode);
    final String methodName = helper.getMethodName();

    final AstNode previousSiblingNode = methodInvocationNode.getPreviousSibling();
    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();
    final ExpressionResult result = reasonerState.getNodeType(previousSiblingNode);

    final ITypeKeeper typeKeeper = magikFile.getTypeKeeper();
    final AbstractType unsetType = typeKeeper.getType(TypeString.SW_UNSET);
    AbstractType type = result.get(0, unsetType);
    final List<Location> locations = new ArrayList<>();
    if (type == UndefinedType.INSTANCE) {
      LOGGER.debug("Finding implementations for method: {}", methodName);
      typeKeeper.getTypes().stream()
          .flatMap(anyType -> anyType.getMethods().stream())
          .filter(m -> m.getName().equals(methodName))
          .map(Method::getLocation)
          .map(Location::validLocation)
          .forEach(locations::add);
    } else {
      if (type == SelfType.INSTANCE) {
        final AstNode methodDefNode = wantedNode.getFirstAncestor(MagikGrammar.METHOD_DEFINITION);
        if (methodDefNode == null) {
          type = UndefinedType.INSTANCE;
        } else {
          final MethodDefinitionNodeHelper methodDefHelper =
              new MethodDefinitionNodeHelper(methodDefNode);
          final TypeString typeString = methodDefHelper.getTypeString();
          type = typeKeeper.getType(typeString);
        }
      }
      LOGGER.debug(
          "Finding implementations for type:, {}, method: {}", type.getFullName(), methodName);
      type.getMethods(methodName).stream()
          .map(Method::getLocation)
          .map(Location::validLocation)
          .forEach(locations::add);
    }

    return locations;
  }
}
