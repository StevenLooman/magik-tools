package nl.ramsolutions.sw.magik.languageserver.codelens;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.IgnoreHandler;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.magik.FileEvent;
import nl.ramsolutions.sw.magik.FileEvent.FileChangeType;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.InheritanceDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.indexer.MagikIndexer;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.languageserver.munit.MUnitTestItemProvider;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.Command;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests for {@link CodeLensProvider}. */
@SuppressWarnings("checkstyle:MagicNumber")
class CodeLensProviderTest {

  private static final Location IN_FILE_LOCATION =
      new Location(MagikTypedFile.DEFAULT_URI, new Range(new Position(1, 0), new Position(1, 0)));

  private static final Location OTHER_FILE_LOCATION =
      new Location(
          MagikTypedFile.DEFAULT_URI.resolve("other_file.magik"),
          new Range(new Position(1, 0), new Position(1, 0)));

  private List<CodeLens> provideCodeLenses(
      final String code, final IDefinitionKeeper definitionKeeper) {
    final MagikTypedFile magikFile =
        new MagikTypedFile(MagikTypedFile.DEFAULT_URI, code, definitionKeeper);
    final CodeLensProvider provider = new CodeLensProvider(definitionKeeper);
    return provider.provideCodeLenses(magikFile);
  }

  private IDefinitionKeeper buildKeeperWithTestCase() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

