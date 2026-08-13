package nl.ramsolutions.sw.magik.languageserver.references;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.loadlist.LoadListFile;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.analysis.definitions.ConditionUsage;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalUsage;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodUsage;
import nl.ramsolutions.sw.magik.analysis.definitions.SlotUsage;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.moduledef.ModuleDefFile;
import nl.ramsolutions.sw.moduledef.ModuleDefinition;
import nl.ramsolutions.sw.moduledef.ModuleUsage;
import nl.ramsolutions.sw.productdef.ProductDefFile;
import nl.ramsolutions.sw.productdef.ProductDefinition;
import nl.ramsolutions.sw.productdef.ProductUsage;
import org.junit.jupiter.api.Test;

/** Test ReferencesProvider. */
@SuppressWarnings("checkstyle:MagicNumber")
class ReferencesProviderTest {

  private static final Location EMPTY_LOCATION = Location.validLocation(null);

  private List<Location> getReferences(
      final String code, final Position position, final IDefinitionKeeper definitionKeeper) {
    final URI uri = EMPTY_LOCATION.getUri();
    final MagikTypedFile magikFile = new MagikTypedFile(uri, code, definitionKeeper);
    final ReferencesProvider provider = new ReferencesProvider();
    return provider.provideReferences(magikFile, position);
  }

  private List<Location> getProductDefReferences(
      final String code, final Position position, final IDefinitionKeeper definitionKeeper) {
    final ProductDefFile productDefFile =
        new ProductDefFile(ProductDefFile.DEFAULT_URI, code, definitionKeeper, null);
    final ReferencesProvider provider = new ReferencesProvider();
    return provider.provideReferences(productDefFile, position);
  }

  private List<Location> getModuleDefReferences(
      final String code, final Position position, final IDefinitionKeeper definitionKeeper) {
    final ModuleDefFile moduleDefFile =
        new ModuleDefFile(ModuleDefFile.DEFAULT_URI, code, definitionKeeper, null);
    final ReferencesProvider provider = new ReferencesProvider();
    return provider.provideReferences(moduleDefFile, position);
  }

  private List<Location> getLoadListReferences(final String code, final Position position) {
    final LoadListFile loadListFile = new LoadListFile(LoadListFile.DEFAULT_URI, code);
    final ReferencesProvider provider = new ReferencesProvider();
    return provider.provideReferences(loadListFile, position);
  }

