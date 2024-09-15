package nl.ramsolutions.sw.magik.languageserver.munit;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import nl.ramsolutions.sw.moduledef.ModuleDefFile;
import nl.ramsolutions.sw.moduledef.ModuleDefFileScanner;
import nl.ramsolutions.sw.moduledef.ModuleDefinition;
import nl.ramsolutions.sw.productdef.ProductDefFile;
import nl.ramsolutions.sw.productdef.ProductDefFileScanner;
import nl.ramsolutions.sw.productdef.ProductDefinition;
import org.eclipse.lsp4j.ServerCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** MUnit {@code TestItem} provider. */
public class MUnitTestItemProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(MUnitTestItemProvider.class);
  private static final TypeString MUNIT_TEST_CASE_EXEMPLAR_NAME =
      TypeString.ofIdentifier("test_case", "sw");
  private static final String MUNIT_TEST_METHOD_PREFIX = "test";

  private final IDefinitionKeeper definitionKeeper;

  public MUnitTestItemProvider(final IDefinitionKeeper definitionKeeper) {
    this.definitionKeeper = definitionKeeper;
  }

  /**
   * Set server capabilities.
   *
   * @param capabilities Server capabilities.
   */
  public void setCapabilities(final ServerCapabilities capabilities) {
    // No capabilities.
  }

  /**
   * Get {@Link TestItem}s.
   *
   * <p>A hierarchy per product is provided: - product - module - test exemplar - test method
   *
   * @return Test items.
   */
  public Collection<MUnitTestItem> getTestItems() {
    LOGGER.debug("Getting test items");

    final Map<ProductDefinition, MUnitTestItem> swProductTestItems = new HashMap<>();

    // Get all test methods of test cases, group by product,module,type
    this.getTestMethods()
        .forEach(
            definition -> {
              try {
                this.createTestItems(definition, swProductTestItems);
              } catch (IOException exception) {
                LOGGER.error(exception.getMessage(), exception);
              }
            });

    return swProductTestItems.values();
  }

  private void createTestItems(
      final MethodDefinition methodDefinition,
      final Map<ProductDefinition, MUnitTestItem> swProductTestItems)
      throws IOException {
    final Location location = methodDefinition.getLocation();
    if (location == null) {
      LOGGER.warn("Test method without location: {}", methodDefinition);
      return;
    }

    final TypeString ownerRef = methodDefinition.getTypeName();
    final ExemplarDefinition exemplar =
        this.definitionKeeper.getExemplarDefinitions(ownerRef).stream().findAny().orElse(null);
    if (exemplar == null) {
      LOGGER.warn("Test method without exemplar: {}", methodDefinition);
      return;
    }

    final URI uri = location.getUri();
    final Path path = Path.of(uri);

    // Get or create product TestItem.
    final ProductDefinition swProduct = this.getSwProduct(path);
    final MUnitTestItem swProductTestItem =
        swProductTestItems.computeIfAbsent(swProduct, this::createTestItem);

    // Get or create module TestItem.
    final ModuleDefinition swModule = this.getSwModule(path);
    final MUnitTestItem newSwModuleTestItem = this.createTestItem(swModule);
    final MUnitTestItem swModuleTestItem = swProductTestItem.addChild(newSwModuleTestItem);

    // Get or create exemplar TestItem.
    final MUnitTestItem newExemplarTestItem = this.createTestItem(exemplar);
    final MUnitTestItem typeTestItem = swModuleTestItem.addChild(newExemplarTestItem);

    // Create method TestItem.
    final MUnitTestItem methodTestItem = this.createTestItem(methodDefinition);
    typeTestItem.addChild(methodTestItem);
  }

  private Stream<ExemplarDefinition> getTestCaseExemplars() {
    final ExemplarDefinition testCaseDefinition =
        this.definitionKeeper.getExemplarDefinitions(MUNIT_TEST_CASE_EXEMPLAR_NAME).stream()
            .findAny()
            .orElse(null);
    if (testCaseDefinition == null) {
      return Stream.of();
    }

    final TypeStringResolver resolver = new TypeStringResolver(this.definitionKeeper);
    return this.definitionKeeper.getExemplarDefinitions().stream()
        .filter(definition -> resolver.isKindOf(definition, testCaseDefinition));
  }

  private Stream<MethodDefinition> getTestMethods() {
    return this.getTestCaseExemplars()
        .flatMap(
            testExemplarDefinition ->
                this.definitionKeeper
                    .getMethodDefinitions(testExemplarDefinition.getTypeString())
                    .stream())
        .filter(
            methodDefinition ->
                methodDefinition
                    .getMethodName()
                    .toLowerCase()
                    .startsWith(MUNIT_TEST_METHOD_PREFIX));
  }

  private ProductDefinition getSwProduct(final Path path) throws IOException {
    final Path productDefPath = path.resolve(ProductDefFileScanner.SW_PRODUCT_DEF);
    if (!Files.exists(productDefPath)) {
      final Path parentPath = path.getParent();
      if (parentPath == null) {
        return new ProductDefinition(
            null, null, "<no_product>", null, "", null, null, null, Collections.emptyList());
      }

      return this.getSwProduct(parentPath);
    }

    // Construct SwProduct.
    final ProductDefFile productDefFile =
        new ProductDefFile(productDefPath, this.definitionKeeper, null);
    return productDefFile.getProductDefinition();
  }

  private ModuleDefinition getSwModule(final Path path) throws IOException {
    final Path moduleDefPath = path.resolve(ModuleDefFileScanner.SW_MODULE_DEF);
    if (!Files.exists(moduleDefPath)) {
      final Path parentPath = path.getParent();
      if (parentPath == null) {
        return new ModuleDefinition(
            null, null, "<no_module>", null, "", "1", null, Collections.emptyList());
      }

      return this.getSwModule(parentPath);
    }

    // Construct SwModule.
    final ModuleDefFile moduleDefFile = new ModuleDefFile(moduleDefPath, definitionKeeper, null);
    return moduleDefFile.getModuleDefinition();
  }

  private MUnitTestItem createTestItem(final ProductDefinition definition) {
    final String productName = definition.getName();
    final Location definitionLocation = definition.getLocation();
    final Location location = Location.validLocation(definitionLocation);
    return new MUnitTestItem(
        "product:" + productName, productName, Lsp4jConversion.locationToLsp4j(location));
  }

  private MUnitTestItem createTestItem(final ModuleDefinition definition) {
    final String moduleName = definition.getName();
    final Location definitionLocation = definition.getLocation();
    final Location location = Location.validLocation(definitionLocation);
    return new MUnitTestItem(
        "module:" + moduleName, moduleName, Lsp4jConversion.locationToLsp4j(location));
  }

  private MUnitTestItem createTestItem(final ExemplarDefinition definition) {
    final String typeName = definition.getTypeString().getFullString();
    final Location definitionLocation = definition.getLocation();
    final Location location = Location.validLocation(definitionLocation);
    return new MUnitTestItem(
        "test_case:" + typeName, typeName, Lsp4jConversion.locationToLsp4j(location));
  }

  private MUnitTestItem createTestItem(final MethodDefinition definition) {
    final String methodName = definition.getMethodName();
    final Location definitionLocation = definition.getLocation();
    final Location location = Location.validLocation(definitionLocation);
    return new MUnitTestItem(
        "method:" + methodName, methodName, Lsp4jConversion.locationToLsp4j(location));
  }
}
