package nl.ramsolutions.sw.magik.languageserver.definitions;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import java.util.Optional;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.definitions.ConditionDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/** Provides definitions for a condition name. */
public class ConditionDefinitionModule implements DefinitionModule<MagikTypedFile> {

  @Override
  public Optional<List<Location>> tryDefinitions(final DefinitionContext<MagikTypedFile> context) {
    final AstNode positionNode = context.positionNode();
    final AstNode wantedNode = DefinitionAncestor.nearest(positionNode);
    if (wantedNode == null || !wantedNode.is(MagikGrammar.CONDITION_NAME)) {
      return Optional.empty();
    }

    final MagikTypedFile magikFile = context.file();
    final IDefinitionKeeper definitionKeeper = magikFile.getDefinitionKeeper();
    final String conditionName = positionNode.getTokenValue();
    final List<Location> locations =
        definitionKeeper.getConditionDefinitions(conditionName).stream()
            .map(ConditionDefinition::getLocation)
            .map(Location::validLocation)
            .toList();
    return Optional.of(locations);
  }
}
