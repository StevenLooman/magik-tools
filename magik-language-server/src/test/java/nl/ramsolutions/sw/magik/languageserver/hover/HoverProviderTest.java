package nl.ramsolutions.sw.magik.languageserver.hover;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Collections;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.definitions.BinaryOperatorDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.Position;
import org.junit.jupiter.api.Test;

/** Test HoverProvider. */
@SuppressWarnings("checkstyle:MagicNumber")
class HoverProviderTest {

  private static final URI DEFAULT_URI = URI.create("memory://source.magik");

  private Hover provideHover(
      final String code, final Position position, final IDefinitionKeeper definitionKeeper) {
    final MagikTypedFile magikFile = new MagikTypedFile(DEFAULT_URI, code, definitionKeeper);
    final HoverProvider provider = new HoverProvider();
    return provider.provideHover(magikFile, position);
  }

  @Test
  void testProvideHoverMethodDefinitionName() {
    // Set up a method.
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new MethodDefinition(
            null,
            null,
            "method_doc",
            null,
            TypeString.SW_OBJECT,
            "hover_me_method()",
            Collections.emptySet(),
            Collections.emptyList(),
            null,
            Collections.emptySet(),
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
            "type_doc",
            null,
            ExemplarDefinition.Sort.SLOTTED,
            hoverMeTypeRef,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptySet()));

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
  void testProvideHoverMethod() {
    // Set up a method.
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new MethodDefinition(
            null,
            null,
            "method_doc",
            null,
            TypeString.SW_INTEGER,
            "hover_me()",
            Collections.emptySet(),
            Collections.emptyList(),
            null,
            Collections.emptySet(),
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
  void testBinaryOperatorTimes() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new BinaryOperatorDefinition(
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
