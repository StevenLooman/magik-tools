package nl.ramsolutions.sw.magik.languageserver.hover;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.definitions.BinaryOperatorDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ConditionDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.InheritanceDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.PackageDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ProcedureDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.SlotDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.moduledef.ModuleDefFile;
import nl.ramsolutions.sw.moduledef.ModuleDefinition;
import nl.ramsolutions.sw.productdef.ProductDefFile;
import nl.ramsolutions.sw.productdef.ProductDefinition;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.Position;
import org.junit.jupiter.api.Test;

/** Test {@link HoverProvider}. */
@SuppressWarnings("checkstyle:MagicNumber")
class HoverProviderTest {

  private static final URI DEFAULT_URI = URI.create("memory:///source.def");

  private Hover provideHover(
      final String code, final Position position, final IDefinitionKeeper definitionKeeper) {
    final MagikTypedFile magikFile =
        new MagikTypedFile(MagikTypedFile.DEFAULT_URI, code, definitionKeeper);
    final HoverProvider provider = new HoverProvider();
    return provider.provideHover(magikFile, position);
  }

  private Hover provideHoverProductDef(
      final String code, final Position position, final IDefinitionKeeper definitionKeeper) {
    final ProductDefFile productDefFile =
        new ProductDefFile(DEFAULT_URI, code, definitionKeeper, null);
    final HoverProvider provider = new HoverProvider();
    return provider.provideHover(productDefFile, position);
  }

  private Hover provideHoverModuleDef(
      final String code, final Position position, final IDefinitionKeeper definitionKeeper) {
    final ModuleDefFile moduleDefFile =
        new ModuleDefFile(DEFAULT_URI, code, definitionKeeper, null);
    final HoverProvider provider = new HoverProvider();
    return provider.provideHover(moduleDefFile, position);
  }

  @Test
  void testProvideHoverMethodDefinitionName() {
    // Set up a method.
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new MethodDefinition(
            null,
            null,
            null,
            "method_doc",
            null,
            TypeString.SW_OBJECT,
            "hover_me_method()",
            Collections.emptySet(),
            Collections.emptyList(),
            null,
            null,
            ExpressionResultString.UNDEFINED,
            ExpressionResultString.EMPTY));

    final String code =
        """
        _method object.hover_me_method()
        _endmethod""";
    final Position position = new Position(0, 18); // On 'hover_me_method'.

