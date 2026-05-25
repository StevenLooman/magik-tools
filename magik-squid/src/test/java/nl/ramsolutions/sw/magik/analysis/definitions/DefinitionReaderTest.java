package nl.ramsolutions.sw.magik.analysis.definitions;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonar.sslr.api.AstNode;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import org.junit.jupiter.api.Test;

/** Test DefinitionReader. */
@SuppressWarnings("checkstyle:MagicNumber")
class DefinitionReaderTest {

  private MagikFile createMagikFile(final String code) {
    return new MagikFile(MagikFile.DEFAULT_URI, code);
  }

  @Test
  void testPackageDefinition() {
    final String code = "def_package(:test)";
    final MagikFile magikFile = this.createMagikFile(code);
    final AstNode node = magikFile.getTopNode();
    final MagikDefinitionReader reader = new MagikDefinitionReader(magikFile);
    reader.walkAst(node);

    final List<MagikDefinition> definitions = reader.getDefinitions();
    assertThat(definitions)
        .containsExactly(
            new PackageDefinition(
                new Location(
                    URI.create("memory:///source.magik"),
                    new Range(new Position(1, 11), new Position(1, 12))),
                null,
                null,
                "",
                null,
                "test",
                List.of("sw")));
  }

  @Test
  void testPackageDefinitionUses() {
    final String code = "def_package(:test, :uses, {:p1, :p2})";
    final MagikFile magikFile = this.createMagikFile(code);
    final AstNode node = magikFile.getTopNode();
    final MagikDefinitionReader reader = new MagikDefinitionReader(magikFile);
    reader.walkAst(node);

    final List<MagikDefinition> definitions = reader.getDefinitions();
    assertThat(definitions)
        .containsExactly(
            new PackageDefinition(
                new Location(
                    URI.create("memory:///source.magik"),
                    new Range(new Position(1, 11), new Position(1, 12))),
                null,
                null,
                "",
                null,
                "test",
                List.of("p1", "p2")));
  }

  @Test
  void testEnumeratorDefinition() {
    final String code = "sw:def_enumeration(:test_enum, _false, :a)";
    final MagikFile magikFile = this.createMagikFile(code);
    final AstNode node = magikFile.getTopNode();
    final MagikDefinitionReader reader = new MagikDefinitionReader(magikFile);
    reader.walkAst(node);

    final List<MagikDefinition> definitions = reader.getDefinitions();
    assertThat(definitions)
        .containsExactly(
            new ExemplarDefinition(
                new Location(
                    URI.create("memory:///source.magik"),
                    new Range(new Position(1, 19), new Position(1, 29))),
                null,
                null,
                "",
                null,
                ExemplarDefinition.Sort.SLOTTED,
                TypeString.ofIdentifier("test_enum", "user"),
                Collections.emptyList(),
                List.of(TypeString.ofIdentifier("enumeration_value", "sw")),
                null));
  }

  @Test
  void testDefSlottedExemplar() {
    final String code =
        "def_slotted_exemplar(:test_exemplar, {{:slot1, _unset}, {:slot2, _unset}})";
    final MagikFile magikFile = this.createMagikFile(code);
    final AstNode node = magikFile.getTopNode();
    final MagikDefinitionReader reader = new MagikDefinitionReader(magikFile);
    reader.walkAst(node);

    final List<MagikDefinition> definitions = reader.getDefinitions();
    assertThat(definitions)
        .containsExactly(
            new ExemplarDefinition(
                new Location(
                    URI.create("memory:///source.magik"),
                    new Range(new Position(1, 21), new Position(1, 35))),
                null,
                null,
                "",
                null,
                ExemplarDefinition.Sort.SLOTTED,
                TypeString.ofIdentifier("test_exemplar", "user"),
                List.of(
                    new SlotDefinition(
                        new Location(
                            URI.create("memory:///source.magik"),
                            new Range(new Position(1, 38), new Position(1, 39))),
                        null,
                        null,
                        null,
                        null,
                        "slot1",
                        TypeString.UNDEFINED),
                    new SlotDefinition(
                        new Location(
                            URI.create("memory:///source.magik"),
                            new Range(new Position(1, 56), new Position(1, 57))),
                        null,
                        null,
                        null,
                        null,
                        "slot2",
                        TypeString.UNDEFINED)),
                Collections.emptyList(),
                null));
  }

