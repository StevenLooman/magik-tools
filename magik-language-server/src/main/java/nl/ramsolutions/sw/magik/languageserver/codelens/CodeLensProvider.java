package nl.ramsolutions.sw.magik.languageserver.codelens;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import nl.ramsolutions.sw.magik.languageserver.munit.MUnitTestItemProvider;
import nl.ramsolutions.sw.moduledef.ModuleDefFile;
import nl.ramsolutions.sw.moduledef.ModuleDefinition;
import nl.ramsolutions.sw.productdef.ProductDefFile;
import nl.ramsolutions.sw.productdef.ProductDefinition;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CodeLensOptions;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.ServerCapabilities;

/** Code lens provider. */
public class CodeLensProvider {

  private static final String MUNIT_RUN_TEST_COMMAND = "magik.munit.runTest";
  private static final String ADD_PRODUCT_COMMAND = "magik.session.addProduct";
  private static final String LOAD_MODULE_COMMAND = "magik.session.loadModule";

  private final MUnitTestItemProvider munitTestItemProvider;

  /**
   * Constructor.
   *
   * @param definitionKeeper Definition keeper.
   */
  public CodeLensProvider(final IDefinitionKeeper definitionKeeper) {
    this.munitTestItemProvider = new MUnitTestItemProvider(definitionKeeper);
  }

  /**
   * Set server capabilities.
   *
   * @param capabilities Server capabilities.
   */
  public void setCapabilities(final ServerCapabilities capabilities) {
    capabilities.setCodeLensProvider(new CodeLensOptions());
  }

  /**
   * Provide code lenses for MUnit tests in the given file.
   *
   * @param magikFile Magik file.
   * @return List of {@link CodeLens}es.
   */
  public List<CodeLens> provideCodeLenses(final MagikTypedFile magikFile) {
    final URI fileUri = magikFile.getUri();
    final List<CodeLens> codeLenses = new ArrayList<>();

    this.munitTestItemProvider
        .getTestCaseExemplars()
        .filter(exemplar -> this.isInFile(exemplar.getLocation(), fileUri))
        .forEach(exemplar -> codeLenses.add(this.createExemplarCodeLens(exemplar)));

    this.munitTestItemProvider
        .getTestMethods()
        .filter(method -> this.isInFile(method.getLocation(), fileUri))
        .forEach(method -> codeLenses.add(this.createMethodCodeLens(method)));

    return codeLenses;
  }

  private boolean isInFile(final Location location, final URI fileUri) {
    if (location == null) {
      return false;
    }
    return Objects.equals(location.getUri(), fileUri);
  }

  private CodeLens createExemplarCodeLens(final ExemplarDefinition exemplar) {
    final String typeFullString = exemplar.getTypeString().getFullString();
    final Command command =
        new Command(
            "Run all tests in " + exemplar.getTypeString().getIdentifier(),
            MUNIT_RUN_TEST_COMMAND,
            List.of("test_case:" + typeFullString));
    final org.eclipse.lsp4j.Range range = this.locationToRange(exemplar.getLocation());
    return new CodeLens(range, command, null);
  }

  private CodeLens createMethodCodeLens(final MethodDefinition method) {
    final String typeName = method.getTypeName().getFullString();
    final Command command =
        new Command(
            "Run test",
            MUNIT_RUN_TEST_COMMAND,
            List.of("method:" + typeName + "." + method.getMethodName()));
    final org.eclipse.lsp4j.Range range = this.locationToRange(method.getLocation());
    return new CodeLens(range, command, null);
  }

  /**
   * Provide code lenses for a product.def file.
   *
   * @param productDefFile Product definition file.
   * @return List of {@link CodeLens}es.
   */
  public List<CodeLens> provideProductDefCodeLenses(final ProductDefFile productDefFile) {
    final ProductDefinition productDefinition = productDefFile.getProductDefinition();
    if (productDefinition == null) {
      return List.of();
    }

    final Path productDir = Path.of(productDefFile.getUri()).getParent();
    final Command command =
        new Command(
            "Add product " + productDefinition.getName(),
            ADD_PRODUCT_COMMAND,
            List.of(productDir.toString()));
    final org.eclipse.lsp4j.Range range = this.firstLineRange();
    return List.of(new CodeLens(range, command, null));
  }

  /**
   * Provide code lenses for a module.def file.
   *
   * @param moduleDefFile Module definition file.
   * @return List of {@link CodeLens}es.
   */
  public List<CodeLens> provideModuleDefCodeLenses(final ModuleDefFile moduleDefFile) {
    final ModuleDefinition moduleDefinition = moduleDefFile.getModuleDefinition();
    if (moduleDefinition == null) {
      return List.of();
    }

    final String moduleName = moduleDefinition.getName();
    final Command command =
        new Command("Load module " + moduleName, LOAD_MODULE_COMMAND, List.of(moduleName));
    final org.eclipse.lsp4j.Range range = this.firstLineRange();
    return List.of(new CodeLens(range, command, null));
  }

  private org.eclipse.lsp4j.Range firstLineRange() {
    return Lsp4jConversion.rangeToLsp4j(
        new Range(
            new nl.ramsolutions.sw.magik.Position(1, 0),
            new nl.ramsolutions.sw.magik.Position(1, 0)));
  }

  private org.eclipse.lsp4j.Range locationToRange(final Location location) {
    final Range range =
        location != null && location.getRange() != null
            ? location.getRange()
            : new Range(
                new nl.ramsolutions.sw.magik.Position(1, 0),
                new nl.ramsolutions.sw.magik.Position(1, 0));
    return Lsp4jConversion.rangeToLsp4j(range);
  }
}
