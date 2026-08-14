package nl.ramsolutions.sw.magik.languageserver.references;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import java.util.Optional;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.helpers.PackageNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/** Provides references to a type, from its exemplar name in a method definition's header. */
public class ExemplarNameReferencesModule implements ReferencesModule<MagikTypedFile> {

  @Override
  public Optional<List<Location>> tryReferences(final ReferencesContext<MagikTypedFile> context) {
    final AstNode positionNode = context.positionNode();
    final AstNode wantedNode = ReferencesAncestor.nearest(positionNode);
    if (wantedNode == null || !wantedNode.is(MagikGrammar.EXEMPLAR_NAME)) {
      return Optional.empty();
    }

    final String identifier = positionNode.getTokenValue();
    final PackageNodeHelper packageHelper = new PackageNodeHelper(wantedNode);
    final String pakkage = packageHelper.getCurrentPackage();
    final TypeString typeString = TypeString.ofIdentifier(identifier, pakkage);

    final MagikTypedFile magikFile = context.file();
    final IDefinitionKeeper definitionKeeper = magikFile.getDefinitionKeeper();
    final List<Location> locations =
        TypeReferencesFinder.referencesToType(definitionKeeper, typeString);
    return Optional.of(locations);
  }
}