  @Test
  void testProvideMethodReferenceFromMethodInvocation() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new MethodDefinition(
            EMPTY_LOCATION,
            null,
            null,
            null,
            null,
            TypeString.SW_INTEGER,
            "referring",
            Collections.emptySet(),
            Collections.emptyList(),
            null,
            null,
            ExpressionResultString.UNDEFINED,
            ExpressionResultString.EMPTY,
            Collections.emptyList(),
            List.of(new MethodUsage(TypeString.UNDEFINED, "referring", EMPTY_LOCATION, null)),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()));

    final String code =
        """
        _method integer.referring
            _self.referring
        _endmethod
        """;
    final Position position = new Position(2, 12); // On `referring`.
    final List<Location> references = this.getReferences(code, position, definitionKeeper);
    assertThat(references).hasSize(1);
  }

  @Test
  void testProvideMethodReferenceFromMethodDefinition() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new MethodDefinition(
            EMPTY_LOCATION,
            null,
            null,
            null,
            null,
            TypeString.SW_INTEGER,
            "referring",
            Collections.emptySet(),
            Collections.emptyList(),
            null,
            null,
            ExpressionResultString.UNDEFINED,
            ExpressionResultString.EMPTY,
            Collections.emptyList(),
            List.of(new MethodUsage(TypeString.UNDEFINED, "referring", EMPTY_LOCATION, null)),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()));

    final String code =
        """
        _method integer.referring
            _self.referring
        _endmethod
        """;
    final Position position = new Position(1, 20); // On `referring`.
    final List<Location> references = this.getReferences(code, position, definitionKeeper);
    assertThat(references).hasSize(1);
  }

  @Test
  void testProvideTypeReferenceFromAtom() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new MethodDefinition(
            EMPTY_LOCATION,
            null,
            null,
            null,
            null,
            TypeString.SW_INTEGER,
            "referring",
            Collections.emptySet(),
            Collections.emptyList(),
            null,
            null,
            ExpressionResultString.UNDEFINED,
            ExpressionResultString.EMPTY,
            List.of(new GlobalUsage(TypeString.SW_INTEGER, EMPTY_LOCATION, null)),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()));

    final String code =
        """
        _method integer.referring
            integer
        _endmethod
        """;
    final Position position = new Position(2, 4); // On `integer`.
    final List<Location> references = this.getReferences(code, position, definitionKeeper);
    assertThat(references).hasSize(1);
  }

  @Test
  void testProvideTypeReferenceFromMethodDefinition() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new MethodDefinition(
            EMPTY_LOCATION,
            null,
            null,
            null,
            null,
            TypeString.SW_INTEGER,
            "referring",
            Collections.emptySet(),
            Collections.emptyList(),
            null,
            null,
            ExpressionResultString.UNDEFINED,
            ExpressionResultString.EMPTY,
            List.of(new GlobalUsage(TypeString.SW_INTEGER, EMPTY_LOCATION, null)),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()));

    final String code =
        """
        _method integer.referring
            print(integer)
        _endmethod
        """;
    final Position position = new Position(1, 10); // On `integer`.
    final List<Location> references = this.getReferences(code, position, definitionKeeper);
    assertThat(references).hasSize(1);
  }

  @Test
  void testProvideReferencesToLocal() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

    final String code =
        """
        _method object.method
            _local local1 << 10
            _return local1
        _endmethod
        """;
    final Position position = new Position(3, 12); // On `local1`.
    final List<Location> references = this.getReferences(code, position, definitionKeeper);
    assertThat(references).hasSize(1);
  }

  @Test
  void testProvideReferencesToCondition() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new MethodDefinition(
            EMPTY_LOCATION,
            null,
            null,
            null,
            null,
            TypeString.SW_INTEGER,
            "referring",
            Collections.emptySet(),
            Collections.emptyList(),
            null,
            null,
            ExpressionResultString.UNDEFINED,
            ExpressionResultString.EMPTY,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            List.of(new ConditionUsage("error", EMPTY_LOCATION, null))));

    final String code =
        """
        _method object.method
            _try
            _when error
            _endtry
        _endmethod
        """;
    final Position position = new Position(3, 12); // On `error`.
    final List<Location> references = this.getReferences(code, position, definitionKeeper);
    assertThat(references).hasSize(1);
  }

  @Test
  void testProvideReferencesToSlot() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new MethodDefinition(
            EMPTY_LOCATION,
            null,
            null,
            null,
            null,
            TypeString.SW_INTEGER,
            "referring",
            Collections.emptySet(),
            Collections.emptyList(),
            null,
            null,
            ExpressionResultString.UNDEFINED,
            ExpressionResultString.EMPTY,
            Collections.emptyList(),
            Collections.emptyList(),
            List.of(new SlotUsage(TypeString.UNDEFINED, "my_slot", EMPTY_LOCATION, null)),
            Collections.emptyList()));

    final String code =
        """
        _method my_exemplar.method
            _return .my_slot
        _endmethod
        """;
    final Position position = new Position(2, 13); // On `my_slot`.
    final List<Location> references = this.getReferences(code, position, definitionKeeper);
    assertThat(references).hasSize(1);
  }

  @Test
  void testProvideReferencesToProductName() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new ProductDefinition(
            EMPTY_LOCATION,
            null,
            "consumer_product",
            null,
            "1",
            null,
            null,
            null,
            List.of(new ProductUsage("my_product", EMPTY_LOCATION))));

    final String code = "my_product layered_product\n";
    final Position position = new Position(1, 4); // On `my_product`.
    final List<Location> references =
        this.getProductDefReferences(code, position, definitionKeeper);
    assertThat(references).hasSize(1);
  }

  @Test
  void testProvideReferencesToModuleName() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new ModuleDefinition(
            EMPTY_LOCATION,
            null,
            "consumer_module",
            null,
            "1",
            "2",
            null,
            List.of(new ModuleUsage("my_module", EMPTY_LOCATION)),
            Collections.emptyList()));

    final String code = "my_module 1\n";
    final Position position = new Position(1, 4); // On `my_module`.
    final List<Location> references = this.getModuleDefReferences(code, position, definitionKeeper);
    assertThat(references).hasSize(1);
  }

  @Test
  void testProvideReferencesLoadListEmpty() {
    final String code = "source/my_class\n";
    final Position position = new Position(1, 4);
    final List<Location> references = this.getLoadListReferences(code, position);
    assertThat(references).isEmpty();
  }
}
