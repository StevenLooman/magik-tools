package nl.ramsolutions.sw.magik.languageserver.munit;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import nl.ramsolutions.sw.definitions.SwModule;
import nl.ramsolutions.sw.definitions.SwModuleScanner;
import nl.ramsolutions.sw.definitions.SwProduct;
import nl.ramsolutions.sw.definitions.SwProductScanner;
import nl.ramsolutions.sw.magik.analysis.Location;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.Method;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import org.eclipse.lsp4j.ServerCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MUnit {@code TestItem} provider.
 */
public class MUnitTestItemProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(MUnitTestItemProvider.class);
    private static final Location DUMMY_LOCATION = new Location(URI.create("file:///"));
    private static final TypeString MUNIT_TEST_CASE_EXEMPLAR_NAME = TypeString.ofIdentifier("test_case", "sw");
    private static final String MUNIT_TEST_METHOD_PREFIX = "test";

    private final ITypeKeeper typeKeeper;

    public MUnitTestItemProvider(final ITypeKeeper typeKeeper) {
        this.typeKeeper = typeKeeper;
    }

    /**
     * Set server capabilities.
     * @param capabilities Server capabilities.
     */
    public void setCapabilities(final ServerCapabilities capabilities) {
        // No capabilities.
    }

    /**
     * Get {@Link TestItem}s.
     *
     * <p>
     * A hierarchy per product is provided:
     * - product
     * - module
     * - test exemplar
     * - test method
     * </p>
     * @return Test items.
     */
    public Collection<MUnitTestItem> getTestItems() {
        LOGGER.debug("Getting test items");

        final Map<SwProduct, MUnitTestItem> swProductTestItems = new HashMap<>();

        // Get all test methods of test cases, group by product,module,type
        this.getTestMethods().forEach(testMethod -> {
            try {
                this.createTestItems(testMethod, swProductTestItems);
            } catch (IOException exception) {
                LOGGER.error(exception.getMessage(), exception);
            }
        });

        return swProductTestItems.values();
    }

    private void createTestItems(
            final Method testMethod,
            final Map<SwProduct, MUnitTestItem> swProductTestItems) throws IOException {
        final Location location = testMethod.getLocation();
        if (location == null) {
            LOGGER.warn("Test method without location: {}", testMethod);
            return;
        }

        final URI uri = location.getUri();
        final Path path = Path.of(uri);

        // Get or create product TestItem.
        final SwProduct swProduct = this.getSwProduct(path);
        final MUnitTestItem swProductTestItem = swProductTestItems.computeIfAbsent(swProduct, this::createTestItem);

        // Get or create module TestItem.
        final SwModule swModule = this.getSwModule(path);
        final MUnitTestItem newSwModuleTestItem = this.createTestItem(swModule);
        final MUnitTestItem swModuleTestItem = swProductTestItem.addChild(newSwModuleTestItem);

        // Get or create exemplar TestItem.
        final AbstractType owner = testMethod.getOwner();
        final MUnitTestItem newTypeTestItem = this.createTestItem(owner);
        final MUnitTestItem typeTestItem = swModuleTestItem.addChild(newTypeTestItem);

        // Create method TestItem.
        final MUnitTestItem methodTestItem = this.createTestItem(testMethod);
        typeTestItem.addChild(methodTestItem);
    }

    private Stream<AbstractType> getTestCaseTypes() {
        final AbstractType testCaseType = this.typeKeeper.getType(MUNIT_TEST_CASE_EXEMPLAR_NAME);
        return this.typeKeeper.getTypes().stream()
            .filter(type -> type.isKindOf(testCaseType));
    }

    private Stream<Method> getTestMethods() {
        return this.getTestCaseTypes()
            .flatMap(type -> type.getLocalMethods().stream())
            .filter(method -> method.getName().toLowerCase().startsWith(MUNIT_TEST_METHOD_PREFIX));
    }

    private SwProduct getSwProduct(final Path path) throws IOException {
        final Path productDefPath = path.resolve(SwProduct.SW_PRODUCT_DEF);
        if (!Files.exists(productDefPath)) {
            final Path parentPath = path.getParent();
            if (parentPath == null) {
                return new SwProduct("<no_product>", null);
            }

            return this.getSwProduct(parentPath);
        }

        // Construct SwProduct.
        return SwProductScanner.readProductDefinition(productDefPath);
    }

    private SwModule getSwModule(final Path path) throws IOException {
        final Path moduleDefPath = path.resolve(SwModule.SW_MODULE_DEF);
        if (!Files.exists(moduleDefPath)) {
            final Path parentPath = path.getParent();
            if (parentPath == null) {
                return new SwModule("<no_module>", null);
            }

            return this.getSwModule(parentPath);
        }

        // Construct SwModule.
        return SwModuleScanner.readModuleDefinition(moduleDefPath);
    }

    private MUnitTestItem createTestItem(final SwProduct swProduct) {
        final String productName = swProduct.getName();
        return new MUnitTestItem("product:" + productName, productName, null);
    }

    private MUnitTestItem createTestItem(final SwModule swModule) {
        final String moduleName = swModule.getName();
        return new MUnitTestItem("module:" + moduleName, moduleName, null);
    }

    private MUnitTestItem createTestItem(final AbstractType type) {
        final String typeName = type.getFullName();
        return new MUnitTestItem("test_case:" + typeName, typeName, null);
    }

    private MUnitTestItem createTestItem(final Method method) {
        final String name = method.getName();
        final Location location = Objects.requireNonNullElse(method.getLocation(), DUMMY_LOCATION);
        return new MUnitTestItem("method:" + name, name, Lsp4jConversion.locationToLsp4j(location));
    }

}
