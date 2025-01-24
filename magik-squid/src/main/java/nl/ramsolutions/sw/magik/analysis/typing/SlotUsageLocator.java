package nl.ramsolutions.sw.magik.analysis.typing;

import com.sonar.sslr.api.AstNode;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import nl.ramsolutions.sw.FileCharsetDeterminer;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.SlotUsage;
import nl.ramsolutions.sw.magik.analysis.helpers.SlotNodeHelper;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/**
 * Slot usage locator.
 *
 * <p>Finds all {@link SlotUsage}s for a given slot.
 */
public class SlotUsageLocator {

  private final IDefinitionKeeper definitionKeeper;

  public SlotUsageLocator(final IDefinitionKeeper definitionKeeper) {
    this.definitionKeeper = definitionKeeper;
  }

  /**
   * Get the slot usages for the wanted slot usage.
   *
   * @param wantedSlotUsage The wanted {@link SlotUsage}.
   * @return The {@link SlotUsage}s for the wanted slot usage in all indexed {@link
   *     MethodDefinition}s.
   */
  public List<Map.Entry<SlotUsage, MagikTypedFile>> getSlotUsages(final SlotUsage wantedSlotUsage) {
    final String slotName = wantedSlotUsage.getSlotName();
    return this.definitionKeeper.getMethodDefinitions().stream()
        .flatMap(methodDef -> methodDef.getUsedSlots().stream())
        .filter(usage -> usage.equals(wantedSlotUsage))
        .map(
            usage -> {
              final Location location = usage.getLocation();
              final MagikTypedFile magikFile = this.getMagikFile(location);

              final AstNode node = magikFile.getTopNode();
              final Position slotPosition = location.getRange().getStartPosition();
              final AstNode slotTokenNode = AstQuery.nodeAt(node, slotPosition);
              final AstNode slotNode = slotTokenNode.getFirstAncestor(MagikGrammar.SLOT);
              final SlotNodeHelper helper = new SlotNodeHelper(slotNode);

              // TODO: Actually, ownerTypeString should be resolved to exemplar with the slot.
              final TypeString ownerTypeString = helper.getMethodExemplarTypeString();
              final SlotUsage slotUsage =
                  new SlotUsage(ownerTypeString, slotName, location, slotNode);
              return Map.entry(slotUsage, magikFile);
            })
        .toList();
  }

  private MagikTypedFile getMagikFile(final Location location) {
    final URI uri = location.getUri();
    final Path path = Path.of(uri);
    final Charset charset = FileCharsetDeterminer.determineCharset(path);
    final String text;
    try {
      text = Files.readString(path, charset);
    } catch (final IOException exception) {
      throw new IllegalStateException(exception);
    }
    return new MagikTypedFile(uri, text, this.definitionKeeper);
  }
}
