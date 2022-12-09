package nl.ramsolutions.sw.magik.ramsolutions.formatting;

import java.net.URI;
import java.util.List;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeKeeper;
import nl.ramsolutions.sw.magik.languageserver.formatting.FormattingProvider;
import org.eclipse.lsp4j.FormattingOptions;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test FormattingProvider.
 */
@SuppressWarnings("checkstyle:MagicNumber")
class FormattingProviderTest {

    private List<TextEdit> getEdits(final String code) {
        final FormattingOptions options = new FormattingOptions();
        return this.getEdits(code, options);
    }

    private List<TextEdit> getEdits(final String code, final FormattingOptions options) {
        final URI uri = URI.create("tests://unittest");
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final MagikTypedFile magikFile = new MagikTypedFile(uri, code, typeKeeper);

        final FormattingProvider provider = new FormattingProvider();
        return provider.provideFormatting(magikFile, options);
    }

    // region: Whitespace
    @Test
    void testWhitespaceMethodDefintion1() {
        final String code = ""
            + "_method a. b(x, y, z)\n"
            + "_endmethod\n";
        final List<TextEdit> edits = this.getEdits(code);

        assertThat(edits).hasSize(1);
    }

    @Test
    void testWhitespaceMethodDefintion2() {
        final String code = ""
            + "_method a.b (x, y, z)\n"
            + "_endmethod\n";
        final List<TextEdit> edits = this.getEdits(code);

        assertThat(edits).hasSize(1);
    }

    @Test
    void testWhitespaceParameters1() {
        final String code = ""
            + "_method a.b(x,y, z)\n"
            + "_endmethod\n";
        final List<TextEdit> edits = this.getEdits(code);

        assertThat(edits).hasSize(1);

        final TextEdit edit0 = edits.get(0);
        final TextEdit expected0 = new TextEdit(
            new Range(
                new Position(0, 14),
                new Position(0, 14)),
            " ");
        assertThat(edit0).isEqualTo(expected0);
    }

    @Test
    void testWhitespaceParameters2() {
        final String code = ""
            + "_method a.b(x, y,z)\n"
            + "_endmethod\n";
        final List<TextEdit> edits = this.getEdits(code);

        assertThat(edits).hasSize(1);

        final TextEdit edit0 = edits.get(0);
        final TextEdit expected0 = new TextEdit(
            new Range(
                new Position(0, 17),
                new Position(0, 17)),
            " ");
        assertThat(edit0).isEqualTo(expected0);
    }

    @Test
    void testWhitespaceParameters3() {
        final String code = ""
            + "_method a.b(x, y , z)\n"
            + "_endmethod\n";
        final List<TextEdit> edits = this.getEdits(code);

        assertThat(edits).hasSize(1);

        final TextEdit edit0 = edits.get(0);
        final TextEdit expected0 = new TextEdit(
            new Range(
                new Position(0, 16),
                new Position(0, 17)),
            "");
        assertThat(edit0).isEqualTo(expected0);
    }

    @Test
    void testWhitespaceParameters4() {
        final String code = "print(a,b, c)\n";
        final List<TextEdit> edits = this.getEdits(code);

        assertThat(edits).hasSize(1);

        final TextEdit edit0 = edits.get(0);
        final TextEdit expected0 = new TextEdit(
            new Range(
                new Position(0, 8),
                new Position(0, 8)),
            " ");
        assertThat(edit0).isEqualTo(expected0);
    }

    @Test
    void testWhitespaceParameters5() {
        final String code = "print(a, b,c)\n";
        final List<TextEdit> edits = this.getEdits(code);

        assertThat(edits).hasSize(1);

        final TextEdit edit0 = edits.get(0);
        final TextEdit expected0 = new TextEdit(
            new Range(
                new Position(0, 11),
                new Position(0, 11)),
            " ");
        assertThat(edit0).isEqualTo(expected0);
    }

    @Test
    void testWhitespaceMethodInvocation1() {
        final String code = "class .method(a, b, c)\n";
        final List<TextEdit> edits = this.getEdits(code);

        assertThat(edits).hasSize(1);

        final TextEdit edit0 = edits.get(0);
        final TextEdit expected0 = new TextEdit(
            new Range(
                new Position(0, 5),
                new Position(0, 6)),
            "");
        assertThat(edit0).isEqualTo(expected0);
    }

    @Test
    void testWhitespaceMethodInvocation2() {
        final String code = "class. method(a, b, c)\n";
        final List<TextEdit> edits = this.getEdits(code);

        assertThat(edits).hasSize(1);

        final TextEdit edit0 = edits.get(0);
        final TextEdit expected0 = new TextEdit(
            new Range(
                new Position(0, 6),
                new Position(0, 7)),
            "");
        assertThat(edit0).isEqualTo(expected0);
    }