  @Test
  void testDefSlottedExemplarSlotAccessMethods() {
    final String code =
        "def_slotted_exemplar(:test_exemplar, {{:slot1, _unset, :writable, :read_only}})";
    final MagikFile magikFile = this.createMagikFile(code);
    final AstNode node = magikFile.getTopNode();
    final MagikDefinitionReader reader = new MagikDefinitionReader(magikFile);
    reader.walkAst(node);

    final List<MagikDefinition> definitions = reader.getDefinitions();
    assertThat(definitions)
        .containsExactly(
            new ExemplarDefinition(
                new Location(
                    URI.create("memory:///source.magik"),
                    new Range(new Position(1, 21), new Position(1, 35))),
                null,
                null,
                "",
                null,
                ExemplarDefinition.Sort.SLOTTED,
                TypeString.ofIdentifier("test_exemplar", "user"),
                List.of(
                    new SlotDefinition(
                        new Location(
                            URI.create("memory:///source.magik"),
                            new Range(new Position(1, 38), new Position(1, 39))),
                        null,
                        null,
                        null,
                        null,
                        "slot1",
                        TypeString.UNDEFINED)),
                Collections.emptyList(),
                null),
            new MethodDefinition(
                new Location(
                    URI.create("memory:///source.magik"),
                    new Range(new Position(1, 38), new Position(1, 39))),
                null,
                null,
                null,
                null,
                TypeString.ofIdentifier("test_exemplar", "user"),
                "slot1",
                Collections.emptySet(),
                Collections.emptyList(),
                null,
                null,
                new ExpressionResultString(TypeString.UNDEFINED),
                ExpressionResultString.EMPTY),
            new MethodDefinition(
                new Location(
                    URI.create("memory:///source.magik"),
                    new Range(new Position(1, 38), new Position(1, 39))),
                null,
                null,
                null,
                null,
                TypeString.ofIdentifier("test_exemplar", "user"),
                "slot1<<",
                Set.of(MethodDefinition.Modifier.PRIVATE),
                Collections.emptyList(),
                new ParameterDefinition(
                    null,
                    null,
                    null,
                    null,
                    null,
                    "val",
                    ParameterDefinition.Modifier.NONE,
                    TypeString.UNDEFINED),
                null,
                new ExpressionResultString(TypeString.ofParameterRef("val")),
                ExpressionResultString.EMPTY),
            new MethodDefinition(
                new Location(
                    URI.create("memory:///source.magik"),
                    new Range(new Position(1, 38), new Position(1, 39))),
                null,
                null,
                null,
                null,
                TypeString.ofIdentifier("test_exemplar", "user"),
                "slot1^<<",
                Set.of(MethodDefinition.Modifier.PRIVATE),
                Collections.emptyList(),
                new ParameterDefinition(
                    null,
                    null,
                    null,
                    null,
                    null,
                    "val",
                    ParameterDefinition.Modifier.NONE,
                    TypeString.UNDEFINED),
                null,
                new ExpressionResultString(TypeString.UNDEFINED),
                ExpressionResultString.EMPTY));
  }

  @Test
  void testDefSlottedExemplarParentsSingle() {
    final String code = "def_slotted_exemplar(:test_exemplar, {}, @sw:rope)";
    final MagikFile magikFile = this.createMagikFile(code);
    final AstNode node = magikFile.getTopNode();
    final MagikDefinitionReader reader = new MagikDefinitionReader(magikFile);
    reader.walkAst(node);

    final List<MagikDefinition> definitions = reader.getDefinitions();
    assertThat(definitions)
        .containsExactly(
            new ExemplarDefinition(
                new Location(
                    URI.create("memory:///source.magik"),
                    new Range(new Position(1, 21), new Position(1, 35))),
                null,
                null,
                "",
                null,
                ExemplarDefinition.Sort.SLOTTED,
                TypeString.ofIdentifier("test_exemplar", "user"),
                Collections.emptyList(),
                List.of(TypeString.ofIdentifier("rope", "sw")),
                null));
  }

