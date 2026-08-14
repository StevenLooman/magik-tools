package nl.ramsolutions.sw.magik.languageserver.symbol;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.analysis.definitions.ConditionDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import nl.ramsolutions.sw.moduledef.ModuleDefinition;
import nl.ramsolutions.sw.productdef.ProductDefinition;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.WorkspaceSymbol;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;

/** Tests for {@link SymbolProvider}. */
@SuppressWarnings("checkstyle:MagicNumber")
class SymbolProviderTest {

  private static final Location EMPTY_LOCATION =
      new Location(MagikTypedFile.DEFAULT_URI, new Range(new Position(0, 0), new Position(0, 0)));

  private static final TypeString TYPE_STR_GATHER_TYPE_TARGET =
      TypeString.ofIdentifier("gather_type_target", "user");
  private static final TypeString TYPE_STR_GATHER_METHOD_HOST =
      TypeString.ofIdentifier("gather_method_host", "user");
  private static final TypeString TYPE_STR_DOTTED_EXCLUDE =
      TypeString.ofIdentifier("gather.dotted_exclude", "user");

  private static WorkspaceSymbol createWorkspaceSymbol(
      final String label, final SymbolKind kind, final Location location) {
    return new WorkspaceSymbol(
        label, kind, Either.forLeft(Lsp4jConversion.locationToLsp4j(location)));
  }

