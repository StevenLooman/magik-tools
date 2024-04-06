package nl.ramsolutions.sw.magik.languageserver.references;

import com.sonar.sslr.api.AstNode;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.definitions.ConditionUsage;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalUsage;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodUsage;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.PackageNodeHelper;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.eclipse.lsp4j.ServerCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** References provider. */
public class ReferencesProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReferencesProvider.class);

  /**
   * Set server capabilities.
   *
   * @param capabilities Server capabilities.
   */
  public void setCapabilities(final ServerCapabilities capabilities) {
    capabilities.setReferencesProvider(true);
  }

  /**
   * Provide references.
   *
   * @param magikFile Magik file.
   * @param position Position in file.
   * @return Locations for references.
   */
  @SuppressWarnings("checkstyle:NestedIfDepth")
  public List<Location> provideReferences(final MagikTypedFile magikFile, final Position position) {
    // Parse magik.
    final AstNode node = magikFile.getTopNode();
    final IDefinitionKeeper definitionKeeper = magikFile.getDefinitionKeeper();

    // Should always be on an identifier.
    final AstNode currentNode = AstQuery.nodeAt(node, position, MagikGrammar.IDENTIFIER);
    if (currentNode == null) {
      return Collections.emptyList();
    }

    final AstNode wantedNode =
        currentNode.getFirstAncestor(
            MagikGrammar.METHOD_INVOCATION,
            MagikGrammar.METHOD_NAME,
            MagikGrammar.EXEMPLAR_NAME,
            MagikGrammar.ATOM,
            MagikGrammar.CONDITION_NAME);
    LOGGER.trace("Wanted node: {}", wantedNode);
    final PackageNodeHelper packageHelper = new PackageNodeHelper(wantedNode);
    if (wantedNode == null) {
      return Collections.emptyList();
    } else if (wantedNode.is(MagikGrammar.METHOD_INVOCATION)) {
      final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(wantedNode);
      final String methodName = helper.getMethodName();
      return this.referencesToMethod(definitionKeeper, TypeString.UNDEFINED, methodName);
    } else if (wantedNode.is(MagikGrammar.METHOD_NAME)) {
      final AstNode methodDefinitionNode = wantedNode.getParent();
      final MethodDefinitionNodeHelper helper =
          new MethodDefinitionNodeHelper(methodDefinitionNode);
      final String methodName = helper.getMethodName();
      return this.referencesToMethod(definitionKeeper, TypeString.UNDEFINED, methodName);
    } else if (wantedNode.is(MagikGrammar.EXEMPLAR_NAME)) {
      final String identifier = currentNode.getTokenValue();
      final String pakkage = packageHelper.getCurrentPackage();
      final TypeString typeString = TypeString.ofIdentifier(identifier, pakkage);
      return this.referencesToType(definitionKeeper, typeString);
    } else if (wantedNode.is(MagikGrammar.ATOM)
        && wantedNode.getFirstChild().is(MagikGrammar.IDENTIFIER)) {
      final Scope scope = magikFile.getGlobalScope().getScopeForNode(wantedNode);
      Objects.requireNonNull(scope);
      final String identifier = currentNode.getTokenValue();
      final ScopeEntry scopeEntry = scope.getScopeEntry(identifier);
      if (scopeEntry == null) {
        return Collections.emptyList();
      } else if (scopeEntry.isType(
          ScopeEntry.Type.DEFINITION,
          ScopeEntry.Type.LOCAL,
          ScopeEntry.Type.IMPORT,
          ScopeEntry.Type.CONSTANT,
          ScopeEntry.Type.PARAMETER)) {
        final List<AstNode> usages = scopeEntry.getUsages();
        final URI uri = magikFile.getUri();
        return usages.stream().map(usageNode -> new Location(uri, usageNode)).toList();
      } else if (scopeEntry.isType(ScopeEntry.Type.GLOBAL, ScopeEntry.Type.DYNAMIC)) {
        final String pakkage = packageHelper.getCurrentPackage();
        final TypeString typeString = TypeString.ofIdentifier(identifier, pakkage);
        return this.referencesToType(definitionKeeper, typeString);
      }
    } else if (wantedNode.is(MagikGrammar.CONDITION_NAME)) {
      final String conditionName = currentNode.getTokenValue();
      LOGGER.debug("Getting references to condition: {}", conditionName);
      return this.referencesToCondition(definitionKeeper, conditionName);
    }

    // TODO: Slot references.

    return Collections.emptyList();
  }

  private List<Location> referencesToMethod(
      final IDefinitionKeeper definitionKeeper,
      final TypeString typeName,
      final String methodName) {
    LOGGER.debug("Finding references to method: {}", methodName);

    // Build set of types which may contain this method: type + ancestors.
    final Set<TypeString> wantedTypeRefs = new HashSet<>();
    wantedTypeRefs.add(TypeString.UNDEFINED); // For unreasoned/undetermined calls.
    wantedTypeRefs.add(typeName);
    // TODO: Add all ancestors too?

    final Collection<MethodUsage> searchedMethodUsages =
        wantedTypeRefs.stream()
            .map(wantedTypeRef -> new MethodUsage(wantedTypeRef, methodName))
            .collect(Collectors.toSet());
    final Predicate<MethodUsage> filterPredicate = searchedMethodUsages::contains;

    // Find references.
    return definitionKeeper.getMethodDefinitions().stream()
        .flatMap(def -> def.getUsedMethods().stream())
        .filter(filterPredicate::test)
        .map(MethodUsage::getLocation)
        .map(Location::validLocation)
        .toList();
  }

  private List<Location> referencesToType(
      final IDefinitionKeeper definitionKeeper, final TypeString typeString) {
    LOGGER.debug("Finding references to type: {}", typeString);

    final TypeStringResolver resolver = new TypeStringResolver(definitionKeeper);
    final ExemplarDefinition exemplarDefinition = resolver.getExemplarDefinition(typeString);
    if (exemplarDefinition == null) {
      return Collections.emptyList();
    }

    // TODO: We need to resolve the referenced types, as the indexed globals might not have the
    // right
    //       (unresolved) package. I.e., We might need to match only on identifier, as the
    // usedGlobal might have a
    //       different package? This is because the ref might be stored with the current package.
    final TypeString exemplarTypeString = exemplarDefinition.getTypeString();
    final Set<TypeString> searchedTypes = Set.of(exemplarTypeString);
    final Collection<GlobalUsage> wantedGlobalUsages =
        searchedTypes.stream()
            .map(wantedTypeRef -> new GlobalUsage(wantedTypeRef, null))
            .collect(Collectors.toSet());
    final Predicate<GlobalUsage> filterPredicate = wantedGlobalUsages::contains;

    // Find references.
    // TODO: Also parameters, return types of methods/procedures.
    // TODO: Also slots of methods.
    // TODO: Also search in all procedures.
    return definitionKeeper.getMethodDefinitions().stream()
        .flatMap(def -> def.getUsedGlobals().stream())
        .filter(filterPredicate::test)
        .map(GlobalUsage::getLocation)
        .map(Location::validLocation)
        .toList();
  }

  private List<Location> referencesToCondition(
      final IDefinitionKeeper definitionKeeper, final String conditionName) {
    LOGGER.debug("Finding references to condition: {}", conditionName);
    return definitionKeeper.getMethodDefinitions().stream()
        .flatMap(def -> def.getUsedConditions().stream())
        .filter(conditionUsage -> conditionUsage.getConditionName().equals(conditionName))
        .map(ConditionUsage::getLocation)
        .map(Location::validLocation)
        .toList();
  }
}
