package nl.ramsolutions.sw.magik.languageserver.references;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import java.util.Optional;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.SlotUsage;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Provides references to a slot. */
public class SlotReferencesModule implements ReferencesModule<MagikTypedFile> {

  private static final Logger LOGGER = LoggerFactory.getLogger(SlotReferencesModule.class);

  @Override
  public Optional<List<Location>> tryReferences(final ReferencesContext<MagikTypedFile> context) {
    final AstNode positionNode = context.positionNode();
    final AstNode wantedNode = ReferencesAncestor.nearest(positionNode);
    if (wantedNode == null || !wantedNode.is(MagikGrammar.SLOT)) {
      return Optional.empty();
    }

    final String slotName = positionNode.getTokenValue();
    LOGGER.debug("Getting references to slot: {}", slotName);

    final MagikTypedFile magikFile = context.file();
    final IDefinitionKeeper definitionKeeper = magikFile.getDefinitionKeeper();
    final List<Location> locations =
        definitionKeeper.getMethodDefinitions().stream()
            .flatMap(def -> def.getUsedSlots().stream())
            .filter(slotUsage -> slotUsage.getSlotName().equals(slotName))
            .map(SlotUsage::getLocation)
            .map(Location::validLocation)
            .toList();
    return Optional.of(locations);
  }
}