    @Test
    void testWhitespaceMethodInvocation3() {
        final String code = "class.method (a, b, c)\n";
        final List<TextEdit> edits = this.getEdits(code);

        assertThat(edits).hasSize(1);

        final TextEdit edit0 = edits.get(0);
        final TextEdit expected0 = new TextEdit(
            new Range(
                new Position(0, 12),
                new Position(0, 13)),
            "");
        assertThat(edit0).isEqualTo(expected0);
    }

    @Test
    void testWhitespaceArguments1() {
        final String code = "prc( a, b, c)\n";
        final List<TextEdit> edits = this.getEdits(code);

        assertThat(edits).hasSize(1);

        final TextEdit edit0 = edits.get(0);
        final TextEdit expected0 = new TextEdit(
            new Range(
                new Position(0, 4),
                new Position(0, 5)),
            "");
        assertThat(edit0).isEqualTo(expected0);
    }

    @Test
    void testWhitespaceArguments2() {
        final String code = "prc(a,b, c)\n";
        final List<TextEdit> edits = this.getEdits(code);

        assertThat(edits).hasSize(1);

        final TextEdit edit0 = edits.get(0);
        final TextEdit expected0 = new TextEdit(
            new Range(
                new Position(0, 6),
                new Position(0, 6)),
            " ");
        assertThat(edit0).isEqualTo(expected0);
    }

    @Test
    void testWhitespaceArguments3() {
        final String code = "prc(a, b,c)\n";
        final List<TextEdit> edits = this.getEdits(code);

        assertThat(edits).hasSize(1);

        final TextEdit edit0 = edits.get(0);
        final TextEdit expected0 = new TextEdit(
            new Range(
                new Position(0, 9),
                new Position(0, 9)),
            " ");
        assertThat(edit0).isEqualTo(expected0);
    }

    @Test
    void testWhitespaceArguments4() {
        final String code = "prc(a, b , c)\n";
        final List<TextEdit> edits = this.getEdits(code);

        assertThat(edits).hasSize(1);

        final TextEdit edit0 = edits.get(0);
        final TextEdit expected0 = new TextEdit(
            new Range(
                new Position(0, 8),
                new Position(0, 9)),
            "");
        assertThat(edit0).isEqualTo(expected0);
    }

    @Test
    void testWhitespaceArgumentsSelf() {
        final String code = "prc(_self)\n";
        final List<TextEdit> edits = this.getEdits(code);

        assertThat(edits).isEmpty();
    }

    @Test
    void testWhitespaceMethodInvocationMultiLine() {
        final String code = ""
            + "obj.\n"
            + "m()\n";
        final List<TextEdit> edits = this.getEdits(code);

        assertThat(edits).hasSize(1);

        final TextEdit edit0 = edits.get(0);
        final TextEdit expected0 = new TextEdit(
            new Range(
                new Position(1, 0),
                new Position(1, 0)),
            "\t");
        assertThat(edit0).isEqualTo(expected0);
    }

    @Test
    void testWhitespaceSimpleVector() {
        final String code = ""
            + "{:slot1, _unset, :readable, :public}";

        final List<TextEdit> edits = this.getEdits(code);
        assertThat(edits).isEmpty();
    }

    @Test
    void testWhitespaceAssignmentMethod() {
        final String code = ""
            + "_self.x() << 10";

        final List<TextEdit> edits = this.getEdits(code);
        assertThat(edits).isEmpty();
    }
    // endregion

    // region: Indenting.
    @Test
    void testIndentBlockStatement() {
        final String code = ""
            + "_block\n"
            + "print(1)\n"
            + "_endblock\n";
        final List<TextEdit> edits = this.getEdits(code);

        assertThat(edits).hasSize(1);

        final TextEdit edit0 = edits.get(0);
        final TextEdit expected0 = new TextEdit(
            new Range(
                new Position(1, 0),
                new Position(1, 0)),
            "\t");
        assertThat(edit0).isEqualTo(expected0);
    }

    @Test
    void testIndentComments() {
        final String code = ""
            + "_block\n"
            + "# comment\n"
            + "_endblock\n";
        final List<TextEdit> edits = this.getEdits(code);

        assertThat(edits).hasSize(1);

        final TextEdit edit0 = edits.get(0);
        final TextEdit expected0 = new TextEdit(
            new Range(
                new Position(1, 0),
                new Position(1, 0)),
            "\t");
        assertThat(edit0).isEqualTo(expected0);
    }

