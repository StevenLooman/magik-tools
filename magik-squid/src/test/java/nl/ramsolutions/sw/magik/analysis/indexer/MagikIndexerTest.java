package nl.ramsolutions.sw.magik.analysis.indexer;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.Collection;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.IgnoreHandler;
import nl.ramsolutions.sw.magik.analysis.MagikAnalysisConfiguration;
import nl.ramsolutions.sw.magik.analysis.definitions.BinaryOperatorDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.SlotDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import org.junit.jupiter.api.Test;

/** Test MagikIndexer. */
class MagikIndexerTest {

  /**
   * VSCode runs from module directory, mvn runs from project directory.
   *
   * @return Proper {@link Path} to file.
   */
  protected Path getPath(final Path relativePath) {
    final Path path = Path.of(".").toAbsolutePath().getParent();
    if (path.endsWith("magik-squid")) {
      return Path.of("..").resolve(relativePath);
    }

    return Path.of(".").resolve(relativePath);
  }

  @Test
  void testFileCreated() {
    final Path path = Path.of("magik-squid/src/test/resources/test_magik_indexer.magik");
    final Path fixedPath = this.getPath(path).toAbsolutePath();
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final IgnoreHandler ignoreHandler = new IgnoreHandler();
    final MagikIndexer magikIndexer =
        new MagikIndexer(
            definitionKeeper, MagikAnalysisConfiguration.DEFAULT_CONFIGURATION, ignoreHandler);
    magikIndexer.indexPathCreated(fixedPath);

    // Test exemplar.
    final TypeString typeString = TypeString.ofIdentifier("test_exemplar", "user");
    final Collection<ExemplarDefinition> exemplarDefs =
        definitionKeeper.getExemplarDefinitions(typeString);
    assertThat(exemplarDefs).hasSize(1);
    final ExemplarDefinition exemplarDef = exemplarDefs.stream().findAny().orElseThrow();

    // Test doc.
    final String doc = exemplarDef.getDoc();
    assertThat(doc).isEqualTo("Test exemplar.");

    // Test slots.
    final Collection<SlotDefinition> slots = exemplarDef.getSlots();
    assertThat(slots).hasSize(2);
    final SlotDefinition slotA = exemplarDef.getSlot("slot_a");
    assertThat(slotA.getName()).isEqualTo("slot_a");
    assertThat(slotA.getTypeName()).isEqualTo(TypeString.UNDEFINED);

    final SlotDefinition slotB = exemplarDef.getSlot("slot_b");
    assertThat(slotB.getName()).isEqualTo("slot_b");
    assertThat(slotB.getTypeName()).isEqualTo(TypeString.UNDEFINED);

    // Test methods.
    final Collection<MethodDefinition> newMethodDefs =
        definitionKeeper.getMethodDefinitions(typeString).stream()
            .filter(def -> def.getMethodName().equals("new()"))
            .collect(Collectors.toSet());
    assertThat(newMethodDefs).hasSize(1);
    final MethodDefinition newMethodDef = newMethodDefs.stream().findAny().orElseThrow();
    assertThat(newMethodDef.getDoc()).isEqualTo("Constructor.");
    assertThat(newMethodDef.getReturnTypes()).isEqualTo(ExpressionResultString.UNDEFINED);
    assertThat(newMethodDef.getLoopTypes()).isEqualTo(ExpressionResultString.EMPTY);

    final Collection<MethodDefinition> initMethodDefs =
        definitionKeeper.getMethodDefinitions(typeString).stream()
            .filter(def -> def.getMethodName().equals("init()"))
            .collect(Collectors.toSet());
    assertThat(initMethodDefs).hasSize(1);
    final MethodDefinition initMethodDef = initMethodDefs.stream().findAny().orElseThrow();
    assertThat(initMethodDef.getDoc()).isEqualTo("Initializer.");
    assertThat(initMethodDef.getReturnTypes()).isEqualTo(ExpressionResultString.UNDEFINED);
    assertThat(initMethodDef.getLoopTypes()).isEqualTo(ExpressionResultString.EMPTY);

    // Test globals.
    final Collection<GlobalDefinition> globalDefs = definitionKeeper.getGlobalDefinitions();
    assertThat(globalDefs).hasSize(1);
    final GlobalDefinition globalDef = globalDefs.stream().findAny().orElseThrow();
    assertThat(globalDef.getTypeString())
        .isEqualTo(TypeString.ofIdentifier("!test_global!", "user"));
    assertThat(globalDef.getAliasedTypeName()).isEqualTo(TypeString.UNDEFINED);

    // Test binary expressions.
    final Collection<BinaryOperatorDefinition> binOpDefs =
        definitionKeeper.getBinaryOperatorDefinitions();
    assertThat(binOpDefs).hasSize(1);
    final BinaryOperatorDefinition binOpDef = binOpDefs.stream().findAny().orElseThrow();
    assertThat(binOpDef.getLhsTypeName()).isEqualTo(TypeString.ofIdentifier("integer", "user"));
    assertThat(binOpDef.getRhsTypeName()).isEqualTo(TypeString.ofIdentifier("integer", "user"));
    assertThat(binOpDef.getResultTypeName())
        .isEqualTo(TypeString.combine(TypeString.SW_FALSE, TypeString.SW_MAYBE));
  }

