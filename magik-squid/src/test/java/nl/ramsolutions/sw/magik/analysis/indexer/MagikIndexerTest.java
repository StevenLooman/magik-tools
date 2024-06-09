package nl.ramsolutions.sw.magik.analysis.indexer;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.IgnoreHandler;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.magik.FileEvent;
import nl.ramsolutions.sw.magik.FileEvent.FileChangeType;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.analysis.definitions.BinaryOperatorDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ParameterDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ProcedureDefinition;
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
  void testFileCreated() throws IOException {
    final Path path = Path.of("magik-squid/src/test/resources/test_magik_indexer.magik");
    final URI uri = this.getPath(path).toUri();
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final IgnoreHandler ignoreHandler = new IgnoreHandler();
    final MagikIndexer magikIndexer =
        new MagikIndexer(definitionKeeper, MagikToolsProperties.DEFAULT_PROPERTIES, ignoreHandler);
    final FileEvent fileEvent = new FileEvent(uri, FileChangeType.CREATED);
    magikIndexer.handleFileEvent(fileEvent);

    // Test exemplar.
    final TypeString typeString = TypeString.ofIdentifier("test_exemplar", "user");
    final Collection<ExemplarDefinition> exemplarDefs =
        definitionKeeper.getExemplarDefinitions(typeString);
    assertThat(exemplarDefs).hasSize(1);
    final ExemplarDefinition exemplarDef = exemplarDefs.stream().findAny().orElseThrow();
    assertThat(exemplarDef)
        .isEqualTo(
            new ExemplarDefinition(
                new Location(uri, new Range(new Position(4, 20), new Position(4, 21))),
                null,
                null,
                "Test exemplar.",
                null,
                ExemplarDefinition.Sort.SLOTTED,
                typeString,
                List.of(
                    new SlotDefinition(
                        new Location(uri, new Range(new Position(7, 8), new Position(7, 9))),
                        null,
                        null,
                        null,
                        null,
                        "slot_a",
                        TypeString.UNDEFINED),
                    new SlotDefinition(
                        new Location(uri, new Range(new Position(8, 8), new Position(8, 9))),
                        null,
                        null,
                        null,
                        null,
                        "slot_b",
                        TypeString.UNDEFINED)),
                List.of(TypeString.ofIdentifier("sw_regexp", "sw")),
                Collections.emptySet()));

    // Test methods.
    final Collection<MethodDefinition> newMethodDefs =
        definitionKeeper.getMethodDefinitions(typeString).stream()
            .filter(def -> def.getMethodName().equals("new()"))
            .collect(Collectors.toSet());
    assertThat(newMethodDefs).hasSize(1);
    final MethodDefinition newMethodDef = newMethodDefs.stream().findAny().orElseThrow();
    assertThat(newMethodDef)
        .isEqualTo(
            new MethodDefinition(
                new Location(uri, new Range(new Position(13, 0), new Position(13, 7))),
                null,
                null,
                "Constructor.",
                null,
                typeString,
                "new()",
                Collections.emptySet(),
                Collections.emptyList(),
                null,
                Collections.emptySet(),
                ExpressionResultString.UNDEFINED,
                ExpressionResultString.EMPTY));

    final Collection<MethodDefinition> initMethodDefs =
        definitionKeeper.getMethodDefinitions(typeString).stream()
            .filter(def -> def.getMethodName().equals("init()"))
            .collect(Collectors.toSet());
    assertThat(initMethodDefs).hasSize(1);
    final MethodDefinition initMethodDef = initMethodDefs.stream().findAny().orElseThrow();
    assertThat(initMethodDef)
        .isEqualTo(
            new MethodDefinition(
                new Location(uri, new Range(new Position(19, 0), new Position(19, 8))),
                null,
                null,
                "Initializer.",
                null,
                typeString,
                "init()",
                Set.of(MethodDefinition.Modifier.PRIVATE),
                Collections.emptyList(),
                null,
                Collections.emptySet(),
                ExpressionResultString.UNDEFINED,
                ExpressionResultString.EMPTY));

    // Test globals.
    final Collection<GlobalDefinition> globalDefs = definitionKeeper.getGlobalDefinitions();
    assertThat(globalDefs).hasSize(1);
    final GlobalDefinition globalDef = globalDefs.stream().findAny().orElseThrow();
    assertThat(globalDef)
        .isEqualTo(
            new GlobalDefinition(
                new Location(uri, new Range(new Position(26, 0), new Position(26, 7))),
                null,
                null,
                "",
                null,
                TypeString.ofIdentifier("!test_global!", "user"),
                TypeString.UNDEFINED));

    // Test binary expressions.
    final Collection<BinaryOperatorDefinition> binOpDefs =
        definitionKeeper.getBinaryOperatorDefinitions();
    assertThat(binOpDefs).hasSize(1);
    final BinaryOperatorDefinition binOpDef = binOpDefs.stream().findAny().orElseThrow();
    assertThat(binOpDef)
        .isEqualTo(
            new BinaryOperatorDefinition(
                new Location(uri, new Range(new Position(29, 27), new Position(29, 28))),
                null,
                null,
                "@return {sw:false|sw:maybe}",
                null,
                "=",
                TypeString.ofIdentifier("integer", "user"),
                TypeString.ofIdentifier("integer", "user"),
                TypeString.combine(TypeString.SW_FALSE, TypeString.SW_MAYBE)));

    // Test procedures.
    final Collection<ProcedureDefinition> procDefs = definitionKeeper.getProcedureDefinitions();
    assertThat(procDefs).hasSize(1);
    final ProcedureDefinition procDef = procDefs.stream().findAny().orElseThrow();
    assertThat(procDef)
        .isEqualTo(
            new ProcedureDefinition(
                new Location(uri, new Range(new Position(30, 4), new Position(30, 9))),
                null,
                null,
                "@return {sw:false|sw:maybe}",
                null,
                Collections.emptySet(),
                procDef.getTypeString(), // Cheat a bit, dependent on absolute file location.
                null,
                List.of(
                    new ParameterDefinition(
                        new Location(uri, new Range(new Position(30, 10), new Position(30, 13))),
                        null,
                        null,
                        null,
                        null,
                        "lhs",
                        ParameterDefinition.Modifier.NONE,
                        TypeString.UNDEFINED),
                    new ParameterDefinition(
                        new Location(uri, new Range(new Position(30, 15), new Position(30, 18))),
                        null,
                        null,
                        null,
                        null,
                        "rhs",
                        ParameterDefinition.Modifier.NONE,
                        TypeString.UNDEFINED)),
                new ExpressionResultString(
                    TypeString.combine(TypeString.SW_FALSE, TypeString.SW_MAYBE)),
                ExpressionResultString.EMPTY));
  }

  @Test
  void testFileCreatedWithTypeDoc() throws IOException {
    final Path path =
        Path.of("magik-squid/src/test/resources/test_magik_indexer_with_type_doc.magik");
    final URI uri = this.getPath(path).toUri();
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final IgnoreHandler ignoreHandler = new IgnoreHandler();
    final MagikIndexer magikIndexer =
        new MagikIndexer(definitionKeeper, MagikToolsProperties.DEFAULT_PROPERTIES, ignoreHandler);
    final FileEvent fileEvent = new FileEvent(uri, FileChangeType.CREATED);
    magikIndexer.handleFileEvent(fileEvent);

    // Test exemplar.
    final TypeString typeString = TypeString.ofIdentifier("test_exemplar", "user");
    final Collection<ExemplarDefinition> exemplarDefs =
        definitionKeeper.getExemplarDefinitions(typeString);
    assertThat(exemplarDefs).hasSize(1);
    final ExemplarDefinition exemplarDef = exemplarDefs.stream().findAny().orElseThrow();
    assertThat(exemplarDef)
        .isEqualTo(
            new ExemplarDefinition(
                new Location(uri, new Range(new Position(6, 20), new Position(6, 21))),
                null,
                null,
                "Test exemplar.\n@slot {sw:rope} slot_a\n@slot {sw:property_list<K=sw:symbol, E=sw:integer>} slot_b",
                null,
                ExemplarDefinition.Sort.SLOTTED,
                typeString,
                List.of(
                    new SlotDefinition(
                        new Location(uri, new Range(new Position(9, 8), new Position(9, 9))),
                        null,
                        null,
                        null,
                        null,
                        "slot_a",
                        TypeString.ofIdentifier("rope", "sw")),
                    new SlotDefinition(
                        new Location(uri, new Range(new Position(10, 8), new Position(10, 9))),
                        null,
                        null,
                        null,
                        null,
                        "slot_b",
                        TypeString.ofIdentifier(
                            "property_list",
                            "sw",
                            TypeString.ofGenericDefinition("K", TypeString.SW_SYMBOL),
                            TypeString.ofGenericDefinition("E", TypeString.SW_INTEGER)))),
                Collections.emptyList(),
                Collections.emptySet()));

    // Test methods.
    final Collection<MethodDefinition> newMethodDefs =
        definitionKeeper.getMethodDefinitions(typeString).stream()
            .filter(def -> def.getMethodName().equals("new()"))
            .collect(Collectors.toSet());
    assertThat(newMethodDefs).hasSize(1);
    final MethodDefinition newMethodDef = newMethodDefs.stream().findAny().orElseThrow();
    assertThat(newMethodDef)
        .isEqualTo(
            new MethodDefinition(
                new Location(uri, new Range(new Position(14, 0), new Position(14, 7))),
                null,
                null,
                "Constructor.\n@return {_self}",
                null,
                typeString,
                "new()",
                Collections.emptySet(),
                Collections.emptyList(),
                null,
                Collections.emptySet(),
                new ExpressionResultString(TypeString.SELF),
                ExpressionResultString.EMPTY));

    final Collection<MethodDefinition> initMethodDefs =
        definitionKeeper.getMethodDefinitions(typeString).stream()
            .filter(def -> def.getMethodName().equals("init()"))
            .collect(Collectors.toSet());
    assertThat(initMethodDefs).hasSize(1);
    final MethodDefinition initMethodDef = initMethodDefs.stream().findAny().orElseThrow();
    assertThat(initMethodDef)
        .isEqualTo(
            new MethodDefinition(
                new Location(uri, new Range(new Position(21, 0), new Position(21, 8))),
                null,
                null,
                "Initializer.\n@return {_self}",
                null,
                typeString,
                "init()",
                Set.of(MethodDefinition.Modifier.PRIVATE),
                Collections.emptyList(),
                null,
                Collections.emptySet(),
                new ExpressionResultString(TypeString.SELF),
                ExpressionResultString.EMPTY));
  }

  @Test
  void testFileChanged() throws IOException {
    // Read first.
    final Path path = Path.of("magik-squid/src/test/resources/test_magik_indexer.magik");
    final URI uri = this.getPath(path).toUri();
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final IgnoreHandler ignoreHandler = new IgnoreHandler();
    final MagikIndexer magikIndexer =
        new MagikIndexer(definitionKeeper, MagikToolsProperties.DEFAULT_PROPERTIES, ignoreHandler);
    final FileEvent createdFileEvent = new FileEvent(uri, FileChangeType.CREATED);
    magikIndexer.handleFileEvent(createdFileEvent);

    // Pretend update.
    final FileEvent changedFileEvent = new FileEvent(uri, FileChangeType.CHANGED);
    magikIndexer.handleFileEvent(changedFileEvent);

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
  void testFileDeleted() throws IOException {
    // Read first.
    final Path path = Path.of("magik-squid/src/test/resources/test_magik_indexer.magik");
    final URI uri = this.getPath(path).toUri();
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final IgnoreHandler ignoreHandler = new IgnoreHandler();
    final MagikIndexer magikIndexer =
        new MagikIndexer(definitionKeeper, MagikToolsProperties.DEFAULT_PROPERTIES, ignoreHandler);
    final FileEvent createdFileEvent = new FileEvent(uri, FileChangeType.CREATED);
    magikIndexer.handleFileEvent(createdFileEvent);

    // Test type.
    final TypeString typeString = TypeString.ofIdentifier("test_exemplar", "user");
    final Collection<ExemplarDefinition> preExemplarDefs =
        definitionKeeper.getExemplarDefinitions(typeString);
    assertThat(preExemplarDefs).hasSize(1);
    final ExemplarDefinition preExemplarDef = preExemplarDefs.stream().findAny().orElseThrow();
    assertThat(preExemplarDef).isNotNull();

    // Pretend delete.
    final FileEvent deletedFileEvent = new FileEvent(uri, FileChangeType.DELETED);
    magikIndexer.handleFileEvent(deletedFileEvent);

    // Test type.
    final Collection<ExemplarDefinition> postExemplarDefs =
        definitionKeeper.getExemplarDefinitions(typeString);
    assertThat(postExemplarDefs).isEmpty();
  }
}