    @Test
    void testIndentCommentsAfterStatement() {
        final String code = ""
            + "_method a.b(a, b, c)\n"
            + "\tprint(1) # test method\n"
            + "_endmethod\n";
        final List<TextEdit> edits = this.getEdits(code);

        assertThat(edits).isEmpty();
    }

    @Test
    void testIndentNewlineExpression() {
        final String code = ""
            + "_if a() _andif\n"
            + "b()\n"
            + "_then _endif\n";
        final List<TextEdit> edits = this.getEdits(code);

        assertThat(edits).hasSize(1);

        final TextEdit edit0 = edits.get(0);
        final TextEdit expected0 = new TextEdit(
            new Range(
                new Position(1, 0),
                new Position(1, 0)),
            "\t");
        assertThat(edit0).isEqualTo(expected0);
    }

    @Test
    void testIndentAssignmentExpression() {
        final String code = ""
            + "_block\n"
            + "a << 2\n"
            + "_endblock\n";
        final List<TextEdit> edits = this.getEdits(code);

        assertThat(edits).hasSize(1);

        final TextEdit edit0 = edits.get(0);
        final TextEdit expected0 = new TextEdit(
            new Range(
                new Position(1, 0),
                new Position(1, 0)),
            "\t");
        assertThat(edit0).isEqualTo(expected0);
    }

    @Test
    void testIndentAssignmentExpression2() {
        final String code = ""
            + "a << _if x?\n"
            + "_then\n"
            + "  >> 1\n"
            + "_else\n"
            + "  >> 2\n"
            + "_endif";
        final List<TextEdit> edits = this.getEdits(code);

        assertThat(edits).hasSize(5);

        final TextEdit edit0 = edits.get(0);
        final TextEdit expected0 = new TextEdit(
            new Range(
                new Position(1, 0),
                new Position(1, 0)),
            "\t");
        assertThat(edit0).isEqualTo(expected0);

        final TextEdit edit1 = edits.get(1);
        final TextEdit expected1 = new TextEdit(
            new Range(
                new Position(2, 0),
                new Position(2, 2)),
            "\t\t");
        assertThat(edit1).isEqualTo(expected1);

        final TextEdit edit2 = edits.get(2);
        final TextEdit expected2 = new TextEdit(
            new Range(
                new Position(3, 0),
                new Position(3, 0)),
            "\t");
        assertThat(edit2).isEqualTo(expected2);

        final TextEdit edit3 = edits.get(3);
        final TextEdit expected3 = new TextEdit(
            new Range(
                new Position(4, 0),
                new Position(4, 2)),
            "\t\t");
        assertThat(edit3).isEqualTo(expected3);

        final TextEdit edit4 = edits.get(4);
        final TextEdit expected4 = new TextEdit(
            new Range(
                new Position(5, 0),
                new Position(5, 0)),
            "\t");
        assertThat(edit4).isEqualTo(expected4);
    }

    @Test
    void testIndentArguments() {
        final String code = ""
            + "def_slotted_exemplar(\n"
            + "\t:test_ex,\n"
            + "\t{\n"
            + "\t\t{:slot1, _unset}\n"
            + "\t})\n";
        final List<TextEdit> edits = this.getEdits(code);
        assertThat(edits).isEmpty();
    }

    @Test
    void testIndentIfElif() {
        final String code = ""
            + "_if a\n"
            + "_then\n"
            + "\tshow(:a)\n"
            + "_elif b\n"
            + "_then\n"
            + "\tshow(:b)\n"
            + "_else\n"
            + "\tshow(:c)\n"
            + "_endif\n";
        final List<TextEdit> edits = this.getEdits(code);
        assertThat(edits).isEmpty();
    }

    @Test
    void testIndentVariableDefinitionAssignment() {
        final String code = ""
            + "_local a <<\n"
            + "\t10";
        final List<TextEdit> edits = this.getEdits(code);
        assertThat(edits).isEmpty();
    }
    // endregion

    // region: Comments
    @Test
    void testCommentsLineBefore() {
        final String code = ""
            + " # comment 1\n"
            + "a\n";
        final List<TextEdit> edits = this.getEdits(code);

        assertThat(edits).hasSize(1);

        final TextEdit edit0 = edits.get(0);
        final TextEdit expected0 = new TextEdit(
            new Range(
                new Position(0, 0),
                new Position(0, 1)),
            "");
        assertThat(edit0).isEqualTo(expected0);
    }