  @Test
  void testDefSlottedExemplarParentsMultiple() {
    final String code = "def_slotted_exemplar(:test_exemplar, {}, {:mixin1, :rope})";
    final MagikFile magikFile = this.createMagikFile(code);
    final AstNode node = magikFile.getTopNode();
    final MagikDefinitionReader reader = new MagikDefinitionReader(magikFile);
    reader.walkAst(node);

    final List<MagikDefinition> definitions = reader.getDefinitions();
    assertThat(definitions)
        .containsExactly(
            new ExemplarDefinition(
                new Location(
                    URI.create("memory:///source.magik"),
                    new Range(new Position(1, 21), new Position(1, 35))),
                null,
                null,
                "",
                null,
                ExemplarDefinition.Sort.SLOTTED,
                TypeString.ofIdentifier("test_exemplar", "user"),
                Collections.emptyList(),
                List.of(
                    TypeString.ofIdentifier("mixin1", "user"),
                    TypeString.ofIdentifier("rope", "user")),
                null));
  }

  @Test
  void testDefIndexedExemplar() {
    final String code = "def_indexed_exemplar(:test_exemplar, _unset, {:mixin1, :integer})";
    final MagikFile magikFile = this.createMagikFile(code);
    final AstNode node = magikFile.getTopNode();
    final MagikDefinitionReader reader = new MagikDefinitionReader(magikFile);
    reader.walkAst(node);

    final List<MagikDefinition> definitions = reader.getDefinitions();
    assertThat(definitions)
        .containsExactly(
            new ExemplarDefinition(
                new Location(
                    URI.create("memory:///source.magik"),
                    new Range(new Position(1, 21), new Position(1, 35))),
                null,
                null,
                "",
                null,
                ExemplarDefinition.Sort.INDEXED,
                TypeString.ofIdentifier("test_exemplar", "user"),
                Collections.emptyList(),
                List.of(
                    TypeString.ofIdentifier("mixin1", "user"),
                    TypeString.ofIdentifier("integer", "user")),
                null));
  }

  @Test
  void testDefIndexedExemplarUnknownParents() {
    final String code = "def_indexed_exemplar(:test_exemplar, _unset, {var})";
    final MagikFile magikFile = this.createMagikFile(code);
    final AstNode node = magikFile.getTopNode();
    final MagikDefinitionReader reader = new MagikDefinitionReader(magikFile);
    reader.walkAst(node);

    final List<MagikDefinition> definitions = reader.getDefinitions();
    assertThat(definitions)
        .containsExactly(
            new ExemplarDefinition(
                new Location(
                    URI.create("memory:///source.magik"),
                    new Range(new Position(1, 21), new Position(1, 35))),
                null,
                null,
                "",
                null,
                ExemplarDefinition.Sort.INDEXED,
                TypeString.ofIdentifier("test_exemplar", "user"),
                Collections.emptyList(),
                Collections.emptyList(),
                null));
  }

  @Test
  void testDefMixin() {
    final String code = "def_mixin(:test_mixin, :mixin1)";
    final MagikFile magikFile = this.createMagikFile(code);
    final AstNode node = magikFile.getTopNode();
    final MagikDefinitionReader reader = new MagikDefinitionReader(magikFile);
    reader.walkAst(node);

    final List<MagikDefinition> definitions = reader.getDefinitions();
    assertThat(definitions)
        .containsExactly(
            new ExemplarDefinition(
                new Location(
                    URI.create("memory:///source.magik"),
                    new Range(new Position(1, 10), new Position(1, 21))),
                null,
                null,
                "",
                null,
                ExemplarDefinition.Sort.MIXIN,
                TypeString.ofIdentifier("test_mixin", "user"),
                Collections.emptyList(),
                List.of(TypeString.ofIdentifier("mixin1", "user")),
                null));
  }