    // Hover and test.
    final Hover hover = this.provideHover(code, position, definitionKeeper);
    final MarkupContent content = hover.getContents().getRight();
    assertThat(content.getKind()).isEqualTo(MarkupKind.MARKDOWN);
    assertThat(content.getValue()).contains("sw:object.hover_me_method()");
    assertThat(content.getValue()).contains("method_doc");
  }

  @Test
  void testProvideHoverExemplarName() {
    // Set up a method.
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString hoverMeTypeRef = TypeString.ofIdentifier("hover_me_type", "user");
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            "type_doc",
            null,
            ExemplarDefinition.Sort.SLOTTED,
            hoverMeTypeRef,
            null));

    final String code =
        """
        _method hover_me_type.method()
        _endmethod""";
    final Position position = new Position(0, 10); // On 'hover_me_type'.

    // Hover and test.
    final Hover hover = this.provideHover(code, position, definitionKeeper);
    final MarkupContent content = hover.getContents().getRight();
    assertThat(content.getKind()).isEqualTo(MarkupKind.MARKDOWN);
    assertThat(content.getValue()).contains("hover_me_type");
    assertThat(content.getValue()).contains("type_doc");
  }

  @Test
  void testProvideHoverExemplarShowsInheritedSlotWithOwnerAnnotation() {
    // Set up a parent exemplar with a slot, and a child exemplar with its own slot,
    // linked by an inheritance edge.
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString parentTypeRef = TypeString.ofIdentifier("hover_parent_type", "user");
    definitionKeeper.add(
        new ExemplarDefinition(
            null, null, null, null, null, ExemplarDefinition.Sort.SLOTTED, parentTypeRef, null));
    definitionKeeper.add(
        new SlotDefinition(
            null, null, null, null, null, parentTypeRef, "parent_slot", TypeString.SW_SYMBOL));

    final TypeString childTypeRef = TypeString.ofIdentifier("hover_child_type", "user");
    definitionKeeper.add(
        new ExemplarDefinition(
            null, null, null, null, null, ExemplarDefinition.Sort.SLOTTED, childTypeRef, null));
    definitionKeeper.add(
        new SlotDefinition(
            null, null, null, null, null, childTypeRef, "child_slot", TypeString.SW_INTEGER));

    definitionKeeper.add(
        new InheritanceDefinition(null, null, null, null, null, childTypeRef, parentTypeRef));

    final String code =
        """
        _method hover_child_type.method()
        _endmethod""";
    final Position position = new Position(0, 10); // On 'hover_child_type'.

    // Hover and test.
    final Hover hover = this.provideHover(code, position, definitionKeeper);
    final MarkupContent content = hover.getContents().getRight();
    assertThat(content.getKind()).isEqualTo(MarkupKind.MARKDOWN);

    // The child's own slot is not annotated with an owner.
    assertThat(content.getValue()).contains("* child_slot: sw:integer\n");
    // The inherited slot is annotated with its owning type.
    assertThat(content.getValue())
        .contains("* parent_slot: sw:symbol _(from user:hover_parent_type)_\n");
  }

  @Test
  void testProvideHoverMethod() {
    // Set up a method.
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new MethodDefinition(
            null,
            null,
            null,
            "method_doc",
            null,
            TypeString.SW_INTEGER,
            "hover_me()",
            Collections.emptySet(),
            Collections.emptyList(),
            null,
            null,
            ExpressionResultString.UNDEFINED,
            ExpressionResultString.EMPTY));

    final String code =
        """
        _method a.b
            _local var << 1
            var.hover_me()
        _endmethod""";
    final Position position = new Position(2, 10); // On `hover_me`.

    // Hover and test.
    final Hover hover = this.provideHover(code, position, definitionKeeper);
    final MarkupContent content = hover.getContents().getRight();
    assertThat(content.getKind()).isEqualTo(MarkupKind.MARKDOWN);
    assertThat(content.getValue()).contains("sw:integer.hover_me()");
    assertThat(content.getValue()).contains("method_doc");
  }

  @Test
  void testProvideHoverMethodUnknown() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

    final String code =
        """
        _method a.b
            _local var << 1
            var.hover_me()
        _endmethod""";
    final Position position = new Position(2, 10); // On `hover_me`.

    // Hover and test.
    final Hover hover = this.provideHover(code, position, definitionKeeper);
    assertThat(hover).isNull();
  }

  @Test
  void testProvideHoverType() {
    // Set up a method.
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

    final String code =
        """
        _method a.b
            _local var << :symbol
            var.hover_me()
        _endmethod""";
    final Position position = new Position(2, 4); // On `var`.

    // Hover and test.
    final Hover hover = this.provideHover(code, position, definitionKeeper);
    final MarkupContent content = hover.getContents().getRight();
    assertThat(content.getKind()).isEqualTo(MarkupKind.MARKDOWN);
    assertThat(content.getValue()).contains("symbol");
  }

  @Test
  void testProvideHoverTypeUnknown() {
    // Set up a method.
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

    final String code =
        """
        _method a.b
            _local var << some_object
            var.hover_me()
        _endmethod""";
    final Position position = new Position(2, 4); // On `var`.

    // Hover and test.
    final Hover hover = this.provideHover(code, position, definitionKeeper);
    assertThat(hover).isNull();
  }

  @Test
  void testProvideHoverAssignedVariable() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

    final String code =
        """
        _method a.b
            _local var << :symbol
        _endmethod""";
    final Position position = new Position(1, 11); // On `var`.

    // Hover and test.
    final Hover hover = this.provideHover(code, position, definitionKeeper);
    final MarkupContent content = hover.getContents().getRight();
    assertThat(content.getKind()).isEqualTo(MarkupKind.MARKDOWN);
    assertThat(content.getValue()).contains("symbol");
  }

  @Test
  void testProvideHoverPackage() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new PackageDefinition(
            null, null, null, "package_doc", null, "hover_me_package", List.of("sw")));

    final String code =
        """
        _package hover_me_package
        """;
    final Position position = new Position(0, 12); // On 'hover_me_package'.

    // Hover and test.
    final Hover hover = this.provideHover(code, position, definitionKeeper);
    final MarkupContent content = hover.getContents().getRight();
    assertThat(content.getKind()).isEqualTo(MarkupKind.MARKDOWN);
    assertThat(content.getValue()).contains("hover_me_package");
    assertThat(content.getValue()).contains("package_doc");
    assertThat(content.getValue()).contains(" ↳ sw");
  }

  @Test
  void testProvideHoverCondition() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new ConditionDefinition(
            null,
            null,
            null,
            "condition_doc",
            null,
            "hover_me_condition",
            "error",
            List.of("data_name"),
            null));

    final String code =
        """
        _method a.b
            _handling hover_me_condition _with _default
        _endmethod""";
    final Position position = new Position(1, 16); // On 'hover_me_condition'.

    // Hover and test.
    final Hover hover = this.provideHover(code, position, definitionKeeper);
    final MarkupContent content = hover.getContents().getRight();
    assertThat(content.getKind()).isEqualTo(MarkupKind.MARKDOWN);
    assertThat(content.getValue()).contains("hover_me_condition");
    assertThat(content.getValue()).contains("condition_doc");
    assertThat(content.getValue()).contains(" ↳ error");
    assertThat(content.getValue()).contains("* data_name");
  }

  @Test
  void testProvideHoverProcedureInvocation() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString procedureTypeRef = TypeString.ofIdentifier("hover_me_proc", "user");
    definitionKeeper.add(
        new ProcedureDefinition(
            null,
            null,
            null,
            "procedure_doc",
            null,
            Collections.emptySet(),
            procedureTypeRef,
            "hover_me_proc",
            Collections.emptyList(),
            null,
            ExpressionResultString.UNDEFINED,
            ExpressionResultString.EMPTY));

    final String code =
        """
        _method a.b
            hover_me_proc()
        _endmethod""";
    final Position position = new Position(1, 6); // On 'hover_me_proc'.

    // Hover and test.
    final Hover hover = this.provideHover(code, position, definitionKeeper);
    final MarkupContent content = hover.getContents().getRight();
    assertThat(content.getKind()).isEqualTo(MarkupKind.MARKDOWN);
    assertThat(content.getValue()).contains("hover_me_proc()");
    assertThat(content.getValue()).contains("procedure_doc");
  }

  @Test
  void testProvideHoverSlot() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString hoverMeTypeRef = TypeString.ofIdentifier("hover_me_type", "user");
    definitionKeeper.add(
        new ExemplarDefinition(
            null, null, null, null, null, ExemplarDefinition.Sort.SLOTTED, hoverMeTypeRef, null));
    definitionKeeper.add(
        new SlotDefinition(
            null, null, null, null, null, hoverMeTypeRef, "hover_me_slot", TypeString.SW_SYMBOL));

    final String code =
        """
        _method hover_me_type.method()
            .hover_me_slot
        _endmethod""";
    final Position position = new Position(1, 8); // On 'hover_me_slot'.

    // Hover and test.
    final Hover hover = this.provideHover(code, position, definitionKeeper);
    final MarkupContent content = hover.getContents().getRight();
    assertThat(content.getKind()).isEqualTo(MarkupKind.MARKDOWN);
    assertThat(content.getValue()).contains("sw:symbol");
  }

  @Test
  void testProvideHoverParameter() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

    final String code =
        """
        _method a.b(p_param)
            ## @param {sw:symbol} p_param
        _endmethod""";
    final Position position = new Position(0, 14); // On 'p_param'.

    // Hover and test.
    final Hover hover = this.provideHover(code, position, definitionKeeper);
    final MarkupContent content = hover.getContents().getRight();
    assertThat(content.getKind()).isEqualTo(MarkupKind.MARKDOWN);
    assertThat(content.getValue()).contains("sw:symbol");
  }

  private IDefinitionKeeper createIndexedDefinitionKeeper() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString hoverMeTypeRef = TypeString.ofIdentifier("hover_me_list", "user");
    definitionKeeper.add(
        new ExemplarDefinition(
            null, null, null, null, null, ExemplarDefinition.Sort.SLOTTED, hoverMeTypeRef, null));
    List.of("[]", "[]<<", "[,]")
        .forEach(
            methodName ->
                definitionKeeper.add(
                    new MethodDefinition(
                        null,
                        null,
                        null,
                        "doc for " + methodName,
                        null,
                        hoverMeTypeRef,
                        methodName,
                        Collections.emptySet(),
                        Collections.emptyList(),
                        null,
                        null,
                        new ExpressionResultString(TypeString.SW_INTEGER),
                        ExpressionResultString.EMPTY)));
    return definitionKeeper;
  }

  @Test
  void testProvideHoverIndexedInvocation() {
    final IDefinitionKeeper definitionKeeper = this.createIndexedDefinitionKeeper();

    final String code =
        """
        _method a.b(p_list)
            ## @param {user:hover_me_list} p_list List.
            p_list[:key]
        _endmethod""";

    // Hover on '[' and on ']'.
    for (final int character : List.of(10, 15)) {
      final Hover hover = this.provideHover(code, new Position(2, character), definitionKeeper);
      final MarkupContent content = hover.getContents().getRight();
      assertThat(content.getKind()).isEqualTo(MarkupKind.MARKDOWN);
      assertThat(content.getValue()).contains("user:hover_me_list[]");
      assertThat(content.getValue()).contains("doc for []");
    }
  }

  @Test
  void testProvideHoverIndexedInvocationAssignment() {
    final IDefinitionKeeper definitionKeeper = this.createIndexedDefinitionKeeper();

    final String code =
        """
        _method a.b(p_list)
            ## @param {user:hover_me_list} p_list List.
            p_list[:key] << 4
        _endmethod""";
    final Position position = new Position(2, 10); // On '['.

    // Hover and test.
    final Hover hover = this.provideHover(code, position, definitionKeeper);
    final MarkupContent content = hover.getContents().getRight();
    assertThat(content.getKind()).isEqualTo(MarkupKind.MARKDOWN);
    assertThat(content.getValue()).contains("user:hover_me_list[]<<");
    assertThat(content.getValue()).contains("doc for []<<");
  }

  @Test
  void testProvideHoverIndexedInvocationMultipleArguments() {
    final IDefinitionKeeper definitionKeeper = this.createIndexedDefinitionKeeper();

    final String code =
        """
        _method a.b(p_list)
            ## @param {user:hover_me_list} p_list List.
            p_list[1, 2]
        _endmethod""";
    final Position position = new Position(2, 10); // On '['.

    // Hover and test.
    final Hover hover = this.provideHover(code, position, definitionKeeper);
    final MarkupContent content = hover.getContents().getRight();
    assertThat(content.getKind()).isEqualTo(MarkupKind.MARKDOWN);
    assertThat(content.getValue()).contains("user:hover_me_list[,]");
    assertThat(content.getValue()).contains("doc for [,]");
  }

  @Test
  void testProvideHoverIndexedInvocationArgument() {
    final IDefinitionKeeper definitionKeeper = this.createIndexedDefinitionKeeper();

    final String code =
        """
        _method a.b(p_list)
            ## @param {user:hover_me_list} p_list List.
            p_list[:key]
        _endmethod""";
    final Position position = new Position(2, 12); // On ':key', inside the brackets.

    // The argument keeps hovering as itself, not as the indexed method.
    final Hover hover = this.provideHover(code, position, definitionKeeper);
    final MarkupContent content = hover.getContents().getRight();
    assertThat(content.getKind()).isEqualTo(MarkupKind.MARKDOWN);
    assertThat(content.getValue()).contains("sw:symbol");
    assertThat(content.getValue()).doesNotContain("hover_me_list[]");
  }

  @Test
  void testProvideHoverCombinedType() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString typeStrA = TypeString.ofIdentifier("hover_me_type_a", "user");
    definitionKeeper.add(
        new ExemplarDefinition(
            null, null, null, "doc_a", null, ExemplarDefinition.Sort.SLOTTED, typeStrA, null));
    final TypeString typeStrB = TypeString.ofIdentifier("hover_me_type_b", "user");
    definitionKeeper.add(
        new ExemplarDefinition(
            null, null, null, "doc_b", null, ExemplarDefinition.Sort.SLOTTED, typeStrB, null));

    final String code =
        """
        _method a.b(value)
            ## @param {user:hover_me_type_a|user:hover_me_type_b} value Value.
            _local x << value
        _endmethod""";
    final Position position = new Position(2, 17); // On 'value'.

    // Hover and test.
    final Hover hover = this.provideHover(code, position, definitionKeeper);
    final MarkupContent content = hover.getContents().getRight();
    assertThat(content.getKind()).isEqualTo(MarkupKind.MARKDOWN);
    assertThat(content.getValue()).contains("user:hover_me_type_a");
    assertThat(content.getValue()).contains("doc_a");
    assertThat(content.getValue()).contains("user:hover_me_type_b");
    assertThat(content.getValue()).contains("doc_b");
  }

  @Test
  void testProvideHoverForVariable() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new MethodDefinition(
            null,
            null,
            null,
            null,
            null,
            TypeString.SW_INTEGER,
            "hover_me_iter()",
            Set.of(MethodDefinition.Modifier.ITER),
            Collections.emptyList(),
            null,
            null,
            ExpressionResultString.EMPTY,
            new ExpressionResultString(TypeString.SW_SYMBOL)));

    final String code =
        """
        _method a.b
            _for i _over 1.hover_me_iter()
            _loop
            _endloop
        _endmethod""";
    final Position position = new Position(1, 9); // On 'i'.

    // Hover and test.
    final Hover hover = this.provideHover(code, position, definitionKeeper);
    final MarkupContent content = hover.getContents().getRight();
    assertThat(content.getKind()).isEqualTo(MarkupKind.MARKDOWN);
    assertThat(content.getValue()).contains("sw:symbol");
  }

  @Test
  void testProvideHoverProductName() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new ProductDefinition(
            null,
            null,
            "hover_me_product",
            null,
            "1",
            "version_comment",
            "product_title",
            "product_description",
            Collections.emptyList()));

    final String code =
        """
        hover_me_product layered_product
        """;
    final Position position = new Position(0, 4); // On 'hover_me_product'.

    // Hover and test.
    final Hover hover = this.provideHoverProductDef(code, position, definitionKeeper);
    final MarkupContent content = hover.getContents().getRight();
    assertThat(content.getKind()).isEqualTo(MarkupKind.MARKDOWN);
    assertThat(content.getValue()).contains("hover_me_product");
    assertThat(content.getValue()).contains("product_title");
    assertThat(content.getValue()).contains("Version: 1 version_comment");
    assertThat(content.getValue()).contains("product_description");
  }

  @Test
  void testProvideHoverModuleName() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new ModuleDefinition(
            null,
            null,
            "hover_me_module",
            null,
            "1",
            "2",
            "module_description",
            Collections.emptyList(),
            Collections.emptyList()));

    final String code =
        """
        hover_me_module 1
        """;
    final Position position = new Position(0, 4); // On 'hover_me_module'.

    // Hover and test.
    final Hover hover = this.provideHoverModuleDef(code, position, definitionKeeper);
    final MarkupContent content = hover.getContents().getRight();
    assertThat(content.getKind()).isEqualTo(MarkupKind.MARKDOWN);
    assertThat(content.getValue()).contains("hover_me_module");
    assertThat(content.getValue()).contains("Version: 1 2");
    assertThat(content.getValue()).contains("module_description");
  }

  @Test
  void testBinaryOperatorTimes() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new BinaryOperatorDefinition(
            null,
            null,
            null,
            null,
            null,
            "*",
            TypeString.SW_INTEGER,
            TypeString.SW_INTEGER,
            TypeString.SW_INTEGER));

    final String code =
        """
        _method a.b
            _local var << 4 * 4
        _endmethod""";
    final Position position = new Position(1, 20); // On `*`.

    // Hover and test.
    final Hover hover = this.provideHover(code, position, definitionKeeper);
    final MarkupContent content = hover.getContents().getRight();
    assertThat(content.getKind()).isEqualTo(MarkupKind.MARKDOWN);
    assertThat(content.getValue()).contains("integer");
  }
}
