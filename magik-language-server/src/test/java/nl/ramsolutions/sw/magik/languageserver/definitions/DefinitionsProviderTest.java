package nl.ramsolutions.sw.magik.languageserver.definitions;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.loadlist.LoadListFile;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.analysis.definitions.ConditionDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.SlotDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.moduledef.ModuleDefFile;
import nl.ramsolutions.sw.moduledef.ModuleDefinition;
import nl.ramsolutions.sw.productdef.ProductDefFile;
import nl.ramsolutions.sw.productdef.ProductDefinition;
import org.junit.jupiter.api.Test;

/** Test DefinitionsProvider. */
@SuppressWarnings("checkstyle:MagicNumber")
class DefinitionsProviderTest {

  private static final Location EMPTY_LOCATION =
      new Location(MagikTypedFile.DEFAULT_URI, new Range(new Position(0, 0), new Position(0, 0)));

  private List<Location> getDefinitions(
      final String code, final Position position, final IDefinitionKeeper definitionKeeper) {
    final MagikTypedFile magikFile =
        new MagikTypedFile(MagikTypedFile.DEFAULT_URI, code, definitionKeeper);
    final DefinitionsProvider provider = new DefinitionsProvider();
    return provider.provideDefinitions(magikFile, position);
  }

  private List<Location> getProductDefDefinitions(
      final String code, final Position position, final IDefinitionKeeper definitionKeeper) {
    final ProductDefFile productDefFile =
        new ProductDefFile(ProductDefFile.DEFAULT_URI, code, definitionKeeper, null);
    final DefinitionsProvider provider = new DefinitionsProvider();
    return provider.provideDefinitions(productDefFile, position);
  }

  private List<Location> getModuleDefDefinitions(
      final String code, final Position position, final IDefinitionKeeper definitionKeeper) {
    final ModuleDefFile moduleDefFile =
        new ModuleDefFile(ModuleDefFile.DEFAULT_URI, code, definitionKeeper, null);
    final DefinitionsProvider provider = new DefinitionsProvider();
    return provider.provideDefinitions(moduleDefFile, position);
  }

  private List<Location> getLoadListDefinitions(final String code, final Position position) {
    final LoadListFile loadListFile = new LoadListFile(LoadListFile.DEFAULT_URI, code);
    final DefinitionsProvider provider = new DefinitionsProvider();
    return provider.provideDefinitions(loadListFile, position);
  }

  @Test
  void testProvideDefinitionsFromGlobal() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString ropeRef = TypeString.ofIdentifier("rope", "sw");
    definitionKeeper.add(
        new ExemplarDefinition(
            EMPTY_LOCATION,
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            ropeRef,
            null));

    final String code =
        """
        _method object.method
            _return rope.new()
        _endmethod
        """;
    final Position position = new Position(2, 14); // On `rope`.
    final List<Location> locations = this.getDefinitions(code, position, definitionKeeper);
    assertThat(locations).containsExactly(EMPTY_LOCATION);
  }

  @Test
  void testProvideDefinitionsFromParameter() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final String code =
        """
        _method object.method(param1)
            _return param1
        _endmethod
        """;
    final Position position = new Position(2, 14); // On `param1`.
    final List<Location> locations = this.getDefinitions(code, position, definitionKeeper);
    assertThat(locations)
        .containsExactly(
            new Location(
                MagikTypedFile.DEFAULT_URI, new Range(new Position(1, 22), new Position(1, 28))));
  }

  @Test
  void testProvideDefinitionsFromLocal() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final String code =
        """
        _method object.method
            _local local1 << 10
            _return local1
        _endmethod
        """;
    final Position position = new Position(3, 14); // On `local1`.
    final List<Location> locations = this.getDefinitions(code, position, definitionKeeper);
    assertThat(locations)
        .containsExactly(
            new Location(
                MagikTypedFile.DEFAULT_URI, new Range(new Position(2, 11), new Position(2, 17))));
  }

  @Test
  void testProvideDefinitionsFromSlot() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString exemplarRef = TypeString.ofIdentifier("my_exemplar", "user");
    final Location slotLocation =
        new Location(
            MagikTypedFile.DEFAULT_URI, new Range(new Position(5, 0), new Position(5, 10)));
    definitionKeeper.add(
        new ExemplarDefinition(
            EMPTY_LOCATION,
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            exemplarRef,
            null));
    definitionKeeper.add(
        new SlotDefinition(
            slotLocation, null, null, null, null, exemplarRef, "my_slot", TypeString.UNDEFINED));

    final String code =
        """
        _method my_exemplar.method
            _return .my_slot
        _endmethod
        """;
    final Position position = new Position(2, 13); // On `my_slot`.
    final List<Location> locations = this.getDefinitions(code, position, definitionKeeper);
    assertThat(locations).containsExactly(slotLocation);
  }

  @Test
  void testProvideConditionDefinitions() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new ConditionDefinition(
            EMPTY_LOCATION, null, null, null, null, "error", null, Collections.emptyList(), null));
    final String code =
        """
        _method object.method
            _try
            _when error
            _endtry
        _endmethod
        """;
    final Position position = new Position(3, 12); // On `error`.
    final List<Location> locations = this.getDefinitions(code, position, definitionKeeper);
    assertThat(locations)
        .containsExactly(
            new Location(
                MagikTypedFile.DEFAULT_URI, new Range(new Position(0, 0), new Position(0, 0))));
  }

  @Test
  void testProvideDefinitionsFromMethodInvocation() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new MethodDefinition(
            EMPTY_LOCATION,
            null,
            null,
            null,
            null,
            TypeString.SW_INTEGER,
            "invoke_me()",
            Collections.emptySet(),
            Collections.emptyList(),
            null,
            null,
            ExpressionResultString.UNDEFINED,
            ExpressionResultString.EMPTY));

    final String code =
        """
        _method object.method
            _local var << 1
            var.invoke_me()
        _endmethod
        """;
    final Position position = new Position(3, 12); // On `invoke_me`.
    final List<Location> locations = this.getDefinitions(code, position, definitionKeeper);
    assertThat(locations).containsExactly(EMPTY_LOCATION);
  }

  @Test
  void testProvideDefinitionsProductName() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new ProductDefinition(
            EMPTY_LOCATION,
            null,
            "my_product",
            null,
            "1",
            null,
            null,
            null,
            Collections.emptyList()));

    final String code = "my_product layered_product\n";
    final Position position = new Position(1, 4); // On `my_product`.
    final List<Location> locations =
        this.getProductDefDefinitions(code, position, definitionKeeper);
    assertThat(locations).containsExactly(EMPTY_LOCATION);
  }

  @Test
  void testProvideDefinitionsModuleName() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new ModuleDefinition(
            EMPTY_LOCATION,
            null,
            "my_module",
            null,
            "1",
            "2",
            null,
            Collections.emptyList(),
            Collections.emptyList()));

    final String code = "my_module 1\n";
    final Position position = new Position(1, 4); // On `my_module`.
    final List<Location> locations = this.getModuleDefDefinitions(code, position, definitionKeeper);
    assertThat(locations).containsExactly(EMPTY_LOCATION);
  }

  @Test
  void testProvideDefinitionsFileEntry() {
    final String code = "source/my_class\n";
    final Position position = new Position(1, 4); // On `source/my_class`.
    final List<Location> locations = this.getLoadListDefinitions(code, position);
    assertThat(locations).hasSize(1);
    assertThat(locations.get(0).getUri().toString()).endsWith("my_class.magik");
  }
}
