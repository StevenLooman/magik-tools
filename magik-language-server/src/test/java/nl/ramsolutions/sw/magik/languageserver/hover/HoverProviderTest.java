package nl.ramsolutions.sw.magik.languageserver.hover;

import java.net.URI;
import java.util.Collections;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.definitions.BinaryOperatorDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.Position;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test HoverProvider.
 */
@SuppressWarnings("checkstyle:MagicNumber")
class HoverProviderTest {

    private Hover provideHover(final String code, final Position position, final IDefinitionKeeper definitionKeeper) {
        final URI uri = URI.create("tests://unittest");
        final MagikTypedFile magikFile = new MagikTypedFile(uri, code, definitionKeeper);
        final HoverProvider provider = new HoverProvider();
        return provider.provideHover(magikFile, position);
    }

    @Test
    void testProvideHoverMethodDefinitionName() {
        // Set up a method in the TypeKeeper.
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final TypeString objectRef = TypeString.ofIdentifier("object", "sw");
        definitionKeeper.add(
            new MethodDefinition(
                null,
                null,
                "method_doc",
                null,
                objectRef,
                "hover_me_method()",
                Collections.emptySet(),
                Collections.emptyList(),
                null,
                ExpressionResultString.UNDEFINED,
            ExpressionResultString.EMPTY));

        final String code = ""
            + "_method object.hover_me_method()\n"
            + "_endmethod";
        final Position position = new Position(0, 18);    // On 'hover_me_method'.

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
                Collections.emptyList()));

        final String code = ""
            + "_method hover_me_type.method()\n"
            + "_endmethod";
        final Position position = new Position(0, 10);    // On 'hover_me_type'.

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
        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        definitionKeeper.add(
            new MethodDefinition(
                null,
                null,
                "method_doc",
                null,
                integerRef,
                "hover_me()",
                Collections.emptySet(),
                Collections.emptyList(),
                null,
                ExpressionResultString.UNDEFINED,
                ExpressionResultString.EMPTY));

        final String code = ""
            + "_method a.b\n"
            + "    _local var << 1\n"
            + "    var.hover_me()\n"
            + "_endmethod";
        final Position position = new Position(2, 10);  // On `hover_me`.

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

        final String code = ""
            + "_method a.b\n"
            + "    _local var << 1\n"
            + "    var.hover_me()\n"
            + "_endmethod";
        final Position position = new Position(2, 10);  // On `hover_me`.

        // Hover and test.
        final Hover hover = this.provideHover(code, position, definitionKeeper);
        final MarkupContent content = hover.getContents().getRight();
        assertThat(content.getKind()).isEqualTo(MarkupKind.MARKDOWN);
        assertThat(content.getValue()).contains("Unknown method hover_me() on type sw:integer");
    }

    @Test
    void testProvideHoverType() {
        // Set up a method.
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

        final String code = ""
            + "_method a.b\n"
            + "    _local var << :symbol\n"
            + "    var.hover_me()\n"
            + "_endmethod";
        final Position position = new Position(2, 4);  // On `var`.

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

        final String code = ""
            + "_method a.b\n"
            + "    _local var << some_object\n"
            + "    var.hover_me()\n"
            + "_endmethod";
        final Position position = new Position(2, 4);  // On `var`.

        // Hover and test.
        final Hover hover = this.provideHover(code, position, definitionKeeper);
        final MarkupContent content = hover.getContents().getRight();
        assertThat(content.getKind()).isEqualTo(MarkupKind.MARKDOWN);
        assertThat(content.getValue()).contains("_undefined");
    }

    @Test
    void testProvideHoverAssignedVariable() {
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

        final String code = ""
            + "_method a.b\n"
            + "    _local var << :symbol\n"
            + "_endmethod";
        final Position position = new Position(1, 11);  // On `var`.

        // Hover and test.
        final Hover hover = this.provideHover(code, position, definitionKeeper);
        final MarkupContent content = hover.getContents().getRight();
        assertThat(content.getKind()).isEqualTo(MarkupKind.MARKDOWN);
        assertThat(content.getValue()).contains("symbol");
    }

    @Test
    void testBinaryOperatorTimes() {
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        definitionKeeper.add(
            new BinaryOperatorDefinition(
                null,
                null,
                null,
                null,
                "*",
                integerRef,
                integerRef,
                integerRef));

        final String code = ""
            + "_method a.b\n"
            + "    _local var << 4 * 4\n"
            + "_endmethod";
        final Position position = new Position(1, 20);  // On `*`.

        // Hover and test.
        final Hover hover = this.provideHover(code, position, definitionKeeper);
        final MarkupContent content = hover.getContents().getRight();
        assertThat(content.getKind()).isEqualTo(MarkupKind.MARKDOWN);
        assertThat(content.getValue()).contains("integer");
    }

}
