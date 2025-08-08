package nl.ramsolutions.sw.magik.formatting;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.TextEdit;
import org.junit.jupiter.api.Test;

class FormattingProviderTest {

  private List<TextEdit> getEdits(final String code) {
    final FormattingOptions options = new FormattingOptions(8, false, false, true, false);
    return this.getEdits(code, options);
  }

  private List<TextEdit> getEdits(final String code, final FormattingOptions options) {
    final MagikFile magikFile = new MagikFile(MagikTypedFile.DEFAULT_URI, code);
    final FormattingProvider provider = new FormattingProvider(options, RelativeIndentWalker.class);
    final AstNode topNode = magikFile.getTopNode();
    return provider.format(topNode);
  }

  @Test
  void testFormatArguments1() {
    final String code = "call( arg1 , arg2, arg3 )";
    final List<TextEdit> edits = this.getEdits(code);

    assertThat(edits)
        .containsOnly(
            new TextEdit(new Range(new Position(1, 5), new Position(1, 6)), ""),
            new TextEdit(new Range(new Position(1, 10), new Position(1, 11)), ""),
            new TextEdit(new Range(new Position(1, 23), new Position(1, 24)), ""));
  }

  @Test
  void testFormatArguments2() {
    final String code = "call(arg1,arg2,arg3)";
    final List<TextEdit> edits = this.getEdits(code);

    assertThat(edits)
        .containsOnly(
            new TextEdit(new Range(new Position(1, 10), new Position(1, 10)), " "),
            new TextEdit(new Range(new Position(1, 15), new Position(1, 15)), " "));
  }

  @Test
  void testFormatArguments3() {
    final String code = "method.call( arg1 , arg2, arg3 )";
    final List<TextEdit> edits = this.getEdits(code);

    assertThat(edits)
        .containsOnly(
            new TextEdit(new Range(new Position(1, 12), new Position(1, 13)), ""),
            new TextEdit(new Range(new Position(1, 17), new Position(1, 18)), ""),
            new TextEdit(new Range(new Position(1, 30), new Position(1, 31)), ""));
  }

  @Test
  void testFormatArguments4() {
    final String code = "method.call(arg1,arg2,arg3)";
    final List<TextEdit> edits = this.getEdits(code);

    assertThat(edits)
        .containsOnly(
            new TextEdit(new Range(new Position(1, 17), new Position(1, 17)), " "),
            new TextEdit(new Range(new Position(1, 22), new Position(1, 22)), " "));
  }

  @Test
  void testFormatParameters1() {
    final String code = "_proc( arg1 , arg2, arg3 ) _endproc";
    final List<TextEdit> edits = this.getEdits(code);

    assertThat(edits)
        .containsOnly(
            new TextEdit(new Range(new Position(1, 6), new Position(1, 7)), ""),
            new TextEdit(new Range(new Position(1, 11), new Position(1, 12)), ""),
            new TextEdit(new Range(new Position(1, 24), new Position(1, 25)), ""));
  }

  @Test
  void testFormatParameters2() {
    final String code = "_proc(arg1,arg2,arg3) _endproc";
    final List<TextEdit> edits = this.getEdits(code);

    assertThat(edits)
        .containsOnly(
            new TextEdit(new Range(new Position(1, 11), new Position(1, 11)), " "),
            new TextEdit(new Range(new Position(1, 16), new Position(1, 16)), " "));
  }

  @Test
  void testFormatParameters3() {
    final String code = "_method obj.call( arg1 , arg2, arg3 ) _endmethod";
    final List<TextEdit> edits = this.getEdits(code);

    assertThat(edits)
        .containsOnly(
            new TextEdit(new Range(new Position(1, 17), new Position(1, 18)), ""),
            new TextEdit(new Range(new Position(1, 22), new Position(1, 23)), ""),
            new TextEdit(new Range(new Position(1, 35), new Position(1, 36)), ""));
  }

  @Test
  void testFormatParameters4() {
    final String code = "_method obj.call(arg1,arg2,arg3) _endmethod";
    final List<TextEdit> edits = this.getEdits(code);

    assertThat(edits)
        .containsOnly(
            new TextEdit(new Range(new Position(1, 22), new Position(1, 22)), " "),
            new TextEdit(new Range(new Position(1, 27), new Position(1, 27)), " "));
  }

  @Test
  void testFormatPragma1() {
    final String code = "_pragma( classify_level=restricted,usage={ internal})";
    final List<TextEdit> edits = this.getEdits(code);

    assertThat(edits)
        .containsOnly(
            new TextEdit(new Range(new Position(1, 8), new Position(1, 9)), ""),
            new TextEdit(new Range(new Position(1, 35), new Position(1, 35)), " "),
            new TextEdit(new Range(new Position(1, 42), new Position(1, 43)), ""));
  }

  @Test
  void testFormatPragma2() {
    final String code = "_pragma(classify_level=restricted,usage={internal,external})";
    final List<TextEdit> edits = this.getEdits(code);

    assertThat(edits)
        .containsOnly(
            new TextEdit(new Range(new Position(1, 34), new Position(1, 34)), " "),
            new TextEdit(new Range(new Position(1, 50), new Position(1, 50)), " "));
  }

  @Test
  void testFormatMethodInvocation1() {
    final String code = "xxx . yyy(x, y, z)";
    final List<TextEdit> edits = this.getEdits(code);

    assertThat(edits)
        .containsOnly(
            new TextEdit(new Range(new Position(1, 3), new Position(1, 4)), ""),
            new TextEdit(new Range(new Position(1, 5), new Position(1, 6)), ""));
  }

  @Test
  void testFormatMethodInvocation2() {
    final String code = "xxx[x]<<  100";
    final List<TextEdit> edits = this.getEdits(code);

    assertThat(edits)
        .containsOnly(
            new TextEdit(new Range(new Position(1, 6), new Position(1, 6)), " "),
            new TextEdit(new Range(new Position(1, 8), new Position(1, 10)), " "));
  }

  @Test
  void testFormatProcedureInvocation1() {
    final String code = "yyy (x, y, z)";
    final List<TextEdit> edits = this.getEdits(code);

    assertThat(edits)
        .containsOnly(new TextEdit(new Range(new Position(1, 3), new Position(1, 4)), ""));
  }

  @Test
  void testRemoveTrailingWhitespace1() {
    final String code = "show(1)  \n";
    final List<TextEdit> edits = this.getEdits(code);

    assertThat(edits)
        .containsOnly(new TextEdit(new Range(new Position(1, 7), new Position(2, 0)), "\n"));
  }
}
