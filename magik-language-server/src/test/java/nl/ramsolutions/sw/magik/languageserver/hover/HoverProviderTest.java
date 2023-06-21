package nl.ramsolutions.sw.magik.languageserver.hover;

import java.net.URI;
import java.util.Collections;
import java.util.EnumSet;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.typing.BinaryOperator;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.types.MagikType;
import nl.ramsolutions.sw.magik.analysis.typing.types.MagikType.Sort;
import nl.ramsolutions.sw.magik.analysis.typing.types.Method;
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

    private Hover provideHover(final String code, final Position position, final ITypeKeeper typeKeeper) {
        final URI uri = URI.create("tests://unittest");
        final MagikTypedFile magikFile = new MagikTypedFile(uri, code, typeKeeper);
        final HoverProvider provider = new HoverProvider();
        return provider.provideHover(magikFile, position);
    }

    @Test
    void testProvideHoverMethodDefinitionName() {
        // Set up a method in the TypeKeeper.
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final TypeString objectRef = TypeString.ofIdentifier("object", "sw");
        final MagikType objectType = (MagikType) typeKeeper.getType(objectRef);
        objectType.addMethod(
            null,
            EnumSet.noneOf(Method.Modifier.class),
            "hover_me_method()",
            Collections.emptyList(),
            null,
            "method_doc",
            ExpressionResultString.UNDEFINED,
            new ExpressionResultString());

        final String code = ""
            + "_method object.hover_me_method()\n"
            + "_endmethod";
        final Position position = new Position(0, 18);    // On 'hover_me_method'.

        // Hover and test.
        final Hover hover = this.provideHover(code, position, typeKeeper);
        final MarkupContent content = hover.getContents().getRight();
        assertThat(content.getKind()).isEqualTo(MarkupKind.MARKDOWN);
        assertThat(content.getValue()).contains("sw:object.hover_me_method()");
        assertThat(content.getValue()).contains("method_doc");
    }

    @Test
    void testProvideHoverMethodDefinitionExemplar() {
        // Set up a method.
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final TypeString hoverMeTypeRef = TypeString.ofIdentifier("hover_me_type", "user");
        final MagikType hoverMeType = new MagikType(typeKeeper, Sort.SLOTTED, hoverMeTypeRef);
        hoverMeType.setDoc("type_doc");

        final String code = ""
            + "_method hover_me_type.method()\n"
            + "_endmethod";
        final Position position = new Position(0, 10);    // On 'hover_me_type'.

        // Hover and test.
        final Hover hover = this.provideHover(code, position, typeKeeper);
        final MarkupContent content = hover.getContents().getRight();
        assertThat(content.getKind()).isEqualTo(MarkupKind.MARKDOWN);
        assertThat(content.getValue()).contains("hover_me_type");
        assertThat(content.getValue()).contains("type_doc");
    }

    @Test
    void testProvideHoverMethod() {
        // Set up a method.
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        final MagikType integerType = (MagikType) typeKeeper.getType(integerRef);
        integerType.addMethod(
            null,
            EnumSet.noneOf(Method.Modifier.class),
            "hover_me()",
            Collections.emptyList(),
            null,
            "method_doc",
            ExpressionResultString.UNDEFINED,
            new ExpressionResultString());

        final String code = ""
            + "_method a.b\n"
            + "    _local var << 1\n"
            + "    var.hover_me()\n"
            + "_endmethod";
        final Position position = new Position(2, 10);  // On `hover_me`.

        // Hover and test.
        final Hover hover = this.provideHover(code, position, typeKeeper);
        final MarkupContent content = hover.getContents().getRight();
        assertThat(content.getKind()).isEqualTo(MarkupKind.MARKDOWN);
        assertThat(content.getValue()).contains("sw:integer.hover_me()");
        assertThat(content.getValue()).contains("method_doc");
    }

    @Test
    void testProvideHoverMethodUnknown() {
        final ITypeKeeper typeKeeper = new TypeKeeper();

        final String code = ""
            + "_method a.b\n"
            + "    _local var << 1\n"
            + "    var.hover_me()\n"
            + "_endmethod";
        final Position position = new Position(2, 10);  // On `hover_me`.

        // Hover and test.
        final Hover hover = this.provideHover(code, position, typeKeeper);
        final MarkupContent content = hover.getContents().getRight();
        assertThat(content.getKind()).isEqualTo(MarkupKind.MARKDOWN);
        assertThat(content.getValue()).contains("Unknown method hover_me() on type sw:integer");
    }

    @Test
    void testProvideHoverType() {
        // Set up a method.
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final TypeString symbolRef = TypeString.ofIdentifier("symbol", "sw");
        final MagikType symbolType = (MagikType) typeKeeper.getType(symbolRef);
        symbolType.setDoc("type_doc");

        final String code = ""
            + "_method a.b\n"
            + "    _local var << :symbol\n"
            + "    var.hover_me()\n"
            + "_endmethod";
        final Position position = new Position(2, 4);  // On `var`.

        // Hover and test.
        final Hover hover = this.provideHover(code, position, typeKeeper);
        final MarkupContent content = hover.getContents().getRight();
        assertThat(content.getKind()).isEqualTo(MarkupKind.MARKDOWN);
        assertThat(content.getValue()).contains("symbol");
        assertThat(content.getValue()).contains("type_doc");
    }

    @Test
    void testProvideHoverTypeUnknown() {
        // Set up a method.
        final ITypeKeeper typeKeeper = new TypeKeeper();

        final String code = ""
            + "_method a.b\n"
            + "    _local var << some_object\n"
            + "    var.hover_me()\n"
            + "_endmethod";
        final Position position = new Position(2, 4);  // On `var`.

        // Hover and test.
        final Hover hover = this.provideHover(code, position, typeKeeper);
        final MarkupContent content = hover.getContents().getRight();
        assertThat(content.getKind()).isEqualTo(MarkupKind.MARKDOWN);
        assertThat(content.getValue()).contains("_undefined");
    }

    @Test
    void testProvideHoverAssignedVariable() {
        // Set up a method.
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final TypeString symbolRef = TypeString.ofIdentifier("symbol", "sw");
        final MagikType symbolType = (MagikType) typeKeeper.getType(symbolRef);
        symbolType.setDoc("type_doc");

        final String code = ""
            + "_method a.b\n"
            + "    _local var << :symbol\n"
            + "_endmethod";
        final Position position = new Position(1, 11);  // On `var`.

        // Hover and test.
        final Hover hover = this.provideHover(code, position, typeKeeper);
        final MarkupContent content = hover.getContents().getRight();
        assertThat(content.getKind()).isEqualTo(MarkupKind.MARKDOWN);
        assertThat(content.getValue()).contains("symbol");
        assertThat(content.getValue()).contains("type_doc");
    }

    @Test
    void testBinaryOperatorTimes() {
        // Set up a method.
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        final MagikType integerType = (MagikType) typeKeeper.getType(integerRef);
        integerType.setDoc("type_doc");

        final BinaryOperator binaryOperator =
            new BinaryOperator(BinaryOperator.Operator.STAR, integerRef, integerRef, integerRef);
        typeKeeper.addBinaryOperator(binaryOperator);

        final String code = ""
            + "_method a.b\n"
            + "    _local var << 4 * 4\n"
            + "_endmethod";
        final Position position = new Position(1, 20);  // On `*`.

        // Hover and test.
        final Hover hover = this.provideHover(code, position, typeKeeper);
        final MarkupContent content = hover.getContents().getRight();
        assertThat(content.getKind()).isEqualTo(MarkupKind.MARKDOWN);
        assertThat(content.getValue()).contains("integer");
        assertThat(content.getValue()).contains("type_doc");
    }

}
