package nl.ramsolutions.sw.magik.languageserver.references;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import java.util.Optional;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.definitions.ConditionUsage;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Provides references to a condition name. */
public class ConditionNameReferencesModule implements ReferencesModule<MagikTypedFile> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConditionNameReferencesModule.class);

  @Override
  public Optional<List<Location>> tryReferences(final ReferencesContext<MagikTypedFile> context) {
    final AstNode positionNode = context.positionNode();
    final AstNode wantedNode = ReferencesAncestor.nearest(positionNode);
    if (wantedNode == null || !wantedNode.is(MagikGrammar.CONDITION_NAME)) {
      return Optional.empty();
    }

    final String conditionName = positionNode.getTokenValue();
    LOGGER.debug("Getting references to condition: {}", conditionName);

    final MagikTypedFile magikFile = context.file();
    final IDefinitionKeeper definitionKeeper = magikFile.getDefinitionKeeper();
    final List<Location> locations =
        definitionKeeper.getMethodDefinitions().stream()
            .flatMap(def -> def.getUsedConditions().stream())
            .filter(conditionUsage -> conditionUsage.getConditionName().equals(conditionName))
            .map(ConditionUsage::getLocation)
            .map(Location::validLocation)
            .toList();
    return Optional.of(locations);
  }
}
