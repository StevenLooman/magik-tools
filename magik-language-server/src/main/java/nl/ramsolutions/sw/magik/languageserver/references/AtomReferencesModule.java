package nl.ramsolutions.sw.magik.languageserver.references;

import com.sonar.sslr.api.AstNode;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.helpers.PackageNodeHelper;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/** Provides references to an identifier atom: a scope entry's usages, or otherwise a type. */
public class AtomReferencesModule implements ReferencesModule<MagikTypedFile> {

  @Override
  public Optional<List<Location>> tryReferences(final ReferencesContext<MagikTypedFile> context) {
    final AstNode positionNode = context.positionNode();
    final AstNode wantedNode = ReferencesAncestor.nearest(positionNode);
    if (wantedNode == null
        || !wantedNode.is(MagikGrammar.ATOM)
        || !wantedNode.getFirstChild().is(MagikGrammar.IDENTIFIER)) {
      return Optional.empty();
    }

    final MagikTypedFile magikFile = context.file();
    final Scope scope = magikFile.getGlobalScope().getScopeForNode(wantedNode);
    Objects.requireNonNull(scope);
    final String identifier = positionNode.getTokenValue();
    final ScopeEntry scopeEntry = scope.getScopeEntry(identifier);
    if (scopeEntry == null) {
      return Optional.of(Collections.emptyList());
    }

    if (scopeEntry.isType(
        ScopeEntry.Type.DEFINITION,
        ScopeEntry.Type.LOCAL,
        ScopeEntry.Type.IMPORT,
        ScopeEntry.Type.CONSTANT,
        ScopeEntry.Type.PARAMETER)) {
      final URI uri = magikFile.getUri();
      final List<Location> locations =
          scopeEntry.getUsages().stream().map(usageNode -> new Location(uri, usageNode)).toList();
      return Optional.of(locations);
    }

    if (scopeEntry.isType(ScopeEntry.Type.GLOBAL, ScopeEntry.Type.DYNAMIC)) {
      final PackageNodeHelper packageHelper = new PackageNodeHelper(wantedNode);
      final String pakkage = packageHelper.getCurrentPackage();
      final TypeString typeString = TypeString.ofIdentifier(identifier, pakkage);
      final IDefinitionKeeper definitionKeeper = magikFile.getDefinitionKeeper();
      final List<Location> locations =
          TypeReferencesFinder.referencesToType(definitionKeeper, typeString);
      return Optional.of(locations);
    }

    return Optional.of(Collections.emptyList());
  }
}