  @Test
  void testDefineBinaryOperatorCase() {
    final String code = "define_binary_operator_case(:|>|, integer, float, _proc(a, b) _endproc)";
    final MagikFile magikFile = this.createMagikFile(code);
    final AstNode node = magikFile.getTopNode();
    final MagikDefinitionReader reader = new MagikDefinitionReader(magikFile);
    reader.walkAst(node);

    final List<MagikDefinition> definitions = reader.getDefinitions();
    assertThat(definitions)
        .containsExactly(
            new ProcedureDefinition(
                new Location(
                    URI.create("memory:///source.magik"),
                    new Range(new Position(1, 50), new Position(1, 55))),
                null,
                null,
                "",
                null,
                Collections.emptySet(),
                TypeString.ofIdentifier("_proc_in_memory_0", TypeString.ANONYMOUS_PACKAGE),
                null,
                List.of(
                    new ParameterDefinition(
                        new Location(
                            URI.create("memory:///source.magik"),
                            new Range(new Position(1, 56), new Position(1, 57))),
                        null,
                        null,
                        null,
                        null,
                        "a",
                        ParameterDefinition.Modifier.NONE,
                        TypeString.UNDEFINED),
                    new ParameterDefinition(
                        new Location(
                            URI.create("memory:///source.magik"),
                            new Range(new Position(1, 59), new Position(1, 60))),
                        null,
                        null,
                        null,
                        null,
                        "b",
                        ParameterDefinition.Modifier.NONE,
                        TypeString.UNDEFINED)),
                null,
                ExpressionResultString.EMPTY,
                ExpressionResultString.EMPTY),
            new BinaryOperatorDefinition(
                new Location(
                    URI.create("memory:///source.magik"),
                    new Range(new Position(1, 27), new Position(1, 28))),
                null,
                null,
                "",
                null,
                ">",
                TypeString.ofIdentifier("integer", "user"),
                TypeString.ofIdentifier("float", "user"),
                TypeString.UNDEFINED));
  }

  @Test
  void testMethodDefinition() { // NOSONAR
    final String code = "_method a.b _endmethod";
    final MagikFile magikFile = this.createMagikFile(code);
    final AstNode node = magikFile.getTopNode();
    final MagikDefinitionReader reader = new MagikDefinitionReader(magikFile);
    reader.walkAst(node);

    final List<MagikDefinition> definitions = reader.getDefinitions();
    assertThat(definitions)
        .containsExactly(
            new MethodDefinition(
                new Location(
                    URI.create("memory:///source.magik"),
                    new Range(new Position(1, 10), new Position(1, 11))),
                null,
                null,
                "",
                null,
                TypeString.ofIdentifier("a", "user"),
                "b",
                Collections.emptySet(),
                Collections.emptyList(),
                null,
                null,
                ExpressionResultString.EMPTY,
                ExpressionResultString.EMPTY));
  }

  @Test
  void testMethodDefinitionSyntaxError() {
    final String code = "_method a.b _a _endmethod";
    final MagikFile magikFile = this.createMagikFile(code);
    final AstNode node = magikFile.getTopNode();
    final MagikDefinitionReader reader = new MagikDefinitionReader(magikFile);
    reader.walkAst(node);

    final List<MagikDefinition> definitions = reader.getDefinitions();
    assertThat(definitions).isEmpty();
  }

  @Test
  void testMethodDefinitionPrivateAbstractIter() {
    final String code = "_abstract _private _iter _method a.b() _endmethod";
    final MagikFile magikFile = this.createMagikFile(code);
    final AstNode node = magikFile.getTopNode();
    final MagikDefinitionReader reader = new MagikDefinitionReader(magikFile);
    reader.walkAst(node);

    final List<MagikDefinition> definitions = reader.getDefinitions();
    assertThat(definitions)
        .containsExactly(
            new MethodDefinition(
                new Location(
                    URI.create("memory:///source.magik"),
                    new Range(new Position(1, 35), new Position(1, 36))),
                null,
                null,
                "",
                null,
                TypeString.ofIdentifier("a", "user"),
                "b()",
                Set.of(
                    MethodDefinition.Modifier.PRIVATE,
                    MethodDefinition.Modifier.ABSTRACT,
                    MethodDefinition.Modifier.ITER),
                Collections.emptyList(),
                null,
                null,
                ExpressionResultString.EMPTY,
                ExpressionResultString.EMPTY));
  }

