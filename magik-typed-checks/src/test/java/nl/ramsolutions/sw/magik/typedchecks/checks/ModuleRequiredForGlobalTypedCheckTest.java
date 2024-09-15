package nl.ramsolutions.sw.magik.typedchecks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;
import nl.ramsolutions.sw.moduledef.ModuleDefFile;
import nl.ramsolutions.sw.moduledef.ModuleDefFileScanner;
import nl.ramsolutions.sw.moduledef.ModuleDefinition;
import org.junit.jupiter.api.Test;

/** Tests for {@link ModuleRequiredForGlobalTypedCheck}. */
class ModuleRequiredForGlobalTypedCheckTest extends MagikTypedCheckTestBase {

  /**
   * VSCode runs from module directory, mvn runs from project directory.
   *
   * @return Proper {@link Path} to file.
   */
  protected Path getPath(final String pathStr) {
    final Path path = Path.of(".").toAbsolutePath().getParent();
    if (path.endsWith("magik-squid")) {
      return Path.of("..").resolve(pathStr);
    }

    return Path.of(".").resolve(pathStr);
  }

  @Test
  void testModuleIsRequired() throws IllegalArgumentException, IOException {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper(true);
    final Path path =
        this.getPath(
            "src/test/resources/test_product/modules/test_module/source/test_exemplar.magik");
    final Path moduleDefPath = ModuleDefFileScanner.getModuleDefFileForPath(path);
    final ModuleDefFile moduleDefFile = new ModuleDefFile(moduleDefPath, definitionKeeper, null);
    final ModuleDefinition moduleDefinition = moduleDefFile.getModuleDefinition();
    definitionKeeper.add(moduleDefinition);

    definitionKeeper.add(
        new ModuleDefinition(
            null, null, "super_test_module", null, null, null, null, Collections.emptyList()));
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            "super_test_module",
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            TypeString.ofIdentifier("rope", "sw"),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptySet()));

    final MagikTypedCheck check = new ModuleRequiredForGlobalTypedCheck();
    final List<MagikIssue> checkResults = this.runCheck(path, definitionKeeper, check);
    assertThat(checkResults).isEmpty();
  }

  @Test
  void testModuleIsNotRequired() throws IllegalArgumentException, IOException {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper(true);
    final Path path =
        this.getPath(
            "src/test/resources/test_product/modules/test_module/source/test_exemplar.magik");
    final Path moduleDefPath = ModuleDefFileScanner.getModuleDefFileForPath(path);
    final ModuleDefFile moduleDefFile = new ModuleDefFile(moduleDefPath, definitionKeeper, null);
    final ModuleDefinition moduleDefinition = moduleDefFile.getModuleDefinition();
    definitionKeeper.add(moduleDefinition);

    definitionKeeper.add(
        new ModuleDefinition(
            null, null, "another_module", null, null, null, null, Collections.emptyList()));
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            "another_module",
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            TypeString.ofIdentifier("rope", "sw"),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptySet()));

    final MagikTypedCheck check = new ModuleRequiredForGlobalTypedCheck();
    final List<MagikIssue> checkResults = this.runCheck(path, definitionKeeper, check);
    assertThat(checkResults).hasSize(1);
  }
}
