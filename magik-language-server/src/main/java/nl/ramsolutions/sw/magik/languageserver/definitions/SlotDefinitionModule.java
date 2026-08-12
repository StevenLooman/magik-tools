package nl.ramsolutions.sw.magik.languageserver.definitions;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import java.util.Optional;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.definitions.SlotDefinition;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.SlotNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/** Provides definitions for a slot access. */
public class SlotDefinitionModule implements DefinitionModule<MagikTypedFile> {

  @Override
  public Optional<List<Location>> tryDefinitions(final DefinitionContext<MagikTypedFile> context) {
    final AstNode positionNode = context.positionNode();
    final AstNode slotNode = DefinitionAncestor.nearest(positionNode);
    if (slotNode == null || !slotNode.is(MagikGrammar.SLOT)) {
      return Optional.empty();
    }

    final String slotName = new SlotNodeHelper(slotNode).getSlotName();
    final AstNode methodDefNode = slotNode.getFirstAncestor(MagikGrammar.METHOD_DEFINITION);
    if (methodDefNode == null) {
      return Optional.of(List.of());
    }

    final MagikTypedFile magikFile = context.file();
    final TypeString ownerTypeStr =
        new MethodDefinitionNodeHelper(methodDefNode).getExemplarTypeString();
    final TypeStringResolver resolver = magikFile.getTypeStringResolver();
    final List<Location> locations =
        resolver.getSlotDefinitions(ownerTypeStr).stream()
            .filter(slot -> slot.getName().equals(slotName))
            .map(SlotDefinition::getLocation)
            .map(Location::validLocation)
            .toList();
    return Optional.of(locations);
  }
}
