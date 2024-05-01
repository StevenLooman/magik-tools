package nl.ramsolutions.sw.magik.languageserver.definitions;

import com.sonar.sslr.api.AstNode;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import nl.ramsolutions.sw.definitions.ModuleDefinition;
import nl.ramsolutions.sw.definitions.ProductDefinition;
import nl.ramsolutions.sw.definitions.api.SwModuleDefinitionGrammar;
import nl.ramsolutions.sw.definitions.api.SwProductDefinitionGrammar;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.ModuleDefFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.ProductDefFile;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.definitions.ConditionDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
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

/** Definitions provider. */
public class DefinitionsProvider {

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
   * @param productDefFile Product.def file.
   * @param position Position.
   * @return Definitions.
   */
  public List<Location> provideDefinitions(
      final ProductDefFile productDefFile, final Position position) {
    final AstNode node = productDefFile.getTopNode();
    final AstNode hoveredTokenNode = AstQuery.nodeAt(node, position);
    if (hoveredTokenNode == null) {
      return Collections.emptyList();
    }

    final AstNode productNameNode =
        AstQuery.getParentFromChain(
            hoveredTokenNode,
            SwProductDefinitionGrammar.IDENTIFIER,
            SwProductDefinitionGrammar.PRODUCT_NAME);
    if (productNameNode == null) {
      return Collections.emptyList();
    }

    return this.locationsForProductName(productDefFile, productNameNode);
  }

  /**
   * Provide definitions.
   *
   * @param moduleDefFile Module.def file.
   * @param position Position.
   * @return Definitions.
   */
  public List<Location> provideDefinitions(
      final ModuleDefFile moduleDefFile, final Position position) {
    final AstNode node = moduleDefFile.getTopNode();
    final AstNode hoveredTokenNode = AstQuery.nodeAt(node, position);
    if (hoveredTokenNode == null) {
      return Collections.emptyList();
    }

    final AstNode moduleNameNode =
        AstQuery.getParentFromChain(
            hoveredTokenNode,
            SwModuleDefinitionGrammar.IDENTIFIER,
            SwModuleDefinitionGrammar.MODULE_NAME);
    if (moduleNameNode == null) {
      return Collections.emptyList();
    }

    return this.locationsForModuleName(moduleDefFile, moduleNameNode);
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
    // Should always be on an identifier.
    final AstNode node = magikFile.getTopNode();
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
        .map(ConditionDefinition::getLocation)
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
        .map(IDefinition::getLocation)
        .map(Location::validLocation)
        .toList();
  }

  @SuppressWarnings("checkstyle:NestedIfDepth")
  private List<Location> locationsForMethodInvocation(
      final MagikTypedFile magikFile, final AstNode wantedNode) {
    final AstNode methodInvocationNode =
        wantedNode.getFirstAncestor(MagikGrammar.METHOD_INVOCATION);
    final MethodInvocationNodeHelper invocationHelper =
        new MethodInvocationNodeHelper(methodInvocationNode);
    final String methodName = invocationHelper.getMethodName();

    final AstNode previousSiblingNode = methodInvocationNode.getPreviousSibling();
    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();
    final ExpressionResultString result = reasonerState.getNodeType(previousSiblingNode);
    final TypeString resultTypeStr = result.get(0, TypeString.UNDEFINED);
    final TypeString typeStr;
    if (resultTypeStr.isSelf()) {
      final AstNode definitionNode =
          methodInvocationNode.getFirstAncestor(
              MagikGrammar.METHOD_DEFINITION, MagikGrammar.PROCEDURE_DEFINITION);
      if (definitionNode == null) {
        typeStr = resultTypeStr;
      } else if (definitionNode.is(MagikGrammar.METHOD_DEFINITION)) {
        final MethodDefinitionNodeHelper definitionHelper =
            new MethodDefinitionNodeHelper(definitionNode);
        typeStr = definitionHelper.getTypeString();
      } else {
        typeStr = TypeString.SW_PROCEDURE;
      }
    } else {
      typeStr = resultTypeStr;
    }

    final TypeStringResolver resolver = magikFile.getTypeStringResolver();
    return resolver.getMethodDefinitions(typeStr, methodName).stream()
        .map(MethodDefinition::getLocation)
        .map(Location::validLocation)
        .toList();
  }

  private List<Location> locationsForProductName(
      final ProductDefFile productDefFile, final AstNode productNameNode) {
    final String productName = productNameNode.getTokenValue();
    final IDefinitionKeeper definitionKeeper = productDefFile.getDefinitionKeeper();
    return definitionKeeper.getProductDefinitions(productName).stream()
        .map(ProductDefinition::getLocation)
        .toList();
  }

  private List<Location> locationsForModuleName(
      final ModuleDefFile moduleDefFile, final AstNode moduleNameNode) {
    final String moduleName = moduleNameNode.getTokenValue();
    final IDefinitionKeeper definitionKeeper = moduleDefFile.getDefinitionKeeper();
    return definitionKeeper.getModuleDefinitions(moduleName).stream()
        .map(ModuleDefinition::getLocation)
        .toList();
  }
}
