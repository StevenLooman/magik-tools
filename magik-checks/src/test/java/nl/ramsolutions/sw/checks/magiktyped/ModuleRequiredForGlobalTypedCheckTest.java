package nl.ramsolutions.sw.checks.magiktyped;

import static nl.ramsolutions.sw.checks.magiktyped.MagikTypedCheckAssert.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import nl.ramsolutions.sw.SourceFileScanner;
import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.moduledef.ModuleDefFile;
import nl.ramsolutions.sw.moduledef.ModuleDefinition;
import org.junit.jupiter.api.Test;

/** Tests for {@link ModuleRequiredForGlobalTypedCheck}. */
class ModuleRequiredForGlobalTypedCheckTest {

  /**
   * VSCode runs from module directory, mvn runs from project directory.
   *
   * @return Proper {@link Path} to file.
   */
  protected Path getPath(final Path path) {
    final Path parentPath = Path.of(".").toAbsolutePath().getParent();
    return parentPath.endsWith("magik-checks")
        ? Path.of("..").resolve(path)
        : Path.of(".").resolve(path);
  }

  @Test
  void testModuleIsRequired() throws IllegalArgumentException, IOException {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper(true);

    final Path path =
        Path.of(
            "magik-checks/src/test/resources/test_product/modules/test_module/source/test_exemplar.magik");
    final Path fixedPath = this.getPath(path);
    final Path moduleDefPath =
        SourceFileScanner.searchFileUpwards(fixedPath, SourceFileScanner.SW_MODULE_DEF);
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
            null));

    final MagikTypedCheck check = new ModuleRequiredForGlobalTypedCheck();
    assertThat(check).reportsNoIssues(path, definitionKeeper);
  }

  @Test
  void testModuleIsNotRequired() throws IllegalArgumentException, IOException {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper(true);
    final Path path =
        Path.of(
            "magik-checks/src/test/resources/test_product/modules/test_module/source/test_exemplar.magik");
    final Path fixedPath = this.getPath(path);
    final Path moduleDefPath =
        SourceFileScanner.searchFileUpwards(fixedPath, SourceFileScanner.SW_MODULE_DEF);
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
            null));

    final MagikTypedCheck check = new ModuleRequiredForGlobalTypedCheck();
    assertThat(check).reportsIssueCount(path, definitionKeeper, 1);
  }
}