  @Test
  void testMethodDefinitionParameters() {
    final String code = "_method a.b(a, _optional b, c, _gather d) << z _endmethod";
    final MagikFile magikFile = this.createMagikFile(code);
    final AstNode node = magikFile.getTopNode();
    final MagikDefinitionReader reader = new MagikDefinitionReader(magikFile);
    reader.walkAst(node);

    final List<MagikDefinition> definitions = reader.getDefinitions();
    assertThat(definitions)
        .containsExactly(
            new MethodDefinition(
                new Location(
                    URI.create("memory:///source.magik"),
                    new Range(new Position(1, 10), new Position(1, 11))),
                null,
                null,
                "",
                null,
                TypeString.ofIdentifier("a", "user"),
                "b()<<",
                Collections.emptySet(),
                List.of(
                    new ParameterDefinition(
                        new Location(
                            URI.create("memory:///source.magik"),
                            new Range(new Position(1, 12), new Position(1, 13))),
                        null,
                        null,
                        null,
                        null,
                        "a",
                        ParameterDefinition.Modifier.NONE,
                        TypeString.UNDEFINED),
                    new ParameterDefinition(
                        new Location(
                            URI.create("memory:///source.magik"),
                            new Range(new Position(1, 15), new Position(1, 24))),
                        null,
                        null,
                        null,
                        null,
                        "b",
                        ParameterDefinition.Modifier.OPTIONAL,
                        TypeString.UNDEFINED),
                    new ParameterDefinition(
                        new Location(
                            URI.create("memory:///source.magik"),
                            new Range(new Position(1, 28), new Position(1, 29))),
                        null,
                        null,
                        null,
                        null,
                        "c",
                        ParameterDefinition.Modifier.OPTIONAL,
                        TypeString.UNDEFINED),
                    new ParameterDefinition(
                        new Location(
                            URI.create("memory:///source.magik"),
                            new Range(new Position(1, 31), new Position(1, 38))),
                        null,
                        null,
                        null,
                        null,
                        "d",
                        ParameterDefinition.Modifier.GATHER,
                        TypeString.UNDEFINED)),
                new ParameterDefinition(
                    new Location(
                        URI.create("memory:///source.magik"),
                        new Range(new Position(1, 45), new Position(1, 46))),
                    null,
                    null,
                    null,
                    null,
                    "z",
                    ParameterDefinition.Modifier.NONE,
                    TypeString.UNDEFINED),
                null,
                ExpressionResultString.EMPTY,
                ExpressionResultString.EMPTY));
  }

  @Test
  void testDefineSlotAccess() { // NOSONAR
    final String code = "test_exemplar.define_slot_access(:slot1, :readable, :public)";
    final MagikFile magikFile = this.createMagikFile(code);
    final AstNode node = magikFile.getTopNode();
    final MagikDefinitionReader reader = new MagikDefinitionReader(magikFile);
    reader.walkAst(node);

    final List<MagikDefinition> definitions = reader.getDefinitions();
    assertThat(definitions)
        .containsExactly(
            new MethodDefinition(
                new Location(
                    URI.create("memory:///source.magik"),
                    new Range(new Position(1, 0), new Position(1, 13))),
                null,
                null,
                "",
                null,
                TypeString.ofIdentifier("test_exemplar", "user"),
                "slot1",
                Collections.emptySet(),
                Collections.emptyList(),
                null,
                null,
                new ExpressionResultString(TypeString.UNDEFINED),
                ExpressionResultString.EMPTY));
  }

  @Test
  void testDefineSlotExternallyReadable() { // NOSONAR
    final String code = "test_exemplar.define_slot_externally_readable(:slot1)";
    final MagikFile magikFile = this.createMagikFile(code);
    final AstNode node = magikFile.getTopNode();
    final MagikDefinitionReader reader = new MagikDefinitionReader(magikFile);
    reader.walkAst(node);

    final List<MagikDefinition> definitions = reader.getDefinitions();
    assertThat(definitions)
        .containsExactly(
            new MethodDefinition(
                new Location(
                    URI.create("memory:///source.magik"),
                    new Range(new Position(1, 0), new Position(1, 13))),
                null,
                null,
                "",
                null,
                TypeString.ofIdentifier("test_exemplar", "user"),
                "slot1",
                Collections.emptySet(),
                Collections.emptyList(),
                null,
                null,
                new ExpressionResultString(TypeString.UNDEFINED),
                ExpressionResultString.EMPTY));
  }