  @Test
  void testGetSymbolsProduct() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new ProductDefinition(
            EMPTY_LOCATION,
            null,
            "gather_product_target",
            null,
            "1",
            null,
            null,
            null,
            Collections.emptyList()));

    final SymbolProvider provider = new SymbolProvider(definitionKeeper);
    final List<WorkspaceSymbol> symbols = provider.getSymbols("gather_product_target");

    assertThat(symbols)
        .containsExactly(
            createWorkspaceSymbol(
                "Product: gather_product_target", SymbolKind.Package, EMPTY_LOCATION));
  }

  @Test
  void testGetSymbolsModule() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new ModuleDefinition(
            EMPTY_LOCATION,
            null,
            "gather_module_target",
            null,
            "1",
            "2",
            null,
            Collections.emptyList(),
            Collections.emptyList()));

    final SymbolProvider provider = new SymbolProvider(definitionKeeper);
    final List<WorkspaceSymbol> symbols = provider.getSymbols("gather_module_target");

    assertThat(symbols)
        .containsExactly(
            createWorkspaceSymbol(
                "Module: gather_module_target", SymbolKind.Module, EMPTY_LOCATION));
  }

  @Test
  void testGetSymbolsType() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new ExemplarDefinition(
            EMPTY_LOCATION,
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            TYPE_STR_GATHER_TYPE_TARGET,
            null));

    final SymbolProvider provider = new SymbolProvider(definitionKeeper);
    final List<WorkspaceSymbol> symbols = provider.getSymbols("gather_type_target");

    assertThat(symbols)
        .containsExactly(
            createWorkspaceSymbol(
                "Exemplar: " + TYPE_STR_GATHER_TYPE_TARGET.getFullString(),
                SymbolKind.Class,
                EMPTY_LOCATION));
  }

  @Test
  void testGetSymbolsMethodNoDot() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MethodDefinition methodDef =
        new MethodDefinition(
            EMPTY_LOCATION,
            null,
            null,
            null,
            null,
            TypeString.SW_OBJECT,
            "gather_method_no_dot_target()",
            Collections.emptySet(),
            Collections.emptyList(),
            null,
            null,
            ExpressionResultString.UNDEFINED,
            ExpressionResultString.EMPTY);
    definitionKeeper.add(methodDef);

    final SymbolProvider provider = new SymbolProvider(definitionKeeper);
    final List<WorkspaceSymbol> symbols = provider.getSymbols("gather_method_no_dot_target");

    assertThat(symbols)
        .containsExactly(
            createWorkspaceSymbol(
                "Method: " + methodDef.getName(), SymbolKind.Method, EMPTY_LOCATION));
  }

  @Test
  void testGetSymbolsMethodWithDot() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MethodDefinition methodDef =
        new MethodDefinition(
            EMPTY_LOCATION,
            null,
            null,
            null,
            null,
            TYPE_STR_GATHER_METHOD_HOST,
            "gather_method_target()",
            Collections.emptySet(),
            Collections.emptyList(),
            null,
            null,
            ExpressionResultString.UNDEFINED,
            ExpressionResultString.EMPTY);
    definitionKeeper.add(methodDef);

    final SymbolProvider provider = new SymbolProvider(definitionKeeper);
    final List<WorkspaceSymbol> symbols =
        provider.getSymbols("gather_method_host.gather_method_target");

    assertThat(symbols)
        .containsExactly(
            createWorkspaceSymbol(
                "Method: " + methodDef.getName(), SymbolKind.Method, EMPTY_LOCATION));
  }

  @Test
  void testGetSymbolsCondition() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new ConditionDefinition(
            EMPTY_LOCATION,
            null,
            null,
            null,
            null,
            "gather_condition_target",
            null,
            Collections.emptyList(),
            null));

    final SymbolProvider provider = new SymbolProvider(definitionKeeper);
    final List<WorkspaceSymbol> symbols = provider.getSymbols("gather_condition_target");

    assertThat(symbols)
        .containsExactly(
            createWorkspaceSymbol(
                "Condition: gather_condition_target", SymbolKind.Class, EMPTY_LOCATION));
  }

  @Test
  void testGetSymbolsEmptyQueryReturnsEmpty() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final SymbolProvider provider = new SymbolProvider(definitionKeeper);

    assertThat(provider.getSymbols("")).isEmpty();
    assertThat(provider.getSymbols("   ")).isEmpty();
  }

  @Test
  void testGetSymbolsDottedQueryExcludesTypeAndCondition() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new ExemplarDefinition(
            EMPTY_LOCATION,
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            TYPE_STR_DOTTED_EXCLUDE,
            null));
    definitionKeeper.add(
        new ConditionDefinition(
            EMPTY_LOCATION,
            null,
            null,
            null,
            null,
            "gather.dotted_exclude",
            null,
            Collections.emptyList(),
            null));

    final SymbolProvider provider = new SymbolProvider(definitionKeeper);
    final List<WorkspaceSymbol> symbols = provider.getSymbols("gather.dotted_exclude");

    assertThat(symbols).isEmpty();
  }

  @Test
  void testGetSymbolsTruncatesAfterMaxResultsInGathererOrder() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final int productCount = 150;
    final int moduleCount = 100;
    for (int i = 0; i < productCount; i++) {
      definitionKeeper.add(
          new ProductDefinition(
              EMPTY_LOCATION,
              null,
              "trunc_target_product_" + i,
              null,
              "1",
              null,
              null,
              null,
              Collections.emptyList()));
    }
    for (int i = 0; i < moduleCount; i++) {
      definitionKeeper.add(
          new ModuleDefinition(
              EMPTY_LOCATION,
              null,
              "trunc_target_module_" + i,
              null,
              "1",
              "2",
              null,
              Collections.emptyList(),
              Collections.emptyList()));
    }

    final SymbolProvider provider = new SymbolProvider(definitionKeeper);
    final List<WorkspaceSymbol> symbols = provider.getSymbols("trunc_target");

    // Products are gathered before modules, so the shared 200-result cap must fall exactly at
    // the product/module boundary: all 150 products, then the first 50 of the 100 modules.
    assertThat(symbols).hasSize(200);

    final List<WorkspaceSymbol> productSymbols = symbols.subList(0, productCount);
    final List<WorkspaceSymbol> moduleSymbols = symbols.subList(productCount, 200);
    assertThat(productSymbols).allMatch(symbol -> symbol.getKind() == SymbolKind.Package);
    assertThat(productSymbols)
        .extracting(WorkspaceSymbol::getName)
        .allMatch(name -> name.startsWith("Product: trunc_target_product_"))
        .doesNotHaveDuplicates();
    assertThat(moduleSymbols).allMatch(symbol -> symbol.getKind() == SymbolKind.Module);
    assertThat(moduleSymbols)
        .extracting(WorkspaceSymbol::getName)
        .allMatch(name -> name.startsWith("Module: trunc_target_module_"))
        .doesNotHaveDuplicates();
  }
}