  @Test
  void testFileCreatedWithTypeDoc() {
    final Path path =
        Path.of("magik-squid/src/test/resources/test_magik_indexer_with_type_doc.magik");
    final Path fixedPath = this.getPath(path).toAbsolutePath();
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final IgnoreHandler ignoreHandler = new IgnoreHandler();
    final MagikIndexer magikIndexer =
        new MagikIndexer(
            definitionKeeper, MagikAnalysisConfiguration.DEFAULT_CONFIGURATION, ignoreHandler);
    magikIndexer.indexPathCreated(fixedPath);

    // Test exemplar.
    final TypeString typeString = TypeString.ofIdentifier("test_exemplar", "user");
    final Collection<ExemplarDefinition> exemplarDefs =
        definitionKeeper.getExemplarDefinitions(typeString);
    assertThat(exemplarDefs).hasSize(1);
    final ExemplarDefinition exemplarDef = exemplarDefs.stream().findAny().orElseThrow();

    // Test doc.
    final String doc = exemplarDef.getDoc();
    assertThat(doc)
        .isEqualTo(
            """
                Test exemplar.
                @slot {sw:rope} slot_a
                @slot {sw:property_list<K=sw:symbol, E=sw:integer>} slot_b""");

    // Test slots.
    final Collection<SlotDefinition> slots = exemplarDef.getSlots();
    assertThat(slots).hasSize(2);
    final SlotDefinition slotA = exemplarDef.getSlot("slot_a");
    assertThat(slotA.getName()).isEqualTo("slot_a");
    final TypeString ropeRef = TypeString.ofIdentifier("rope", "sw");
    assertThat(slotA.getTypeName()).isEqualTo(ropeRef);

    final SlotDefinition slotB = exemplarDef.getSlot("slot_b");
    assertThat(slotB.getName()).isEqualTo("slot_b");
    assertThat(slotB.getTypeName())
        .isEqualTo(
            TypeString.ofIdentifier(
                "sw:property_list",
                "user",
                TypeString.ofGenericDefinition("K", TypeString.SW_SYMBOL),
                TypeString.ofGenericDefinition("E", TypeString.SW_INTEGER)));

    // Test methods.
    final Collection<MethodDefinition> newMethodDefs =
        definitionKeeper.getMethodDefinitions(typeString).stream()
            .filter(def -> def.getMethodName().equals("new()"))
            .collect(Collectors.toSet());
    assertThat(newMethodDefs).hasSize(1);
    final MethodDefinition newMethodDef = newMethodDefs.stream().findAny().orElseThrow();
    assertThat(newMethodDef.getDoc())
        .isEqualTo(
            """
        Constructor.
        @return {_self}""");
    assertThat(newMethodDef.getReturnTypes())
        .isEqualTo(new ExpressionResultString(TypeString.SELF));
    assertThat(newMethodDef.getLoopTypes()).isEqualTo(ExpressionResultString.EMPTY);

    final Collection<MethodDefinition> initMethodDefs =
        definitionKeeper.getMethodDefinitions(typeString).stream()
            .filter(def -> def.getMethodName().equals("init()"))
            .collect(Collectors.toSet());
    assertThat(initMethodDefs).hasSize(1);
    final MethodDefinition initMethodDef = initMethodDefs.stream().findAny().orElseThrow();
    assertThat(initMethodDef.getDoc())
        .isEqualTo(
            """
        Initializer.
        @return {_self}""");
    assertThat(initMethodDef.getReturnTypes())
        .isEqualTo(new ExpressionResultString(TypeString.SELF));
    assertThat(initMethodDef.getLoopTypes()).isEqualTo(ExpressionResultString.EMPTY);
  }