  @Test
  void testDefineSlotExternallyReadablePublic() { // NOSONAR
    final String code = "test_exemplar.define_slot_externally_readable(:slot1, :public)";
    final MagikFile magikFile = this.createMagikFile(code);
    final AstNode node = magikFile.getTopNode();
    final MagikDefinitionReader reader = new MagikDefinitionReader(magikFile);
    reader.walkAst(node);

    final List<MagikDefinition> definitions = reader.getDefinitions();
    assertThat(definitions)
        .containsExactly(
            new MethodDefinition(
                new Location(
                    URI.create("memory:///source.magik"),
                    new Range(new Position(1, 0), new Position(1, 13))),
                null,
                null,
                "",
                null,
                TypeString.ofIdentifier("test_exemplar", "user"),
                "slot1",
                Collections.emptySet(),
                Collections.emptyList(),
                null,
                null,
                new ExpressionResultString(TypeString.UNDEFINED),
                ExpressionResultString.EMPTY));
  }

  @Test
  void testDefineSlotExternallyWritable() {
    final String code = "test_exemplar.define_slot_externally_writable(:slot1, :public)";
    final MagikFile magikFile = this.createMagikFile(code);
    final AstNode node = magikFile.getTopNode();
    final MagikDefinitionReader reader = new MagikDefinitionReader(magikFile);
    reader.walkAst(node);

    final List<MagikDefinition> definitions = reader.getDefinitions();
    assertThat(definitions)
        .containsExactly(
            new MethodDefinition(
                new Location(
                    URI.create("memory:///source.magik"),
                    new Range(new Position(1, 0), new Position(1, 13))),
                null,
                null,
                "",
                null,
                TypeString.ofIdentifier("test_exemplar", "user"),
                "slot1",
                Collections.emptySet(),
                Collections.emptyList(),
                null,
                null,
                new ExpressionResultString(TypeString.UNDEFINED),
                ExpressionResultString.EMPTY),
            new MethodDefinition(
                new Location(
                    URI.create("memory:///source.magik"),
                    new Range(new Position(1, 0), new Position(1, 13))),
                null,
                null,
                "",
                null,
                TypeString.ofIdentifier("test_exemplar", "user"),
                "slot1<<",
                Collections.emptySet(),
                Collections.emptyList(),
                new ParameterDefinition(
                    null,
                    null,
                    null,
                    null,
                    null,
                    "val",
                    ParameterDefinition.Modifier.NONE,
                    TypeString.UNDEFINED),
                null,
                new ExpressionResultString(TypeString.ofParameterRef("val")),
                ExpressionResultString.EMPTY),
            new MethodDefinition(
                new Location(
                    URI.create("memory:///source.magik"),
                    new Range(new Position(1, 0), new Position(1, 13))),
                null,
                null,
                "",
                null,
                TypeString.ofIdentifier("test_exemplar", "user"),
                "slot1^<<",
                Collections.emptySet(),
                Collections.emptyList(),
                new ParameterDefinition(
                    null,
                    null,
                    null,
                    null,
                    null,
                    "val",
                    ParameterDefinition.Modifier.NONE,
                    TypeString.UNDEFINED),
                null,
                new ExpressionResultString(TypeString.UNDEFINED),
                ExpressionResultString.EMPTY));
  }

