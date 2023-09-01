package nl.ramsolutions.sw.magik.typedchecks.fixers;

import java.net.URI;
import java.util.List;
import nl.ramsolutions.sw.magik.CodeAction;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.TextEdit;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeKeeper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TypeDocParameterFixer}.
 */
@SuppressWarnings("checkstyle:MagicNumber")
class TypeDocParameterFixerTest {

    private static final String NEWLINE = System.lineSeparator();

    private List<CodeAction> getCodeActions(
            final String code,
            final Range range) {
        final URI uri = URI.create("tests://unittest");
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final MagikTypedFile magikFile = new MagikTypedFile(uri, code, typeKeeper);
        final TypeDocParameterFixer provider = new TypeDocParameterFixer();
        return provider.provideCodeActions(magikFile, range);
    }

    @Test
    void testAddParameter() {
        final String code = ""
            + "_method obj.method(param1, param2)\n"
            + "  ## @param {sw:integer} param1\n"
            + "_endmethod\n";
        final Range range = new Range(
            new Position(0, 0),
            new Position(3, 0));
        final List<CodeAction> codeactions = this.getCodeActions(code, range);
        assertThat(codeactions).hasSize(1);
        final CodeAction codeAction = codeactions.get(0);
        assertThat(codeAction).isEqualTo(
            new CodeAction(
                "Add type-doc for parameter param2",
                new TextEdit(
                    new Range(
                        new Position(3, 0),
                        new Position(3, 0)),
                    "\t## @param {_undefined} param2 Description" + NEWLINE)));
    }

    @Test
    void testRemoveParameter() {
        final String code = ""
            + "_method obj.method(param1)\n"
            + "  ## @param {sw:integer} param1\n"
            + "  ## @param {sw:integer} param3\n"
            + "_endmethod\n";
        final Range range = new Range(
            new Position(0, 0),
            new Position(4, 0));
        final List<CodeAction> codeactions = this.getCodeActions(code, range);
        assertThat(codeactions).hasSize(1);
        final CodeAction codeAction = codeactions.get(0);
        assertThat(codeAction).isEqualTo(
            new CodeAction(
                "Remove type-doc for parameter param3",
                new TextEdit(
                    new Range(
                        new Position(3, 0),
                        new Position(4, 0)),
                    "")));
    }

    @Test
    void testNoChangeParameter() {
        final String code = ""
            + "_method obj.method(param1, param2)\n"
            + "  ## @param {sw:integer} param1\n"
            + "  ## @param {sw:integer} param2\n"
            + "_endmethod\n";
        final Range range = new Range(
            new Position(0, 0),
            new Position(4, 0));
        final List<CodeAction> codeactions = this.getCodeActions(code, range);
        assertThat(codeactions).isEmpty();
    }

}
