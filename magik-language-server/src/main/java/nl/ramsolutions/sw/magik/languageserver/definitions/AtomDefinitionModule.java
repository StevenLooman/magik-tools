package nl.ramsolutions.sw.magik.languageserver.definitions;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import nl.ramsolutions.sw.IDefinition;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.helpers.PackageNodeHelper;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/** Provides definitions for an identifier atom: a scope entry, or otherwise a global type. */
public class AtomDefinitionModule implements DefinitionModule<MagikTypedFile> {

  @Override
  public Optional<List<Location>> tryDefinitions(final DefinitionContext<MagikTypedFile> context) {
    final AstNode positionNode = context.positionNode();
    final AstNode wantedNode = DefinitionAncestor.nearest(positionNode);
    if (wantedNode == null
        || !wantedNode.is(MagikGrammar.ATOM)
        || !wantedNode.getFirstChild().is(MagikGrammar.IDENTIFIER)) {
      return Optional.empty();
    }

    final MagikTypedFile magikFile = context.file();
    final Scope scope = magikFile.getGlobalScope().getScopeForNode(positionNode);
    Objects.requireNonNull(scope);
    final String identifier = positionNode.getTokenValue();
    final ScopeEntry scopeEntry = scope.getScopeEntry(identifier);
    if (scopeEntry == null) {
      return Optional.of(List.of());
    }

    if (scopeEntry.isType(
        ScopeEntry.Type.DEFINITION,
        ScopeEntry.Type.LOCAL,
        ScopeEntry.Type.IMPORT,
        ScopeEntry.Type.CONSTANT,
        ScopeEntry.Type.PARAMETER)) {
      final AstNode definitionNode = scopeEntry.getDefinitionNode();
      final Location definitionLocation = new Location(magikFile.getUri(), definitionNode);
      return Optional.of(List.of(definitionLocation));
    }

    // Assume type.
    final PackageNodeHelper packageHelper = new PackageNodeHelper(positionNode);
    final String pakkage = packageHelper.getCurrentPackage();
    final TypeString typeString = TypeString.ofIdentifier(identifier, pakkage);
    final TypeStringResolver resolver = magikFile.getTypeStringResolver();
    final List<Location> locations =
        resolver.resolve(typeString).stream()
            .map(IDefinition::getLocation)
            .map(Location::validLocation)
            .toList();
    return Optional.of(locations);
  }
}
