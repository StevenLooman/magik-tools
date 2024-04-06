package nl.ramsolutions.sw.magik.languageserver.definitions;

import com.sonar.sslr.api.AstNode;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.PackageNodeHelper;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
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
      return this.locationsForMethodInvocation(magikFile, currentNode);
    } else if (wantedNode.is(MagikGrammar.ATOM)
        && wantedNode.getFirstChild().is(MagikGrammar.IDENTIFIER)) {
      return this.locationsForAtom(magikFile, currentNode);
    } else if (wantedNode.is(MagikGrammar.CONDITION_NAME)) {
      return this.locationsForCondition(magikFile, currentNode);
    }

    // TODO: Slot definitions.

    return Collections.emptyList();
  }

  private List<Location> locationsForCondition(
      final MagikTypedFile magikFile, final AstNode wantedNode) {
    final IDefinitionKeeper definitionKeeper = magikFile.getDefinitionKeeper();
    final String conditionName = wantedNode.getTokenValue();
    return definitionKeeper.getConditionDefinitions(conditionName).stream()
        .map(conditionDef -> conditionDef.getLocation())
        .map(Location::validLocation)
        .toList();
  }

  private List<Location> locationsForAtom(
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
    final TypeStringResolver resolver = magikFile.getTypeStringResolver();
    return resolver.resolve(typeString).stream()
        .map(def -> def.getLocation())
        .map(Location::validLocation)
        .toList();
  }

  @SuppressWarnings("checkstyle:NestedIfDepth")
  private List<Location> locationsForMethodInvocation(
      final MagikTypedFile magikFile, final AstNode wantedNode) {
    final AstNode methodInvocationNode =
        wantedNode.getFirstAncestor(MagikGrammar.METHOD_INVOCATION);
    final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(methodInvocationNode);
    final String methodName = helper.getMethodName();

    final AstNode previousSiblingNode = methodInvocationNode.getPreviousSibling();
    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();
    final ExpressionResultString result = reasonerState.getNodeType(previousSiblingNode);
    final TypeString typeStr = result.get(0, TypeString.UNDEFINED);

    final TypeStringResolver resolver = magikFile.getTypeStringResolver();
    return resolver.getMethodDefinitions(typeStr, methodName).stream()
        .map(MethodDefinition::getLocation)
        .map(Location::validLocation)
        .toList();
  }
}