    @Test
    void testCommentsLineAfter() {
        final String code = ""
            + "a\n"
            + " # comment 1\n";
        final List<TextEdit> edits = this.getEdits(code);

        assertThat(edits).hasSize(1);

        final TextEdit edit0 = edits.get(0);
        final TextEdit expected0 = new TextEdit(
            new Range(
                new Position(1, 0),
                new Position(1, 1)),
            "");
        assertThat(edit0).isEqualTo(expected0);
    }
    // endregion

    // region: Trimming
    @Test
    void testNoTrimTrailingWhitespaceStatement() {
        final String code = "a    \n";
        final FormattingOptions options = new FormattingOptions();
        options.setTrimTrailingWhitespace(false);
        final List<TextEdit> edits = this.getEdits(code, options);

        assertThat(edits).isEmpty();
    }

    @Test
    void testTrimTrailingWhitespaceStatement() {
        final String code = "a  \n";
        final FormattingOptions options = new FormattingOptions();
        options.setTrimTrailingWhitespace(true);
        final List<TextEdit> edits = this.getEdits(code, options);

        assertThat(edits).hasSize(1);

        final TextEdit edit0 = edits.get(0);
        final TextEdit expected0 = new TextEdit(
            new Range(
                new Position(0, 1),
                new Position(0, 3)),
            "");
        assertThat(edit0).isEqualTo(expected0);
    }

    @Test
    void testNoTrimTrailingWhitespaceComment() {
        final String code = "# comment  \n";
        final FormattingOptions options = new FormattingOptions();
        options.setTrimTrailingWhitespace(false);
        final List<TextEdit> edits = this.getEdits(code, options);

        assertThat(edits).isEmpty();
    }

    @Test
    void testTrimTrailingWhitespaceComment() {
        final String code = "# comment  \n";
        final FormattingOptions options = new FormattingOptions();
        options.setTrimTrailingWhitespace(true);
        final List<TextEdit> edits = this.getEdits(code, options);

        assertThat(edits).hasSize(1);

        final TextEdit edit0 = edits.get(0);
        final TextEdit expected0 = new TextEdit(
            new Range(
                new Position(0, 9),
                new Position(0, 11)),
            "");
        assertThat(edit0).isEqualTo(expected0);
    }
    // endregion

    // region: Final newlines
    @Test
    void testInsertFinalNewlinePresent() {
        final String code = "1 + 1\n";
        final FormattingOptions options = new FormattingOptions();
        options.setInsertFinalNewline(true);
        final List<TextEdit> edits = this.getEdits(code, options);

        assertThat(edits).isEmpty();
    }

    @Test
    void testInsertFinalNewlineAbsent() {
        final String code = "1 + 1";
        final FormattingOptions options = new FormattingOptions();
        options.setInsertFinalNewline(true);
        final List<TextEdit> edits = this.getEdits(code, options);

        assertThat(edits).hasSize(1);

        final TextEdit edit0 = edits.get(0);
        final TextEdit expected0 = new TextEdit(
            new Range(
                new Position(0, 5),
                new Position(0, 5)),
            "\n");
        assertThat(edit0).isEqualTo(expected0);
    }

    @Test
    void testTrimFinalNewlinesAbsent() {
        final String code = "1 + 1";
        final FormattingOptions options = new FormattingOptions();
        options.setTrimFinalNewlines(true);
        final List<TextEdit> edits = this.getEdits(code, options);

        assertThat(edits).isEmpty();
    }

    @Test
    void testTrimFinalNewlinesPresent() {
        final String code = ""
            + "1 + 1\n"
            + "\n";
        final FormattingOptions options = new FormattingOptions();
        options.setTrimFinalNewlines(true);
        final List<TextEdit> edits = this.getEdits(code, options);

        assertThat(edits).hasSize(1);

        final TextEdit edit0 = edits.get(0);
        final TextEdit expected0 = new TextEdit(
            new Range(
                new Position(1, 5),
                new Position(3, 0)),
            "");
        assertThat(edit0).isEqualTo(expected0);
    }
    // endregion

    @Test
    void testFormattingCrLf() {
        final String code = ""
            + "# comment  \r\n"
            + "_block\r\n"
            + "a.do()\r\n"
            + "_endblock\r\n";
        final FormattingOptions options = new FormattingOptions();
        options.setTrimTrailingWhitespace(true);
        final List<TextEdit> edits = this.getEdits(code, options);

        assertThat(edits).hasSize(2);

        // Trim whitespace after comment.
        final TextEdit edit0 = edits.get(0);
        final TextEdit expected0 = new TextEdit(
            new Range(
                new Position(0, 9),
                new Position(0, 11)),
            "");
        assertThat(edit0).isEqualTo(expected0);

        // Indent statements in block.
        final TextEdit edit1 = edits.get(1);
        final TextEdit expected1 = new TextEdit(
            new Range(
                new Position(2, 0),
                new Position(2, 0)),
            "\t");
        assertThat(edit1).isEqualTo(expected1);
    }

