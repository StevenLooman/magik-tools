package nl.ramsolutions.sw.checks.magiktyped;

import static nl.ramsolutions.sw.checks.magiktyped.MagikTypedCheckAssert.assertThat;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.checks.Issue;
import nl.ramsolutions.sw.checks.MagikCheck;
import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.checks.TestPaths;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.moduledef.ModuleDefFile;
import nl.ramsolutions.sw.moduledef.ModuleDefinition;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/** Tests for {@link ModuleRequiredForGlobalTypedCheck}. */
class ModuleRequiredForGlobalTypedCheckTest {

  private static final Path TEST_EXEMPLAR_PATH =
      Path.of(
          "magik-checks/src/test/resources/test_product/modules/test_module/source/test_exemplar.magik");

  private static final String CODE_USING_THE_GLOBAL =
      """
      _package user

      _method test_exemplar.init()
          _return sw:rope.new()
      _endmethod
      """;

  private void addModuleDefinitionToDefinitionKeeper(
      final Path path, final IDefinitionKeeper definitionKeeper) throws IOException {
    final Path fixedPath = TestPaths.createPath(path);
    final URI fixedUri = fixedPath.toUri();
    ModuleDefFile moduleDefFile = ModuleDefFile.getModuleDefFileForUri(fixedUri, definitionKeeper);
    definitionKeeper.add(moduleDefFile.getModuleDefinition());
  }

  private IDefinitionKeeper createKeeperWithGlobalInModule(final String moduleName)
      throws IOException {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper(true);
    this.addModuleDefinitionToDefinitionKeeper(TEST_EXEMPLAR_PATH, definitionKeeper);

    definitionKeeper.add(
        new ModuleDefinition(
            null,
            null,
            moduleName,
            null,
            null,
            null,
            null,
            Collections.emptyList(),
            Collections.emptyList()));
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            moduleName,
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            TypeString.ofIdentifier("rope", "sw"),
            null));
    return definitionKeeper;
  }

  private List<Issue> runCheck(final String code, final IDefinitionKeeper definitionKeeper) {
    final Path fixedPath = TestPaths.createPath(TEST_EXEMPLAR_PATH);
    final URI uri = fixedPath.toUri();
    final MagikTypedFile magikFile = new MagikTypedFile(uri, code, definitionKeeper);
    final MagikCheck check = new ModuleRequiredForGlobalTypedCheck();
    return check.scanFileForIssues(magikFile);
  }

  @Test
  void testModuleIsRequired() throws IllegalArgumentException, IOException {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper(true);

    final Path path =
        Path.of(
            "magik-checks/src/test/resources/test_product/modules/test_module/source/test_exemplar.magik");
    this.addModuleDefinitionToDefinitionKeeper(path, definitionKeeper);

    definitionKeeper.add(
        new ModuleDefinition(
            null,
            null,
            "super_test_module",
            null,
            null,
            null,
            null,
            Collections.emptyList(),
            Collections.emptyList()));
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            "super_test_module",
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            TypeString.ofIdentifier("rope", "sw"),
            null));
    definitionKeeper.addLoadedModuleNames(List.of("sw_core"));

    final MagikTypedCheck check = new ModuleRequiredForGlobalTypedCheck();
    assertThat(check).reportsNoIssues(path, definitionKeeper);
  }

  @Test
  void testModuleIsNotRequired() throws IllegalArgumentException, IOException {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper(true);
    final Path path =
        Path.of(
            "magik-checks/src/test/resources/test_product/modules/test_module/source/test_exemplar.magik");
    this.addModuleDefinitionToDefinitionKeeper(path, definitionKeeper);

    definitionKeeper.add(
        new ModuleDefinition(
            null,
            null,
            "another_module",
            null,
            null,
            null,
            null,
            Collections.emptyList(),
            Collections.emptyList()));
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            "another_module",
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            TypeString.ofIdentifier("rope", "sw"),
            null));
    definitionKeeper.addLoadedModuleNames(List.of("sw_core"));

    final MagikTypedCheck check = new ModuleRequiredForGlobalTypedCheck();
    assertThat(check).reportsIssueCount(path, definitionKeeper, 1);
  }

  @Test
  void noLoadedModulesRecordReportsNothing() throws IOException {
    final IDefinitionKeeper keeper = this.createKeeperWithGlobalInModule("other_module");
    final List<Issue> issues = this.runCheck(CODE_USING_THE_GLOBAL, keeper);
    Assertions.assertThat(issues).isEmpty();
  }

  @Test
  void moduleInTheLoadedRecordIsNotReported() throws IOException {
    final IDefinitionKeeper keeper = this.createKeeperWithGlobalInModule("other_module");
    keeper.addLoadedModuleNames(List.of("other_module"));
    final List<Issue> issues = this.runCheck(CODE_USING_THE_GLOBAL, keeper);
    Assertions.assertThat(issues).isEmpty();
  }

  @Test
  void moduleOutsideTheLoadedRecordIsReported() throws IOException {
    final IDefinitionKeeper keeper = this.createKeeperWithGlobalInModule("other_module");
    keeper.addLoadedModuleNames(List.of("sw_core"));
    final List<Issue> issues = this.runCheck(CODE_USING_THE_GLOBAL, keeper);
    Assertions.assertThat(issues).hasSize(1);
  }
}