  @Test
  void testDefineSharedVariable() {
    final String code = "test_exemplar.define_shared_variable(:var1, 1, :readonly)";
    final MagikFile magikFile = this.createMagikFile(code);
    final AstNode node = magikFile.getTopNode();
    final MagikDefinitionReader reader = new MagikDefinitionReader(magikFile);
    reader.walkAst(node);

    final List<MagikDefinition> definitions = reader.getDefinitions();
    assertThat(definitions)
        .containsExactly(
            new MethodDefinition(
                new Location(
                    URI.create("memory:///source.magik"),
                    new Range(new Position(1, 37), new Position(1, 42))),
                null,
                null,
                "",
                null,
                TypeString.ofIdentifier("test_exemplar", "user"),
                "var1",
                Collections.emptySet(),
                Collections.emptyList(),
                null,
                null,
                new ExpressionResultString(TypeString.UNDEFINED),
                ExpressionResultString.EMPTY),
            new MethodDefinition(
                new Location(
                    URI.create("memory:///source.magik"),
                    new Range(new Position(1, 37), new Position(1, 42))),
                null,
                null,
                "",
                null,
                TypeString.ofIdentifier("test_exemplar", "user"),
                "var1<<",
                Set.of(MethodDefinition.Modifier.PRIVATE),
                Collections.emptyList(),
                new ParameterDefinition(
                    null,
                    null,
                    null,
                    null,
                    null,
                    "val",
                    ParameterDefinition.Modifier.NONE,
                    TypeString.UNDEFINED),
                null,
                new ExpressionResultString(TypeString.ofParameterRef("val")),
                ExpressionResultString.EMPTY),
            new MethodDefinition(
                new Location(
                    URI.create("memory:///source.magik"),
                    new Range(new Position(1, 37), new Position(1, 42))),
                null,
                null,
                "",
                null,
                TypeString.ofIdentifier("test_exemplar", "user"),
                "var1^<<",
                Set.of(MethodDefinition.Modifier.PRIVATE),
                Collections.emptyList(),
                new ParameterDefinition(
                    null,
                    null,
                    null,
                    null,
                    null,
                    "val",
                    ParameterDefinition.Modifier.NONE,
                    TypeString.UNDEFINED),
                null,
                new ExpressionResultString(TypeString.UNDEFINED),
                ExpressionResultString.EMPTY));
  }

  @Test
  void testDefineSharedConstant() {
    final String code = "test_exemplar.define_shared_constant(:const1, 1, :private)";
    final MagikFile magikFile = this.createMagikFile(code);
    final AstNode node = magikFile.getTopNode();
    final MagikDefinitionReader reader = new MagikDefinitionReader(magikFile);
    reader.walkAst(node);

    final List<MagikDefinition> definitions = reader.getDefinitions();
    assertThat(definitions)
        .containsExactly(
            new MethodDefinition(
                new Location(
                    URI.create("memory:///source.magik"),
                    new Range(new Position(1, 37), new Position(1, 44))),
                null,
                null,
                "",
                null,
                TypeString.ofIdentifier("test_exemplar", "user"),
                "const1",
                Set.of(MethodDefinition.Modifier.PRIVATE),
                Collections.emptyList(),
                null,
                null,
                new ExpressionResultString(TypeString.UNDEFINED),
                ExpressionResultString.EMPTY));
  }

  @Test
  void testDefineSharedConstant2() {
    final String code = "test_exemplar.define_shared_constant(:const1, 1, _false)";
    final MagikFile magikFile = this.createMagikFile(code);
    final AstNode node = magikFile.getTopNode();
    final MagikDefinitionReader reader = new MagikDefinitionReader(magikFile);
    reader.walkAst(node);

    final List<MagikDefinition> definitions = reader.getDefinitions();
    assertThat(definitions)
        .containsExactly(
            new MethodDefinition(
                new Location(
                    URI.create("memory:///source.magik"),
                    new Range(new Position(1, 37), new Position(1, 44))),
                null,
                null,
                "",
                null,
                TypeString.ofIdentifier("test_exemplar", "user"),
                "const1",
                Collections.emptySet(),
                Collections.emptyList(),
                null,
                null,
                new ExpressionResultString(TypeString.UNDEFINED),
                ExpressionResultString.EMPTY));
  }

  @Test
  void testAssignGlobal() {
    final String code = "_global g << _unset";
    final MagikFile magikFile = this.createMagikFile(code);
    final AstNode node = magikFile.getTopNode();
    final MagikDefinitionReader reader = new MagikDefinitionReader(magikFile);
    reader.walkAst(node);

    final List<MagikDefinition> definitions = reader.getDefinitions();
    assertThat(definitions)
        .containsExactly(
            new GlobalDefinition(
                new Location(
                    URI.create("memory:///source.magik"),
                    new Range(new Position(1, 0), new Position(1, 7))),
                null,
                null,
                "",
                null,
                TypeString.ofIdentifier("g", "user"),
                TypeString.UNDEFINED,
                null));
  }