    // Add sw:test_case exemplar (the base MUnit test class).
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            MUnitTestItemProvider.MUNIT_TEST_CASE_EXEMPLAR_NAME,
            null));

    return definitionKeeper;
  }

  @Test
  void testCodeLensForTestExemplarAndMethod() {
    final IDefinitionKeeper definitionKeeper = this.buildKeeperWithTestCase();

    final TypeString myTestRef = TypeString.ofIdentifier("my_test", "user");

    // Add a test exemplar inheriting from sw:test_case.
    definitionKeeper.add(
        new ExemplarDefinition(
            IN_FILE_LOCATION,
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            myTestRef,
            null));
    definitionKeeper.add(
        new InheritanceDefinition(
            null,
            null,
            null,
            null,
            null,
            myTestRef,
            MUnitTestItemProvider.MUNIT_TEST_CASE_EXEMPLAR_NAME));

    // Add a test_* method on the test exemplar.
    definitionKeeper.add(
        new MethodDefinition(
            IN_FILE_LOCATION,
            null,
            null,
            null,
            null,
            myTestRef,
            "test_something()",
            Collections.emptySet(),
            Collections.emptyList(),
            null,
            null,
            ExpressionResultString.UNDEFINED,
            ExpressionResultString.EMPTY));

    final String code = "_method my_test.test_something()\n_endmethod\n";
    final List<CodeLens> lenses = this.provideCodeLenses(code, definitionKeeper);

    // Expect one lens for the exemplar, one for the test method.
    assertThat(lenses).hasSize(2);
    assertThat(lenses).anyMatch(l -> l.getCommand().getTitle().startsWith("Run all tests in"));
    assertThat(lenses).anyMatch(l -> l.getCommand().getTitle().equals("Run test"));
  }

  @Test
  void testNoCodeLensForNonTestMethod() {
    final IDefinitionKeeper definitionKeeper = this.buildKeeperWithTestCase();

    final TypeString myTestRef = TypeString.ofIdentifier("my_test", "user");

    definitionKeeper.add(
        new ExemplarDefinition(
            IN_FILE_LOCATION,
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            myTestRef,
            null));
    definitionKeeper.add(
        new InheritanceDefinition(
            null,
            null,
            null,
            null,
            null,
            myTestRef,
            MUnitTestItemProvider.MUNIT_TEST_CASE_EXEMPLAR_NAME));

    // Method without "test" prefix — should not get a code lens.
    definitionKeeper.add(
        new MethodDefinition(
            IN_FILE_LOCATION,
            null,
            null,
            null,
            null,
            myTestRef,
            "helper_method()",
            Collections.emptySet(),
            Collections.emptyList(),
            null,
            null,
            ExpressionResultString.UNDEFINED,
            ExpressionResultString.EMPTY));

    final String code = "_method my_test.helper_method()\n_endmethod\n";
    final List<CodeLens> lenses = this.provideCodeLenses(code, definitionKeeper);

    // Only the exemplar lens; no method lens for helper_method.
    assertThat(lenses).hasSize(1);
    assertThat(lenses.get(0).getCommand().getTitle()).startsWith("Run all tests in");
  }

  @Test
  void testNoCodeLensForMethodInOtherFile() {
    final IDefinitionKeeper definitionKeeper = this.buildKeeperWithTestCase();

    final TypeString myTestRef = TypeString.ofIdentifier("my_test", "user");

    definitionKeeper.add(
        new ExemplarDefinition(
            OTHER_FILE_LOCATION,
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            myTestRef,
            null));
    definitionKeeper.add(
        new InheritanceDefinition(
            OTHER_FILE_LOCATION,
            null,
            null,
            null,
            null,
            myTestRef,
            MUnitTestItemProvider.MUNIT_TEST_CASE_EXEMPLAR_NAME));

    definitionKeeper.add(
        new MethodDefinition(
            OTHER_FILE_LOCATION,
            null,
            null,
            null,
            null,
            myTestRef,
            "test_something()",
            Collections.emptySet(),
            Collections.emptyList(),
            null,
            null,
            ExpressionResultString.UNDEFINED,
            ExpressionResultString.EMPTY));

    // Current file is the DEFAULT_URI, but definitions are in OTHER_FILE_LOCATION.
    final String code = "";
    final List<CodeLens> lenses = this.provideCodeLenses(code, definitionKeeper);
    assertThat(lenses).isEmpty();
  }

  @Test
  void testNoCodeLensWhenNoTestCaseInKeeper() {
    // definitionKeeper has no sw:test_case — should return empty gracefully.
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final String code = "";
    final List<CodeLens> lenses = this.provideCodeLenses(code, definitionKeeper);
    assertThat(lenses).isEmpty();
  }

  @Test
  void testCodeLensesAfterReindexWithClientEncodedUri(@TempDir final Path tempDir)
      throws IOException {
    // Emacs lsp-mode percent-encodes a '+' (and on Windows the drive colon), Path.toUri() does not.
    final Path dirPath = tempDir.resolve("a+b");
    final Path dir = Files.createDirectory(dirPath);
    final Path file = dir.resolve("my_test.magik");
    final String source = CodeLensProviderTest.createTestSource("test_v0");
    Files.writeString(file, source);
    final URI fileUri = file.toUri();
    final String clientUriStr = fileUri.toString().replace("+", "%2B");
    final URI clientUri = URI.create(clientUriStr);

    final IDefinitionKeeper definitionKeeper = this.buildKeeperWithTestCase();
    final MagikIndexer indexer =
        new MagikIndexer(
            definitionKeeper, MagikToolsProperties.DEFAULT_PROPERTIES, new IgnoreHandler());
    final FileEvent fileEvent = new FileEvent(fileUri, FileChangeType.CREATED);
    indexer.handleFileEvent(fileEvent);
    final String code1 = CodeLensProviderTest.createTestSource("test_v1");
    indexer.indexFileContent(clientUri, code1);
    final String code2 = CodeLensProviderTest.createTestSource("test_v2");
    indexer.indexFileContent(clientUri, code2);

    final MagikTypedFile magikFile = new MagikTypedFile(clientUri, code2, definitionKeeper);
    final CodeLensProvider provider = new CodeLensProvider(definitionKeeper);
    final List<CodeLens> lenses = provider.provideCodeLenses(magikFile);

    final List<Object> arguments =
        lenses.stream()
            .map(CodeLens::getCommand)
            .map(Command::getArguments)
            .map(commandArguments -> commandArguments.get(0))
            .toList();
    assertThat(arguments)
        .containsExactlyInAnyOrder("test_case:user:my_test", "method:user:my_test.test_v2()");
  }

  private static String createTestSource(final String methodName) {
    return """
        def_slotted_exemplar(:my_test, {}, {:test_case})
        $
        _method my_test.%s()
        _endmethod
        $
        """
        .formatted(methodName);
  }
}