    // region: Pragma
    @Test
    void testFormatPragma() {
        final String code = "_pragma(a=b,c=d,e={f,g})\n";
        final List<TextEdit> edits = this.getEdits(code);

        assertThat(edits).hasSize(3);

        final TextEdit edit0 = edits.get(0);
        final TextEdit expected0 = new TextEdit(
            new Range(
                new Position(0, 12),
                new Position(0, 12)),
            " ");
        assertThat(edit0).isEqualTo(expected0);

        final TextEdit edit1 = edits.get(1);
        final TextEdit expected1 = new TextEdit(
            new Range(
                new Position(0, 16),
                new Position(0, 16)),
            " ");
        assertThat(edit1).isEqualTo(expected1);

        final TextEdit edit2 = edits.get(2);
        final TextEdit expected2 = new TextEdit(
            new Range(
                new Position(0, 21),
                new Position(0, 21)),
            " ");
        assertThat(edit2).isEqualTo(expected2);
    }

    @Test
    void testFormatPragma2() {
        final String code = "_pragma(a=b, c=d, e={f, g})\n";
        final List<TextEdit> edits = this.getEdits(code);

        assertThat(edits).isEmpty();
    }
    // endregion

    // region: Expressions
    @Test
    void testUnaryExpression() {
        final String code = "a+-2\n";
        final List<TextEdit> edits = this.getEdits(code);

        assertThat(edits).hasSize(2);

        final TextEdit edit0 = edits.get(0);
        final TextEdit expected0 = new TextEdit(
            new Range(
                new Position(0, 1),
                new Position(0, 1)),
            " ");
        assertThat(edit0).isEqualTo(expected0);

        final TextEdit edit1 = edits.get(1);
        final TextEdit expected1 = new TextEdit(
            new Range(
                new Position(0, 2),
                new Position(0, 2)),
            " ");
        assertThat(edit1).isEqualTo(expected1);
    }

    @Test
    void testParenExpression() {
        final String code = "( a _andif b )";
        final List<TextEdit> edits = this.getEdits(code);

        assertThat(edits).hasSize(2);

        final TextEdit edit0 = edits.get(0);
        final TextEdit expected0 = new TextEdit(
            new Range(
                new Position(0, 1),
                new Position(0, 2)),
            "");
        assertThat(edit0).isEqualTo(expected0);

        final TextEdit edit1 = edits.get(1);
        final TextEdit expected1 = new TextEdit(
            new Range(
                new Position(0, 12),
                new Position(0, 13)),
            "");
        assertThat(edit1).isEqualTo(expected1);
    }

    @Test
    void testBinaryExpression() {
        final String code = "a+b\n";
        final List<TextEdit> edits = this.getEdits(code);

        assertThat(edits).hasSize(2);

        final TextEdit edit0 = edits.get(0);
        final TextEdit expected0 = new TextEdit(
            new Range(
                new Position(0, 1),
                new Position(0, 1)),
            " ");
        assertThat(edit0).isEqualTo(expected0);

        final TextEdit edit1 = edits.get(1);
        final TextEdit expected1 = new TextEdit(
            new Range(
                new Position(0, 2),
                new Position(0, 2)),
            " ");
        assertThat(edit1).isEqualTo(expected1);
    }

    @Test
    void testBinaryExpressionMultiple() {
        final String code = ""
            + "_if a? _andif\n"
            + "\tb? _andif\n"
            + "\tc?\n"
            + "_then\n"
            + "\tdo()\n"
            + "_endif\n";
        final List<TextEdit> edits = this.getEdits(code);

        assertThat(edits).isEmpty();
    }

    @Test
    void testSlotExpression() {
        final String code = "1 + . slot";
        final List<TextEdit> edits = this.getEdits(code);

        assertThat(edits).hasSize(1);

        final TextEdit edit0 = edits.get(0);
        final TextEdit expected0 = new TextEdit(
            new Range(
                new Position(0, 5),
                new Position(0, 6)),
            "");
        assertThat(edit0).isEqualTo(expected0);
    }
    // endregion

    @Test
    void testLabel() {
        final String code = "@label";
        final List<TextEdit> edits = this.getEdits(code);

        assertThat(edits).isEmpty();
    }

}