  @Test
  void testFileChanged() {
    // Read first.
    final Path path = Path.of("magik-squid/src/test/resources/test_magik_indexer.magik");
    final Path fixedPath = this.getPath(path).toAbsolutePath();
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final IgnoreHandler ignoreHandler = new IgnoreHandler();
    final MagikIndexer magikIndexer =
        new MagikIndexer(
            definitionKeeper, MagikAnalysisConfiguration.DEFAULT_CONFIGURATION, ignoreHandler);
    magikIndexer.indexPathCreated(fixedPath);

    // Pretend update.
    magikIndexer.indexPathChanged(fixedPath);

    // Test type.
    final TypeString typeString = TypeString.ofIdentifier("test_exemplar", "user");
    final Collection<ExemplarDefinition> exemplarDefs =
        definitionKeeper.getExemplarDefinitions(typeString);
    assertThat(exemplarDefs).hasSize(1);
    final ExemplarDefinition exemplarDef = exemplarDefs.stream().findAny().orElseThrow();
    assertThat(exemplarDef).isNotNull();

    // Test methods.
    final Collection<MethodDefinition> newMethodDefs =
        definitionKeeper.getMethodDefinitions(typeString).stream()
            .filter(def -> def.getMethodName().equals("new()"))
            .collect(Collectors.toSet());
    assertThat(newMethodDefs).hasSize(1);
    final Collection<MethodDefinition> initMethodDefs =
        definitionKeeper.getMethodDefinitions(typeString).stream()
            .filter(def -> def.getMethodName().equals("init()"))
            .collect(Collectors.toSet());
    assertThat(initMethodDefs).hasSize(1);
  }

  @Test
  void testFileDeleted() {
    // Read first.
    final Path path = Path.of("magik-squid/src/test/resources/test_magik_indexer.magik");
    final Path fixedPath = this.getPath(path).toAbsolutePath();
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final IgnoreHandler ignoreHandler = new IgnoreHandler();
    final MagikIndexer magikIndexer =
        new MagikIndexer(
            definitionKeeper, MagikAnalysisConfiguration.DEFAULT_CONFIGURATION, ignoreHandler);
    magikIndexer.indexPathCreated(fixedPath);

    // Test type.
    final TypeString typeString = TypeString.ofIdentifier("test_exemplar", "user");
    final Collection<ExemplarDefinition> preExemplarDefs =
        definitionKeeper.getExemplarDefinitions(typeString);
    assertThat(preExemplarDefs).hasSize(1);
    final ExemplarDefinition preExemplarDef = preExemplarDefs.stream().findAny().orElseThrow();
    assertThat(preExemplarDef).isNotNull();

    // Pretend delete.
    magikIndexer.indexPathDeleted(fixedPath);

    // Test type.
    final Collection<ExemplarDefinition> postExemplarDefs =
        definitionKeeper.getExemplarDefinitions(typeString);
    assertThat(postExemplarDefs).isEmpty();
  }
}