  @Test
  void testParseCondition() {
    final String code =
        """
        _pragma(classify_level=restricted,topic=test)
        condition.define_condition(:cond1, :information, {:data1, :data2})
        """;
    final MagikFile magikFile = this.createMagikFile(code);
    final AstNode node = magikFile.getTopNode();
    final MagikDefinitionReader reader = new MagikDefinitionReader(magikFile);
    reader.walkAst(node);

    final List<MagikDefinition> definitions = reader.getDefinitions();
    assertThat(definitions)
        .containsExactly(
            new ConditionDefinition(
                new Location(
                    URI.create("memory:///source.magik"),
                    new Range(new Position(2, 9), new Position(2, 10))),
                null,
                null,
                "",
                null,
                "cond1",
                "information",
                List.of("data1", "data2"),
                new Pragma(null, Set.of("restricted"), Set.of("test"), Set.of())));
  }

  @Test
  void testParseConditionInBlock() {
    final String code =
        """
        _block
          condition.define_condition(:cond1, :information, {:data1, :data2})
        _endblock""";
    final MagikFile magikFile = this.createMagikFile(code);
    final AstNode node = magikFile.getTopNode();
    final MagikDefinitionReader reader = new MagikDefinitionReader(magikFile);
    reader.walkAst(node);

    final List<MagikDefinition> definitions = reader.getDefinitions();
    assertThat(definitions)
        .containsExactly(
            new ConditionDefinition(
                new Location(
                    URI.create("memory:///source.magik"),
                    new Range(new Position(2, 11), new Position(2, 12))),
                null,
                null,
                "",
                null,
                "cond1",
                "information",
                List.of("data1", "data2"),
                null));
  }

  @Test
  void testMethodDefinitionWithInvokesMethod() {
    final String code =
        """
        _method a.b()
            ## @invokes_method {rope.size}
            ## @invokes_method {sw:integer.write()}
        _endmethod
        """;
    final MagikFile magikFile = this.createMagikFile(code);
    final AstNode node = magikFile.getTopNode();
    final MagikDefinitionReader reader = new MagikDefinitionReader(magikFile);
    reader.walkAst(node);

    final List<MagikDefinition> definitions = reader.getDefinitions();
    assertThat(definitions).hasSize(1);

    final MethodDefinition methodDef = (MethodDefinition) definitions.get(0);
    final List<MethodUsage> usedMethods = methodDef.getUsedMethods();
    assertThat(usedMethods)
        .containsExactly(
            new MethodUsage(TypeString.ofIdentifier("rope", "user"), "size"),
            new MethodUsage(TypeString.ofIdentifier("integer", "sw"), "write()"));
  }

  @Test
  void testMethodDefinitionWithInvokesMethodSquareBrackets() {
    final String code =
        """
        _method a.b()
            ## @invokes_method {rope[]}
            ## @invokes_method {sw:vector_key_equality_table[,]}
        _endmethod
        """;
    final MagikFile magikFile = this.createMagikFile(code);
    final AstNode node = magikFile.getTopNode();
    final MagikDefinitionReader reader = new MagikDefinitionReader(magikFile);
    reader.walkAst(node);

    final List<MagikDefinition> definitions = reader.getDefinitions();
    assertThat(definitions).hasSize(1);

    final MethodDefinition methodDef = (MethodDefinition) definitions.get(0);
    final List<MethodUsage> usedMethods = methodDef.getUsedMethods();
    assertThat(usedMethods)
        .containsExactly(
            new MethodUsage(TypeString.ofIdentifier("rope", "user"), "[]"),
            new MethodUsage(TypeString.ofIdentifier("vector_key_equality_table", "sw"), "[,]"));
  }

  @Test
  void testProcedureDefinitionWithInvokesMethod() {
    final String code =
        """
        _proc()
            ## @invokes_method {rope.size}
            ## @invokes_method {sw:integer.write()}
        _endproc
        """;
    final MagikFile magikFile = this.createMagikFile(code);
    final AstNode node = magikFile.getTopNode();
    final MagikDefinitionReader reader = new MagikDefinitionReader(magikFile);
    reader.walkAst(node);

    final List<MagikDefinition> definitions = reader.getDefinitions();
    assertThat(definitions).hasSize(1);

    final ProcedureDefinition procDef = (ProcedureDefinition) definitions.get(0);
    final List<MethodUsage> usedMethods = procDef.getUsedMethods();
    assertThat(usedMethods)
        .containsExactly(
            new MethodUsage(TypeString.ofIdentifier("rope", "user"), "size"),
            new MethodUsage(TypeString.ofIdentifier("integer", "sw"), "write()"));
  }
}
