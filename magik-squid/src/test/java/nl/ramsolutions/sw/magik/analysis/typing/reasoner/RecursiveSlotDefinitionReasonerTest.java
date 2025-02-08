package nl.ramsolutions.sw.magik.analysis.typing.reasoner;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.MagikAnalysisSettings;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.SlotDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import org.junit.jupiter.api.Test;

class RecursiveSlotDefinitionReasonerTest {

  /**
   * VSCode runs from module directory, mvn runs from project directory.
   *
   * @return Proper {@link Path} to file.
   */
  private Path getPath(final Path relativePath) {
    final Path path = Path.of(".").toAbsolutePath().getParent();
    if (path.endsWith("magik-squid")) {
      return Path.of("..").resolve(relativePath);
    }

    return Path.of(".").resolve(relativePath);
  }

  private void addFileToDefinitionKeeper(final IDefinitionKeeper definitionKeeper, final Path path)
      throws IOException {
    final URI uri = path.toUri();
    final String content = Files.readString(path, Charset.defaultCharset());
    final MagikToolsProperties properties =
        new MagikToolsProperties(
            Map.of(
                MagikAnalysisSettings.INDEX_GLOBAL_USAGES, "true",
                MagikAnalysisSettings.INDEX_METHOD_USAGES, "true",
                MagikAnalysisSettings.INDEX_SLOT_USAGES, "true",
                MagikAnalysisSettings.INDEX_CONDITION_USAGES, "true"));
    final MagikTypedFile magikTypedFile =
        new MagikTypedFile(properties, uri, content, definitionKeeper);
    magikTypedFile.getDefinitions().forEach(definitionKeeper::add);
  }

  @Test
  void testRecursiveSlotReasoningSlot1() throws IOException {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final Path path = Path.of("magik-squid/src/test/resources/test_recursive_reasoner.magik");
    final Path fixedPath = this.getPath(path);
    this.addFileToDefinitionKeeper(definitionKeeper, fixedPath);

    // Ensure typing isn't known currently.
    final TypeString typeStr = TypeString.ofIdentifier("test_exemplar", "user");
    final SlotDefinition slotDefinition =
        definitionKeeper.getExemplarDefinitions(typeStr).stream()
            .flatMap(def -> def.getSlots().stream())
            .filter(def -> def.getName().equals("slot1"))
            .findFirst()
            .orElseThrow();
    final TypeString slotTypeStr = slotDefinition.getTypeName();
    assertThat(slotTypeStr).isEqualTo(TypeString.UNDEFINED);

    // Recusively reason the slot definition.
    final RecursiveSlotDefinitionReasoner recursiveReasoner =
        new RecursiveSlotDefinitionReasoner(definitionKeeper, 3);
    recursiveReasoner.reason(slotDefinition);

    // Test if the slot definition now has a type.
    final SlotDefinition updatedSlotDefinition =
        definitionKeeper.getExemplarDefinitions(typeStr).stream()
            .flatMap(def -> def.getSlots().stream())
            .filter(def -> def.getName().equals("slot1"))
            .findFirst()
            .orElseThrow();
    final TypeString updatedSlotTypeStr = updatedSlotDefinition.getTypeName();
    assertThat(updatedSlotTypeStr)
        .isEqualTo(TypeString.combine(TypeString.SW_UNSET, TypeString.SW_INTEGER));
  }

  @Test
  void testRecursiveSlotReasoningSlot2() throws IOException {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final Path path = Path.of("magik-squid/src/test/resources/test_recursive_reasoner.magik");
    final Path fixedPath = this.getPath(path);
    this.addFileToDefinitionKeeper(definitionKeeper, fixedPath);

    // Ensure typing isn't known currently.
    final TypeString typeStr = TypeString.ofIdentifier("test_exemplar", "user");
    final SlotDefinition slotDefinition =
        definitionKeeper.getExemplarDefinitions(typeStr).stream()
            .flatMap(def -> def.getSlots().stream())
            .filter(def -> def.getName().equals("slot2"))
            .findFirst()
            .orElseThrow();
    final TypeString slotTypeStr = slotDefinition.getTypeName();
    assertThat(slotTypeStr).isEqualTo(TypeString.UNDEFINED);

    // Recusively reason the slot definition.
    final RecursiveSlotDefinitionReasoner recursiveReasoner =
        new RecursiveSlotDefinitionReasoner(definitionKeeper, 3);
    recursiveReasoner.reason(slotDefinition);

    // Test if the slot definition now has a type.
    final SlotDefinition updatedSlotDefinition =
        definitionKeeper.getExemplarDefinitions(typeStr).stream()
            .flatMap(def -> def.getSlots().stream())
            .filter(def -> def.getName().equals("slot2"))
            .findFirst()
            .orElseThrow();
    final TypeString updatedSlotTypeStr = updatedSlotDefinition.getTypeName();
    assertThat(updatedSlotTypeStr).isEqualTo(TypeString.SW_INTEGER);
  }
}
