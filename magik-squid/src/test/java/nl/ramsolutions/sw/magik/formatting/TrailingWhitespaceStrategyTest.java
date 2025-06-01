package nl.ramsolutions.sw.magik.formatting;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.TextEdit;
import nl.ramsolutions.sw.magik.analysis.MagikAstWalker;
import nl.ramsolutions.sw.magik.parser.MagikParser;
import org.junit.jupiter.api.Test;

class TrailingWhitespaceStrategyTest {

  protected List<TextEdit> getEdits(final String code, final FormattingOptions options) {
    final List<TextEdit> edits = new ArrayList<>();
    final TrailingWhitespaceStrategy strategy = new TrailingWhitespaceStrategy(options);
    final MagikParser parser = new MagikParser();
    final AstNode topNode = parser.parse(code);
    final MagikAstWalker walker =
        new MagikAstWalker() {

          @Override
          protected void walkTrivia(Trivia trivia) {
            for (final Token token : trivia.getTokens()) {
              if (trivia.isComment()) {
                final String comment = token.getOriginalValue();
                final String trimmedComment = comment.stripTrailing();
                if (comment.length() != trimmedComment.length()) {
                  final Token commentToken =
                      Token.builder(token).setValueAndOriginalValue(trimmedComment).build();
                  this.walkToken(commentToken);

                  final String trimmed = comment.substring(trimmedComment.length());
                  final Token whitespaceToken =
                      Token.builder(token)
                          .setValueAndOriginalValue(trimmed)
                          .setColumn(token.getColumn() + trimmedComment.length())
                          .setType(GenericTokenType.WHITESPACE)
                          .build();
                  this.walkToken(whitespaceToken);
                }
              } else if (trivia.isSkippedText()) {
                if (token.getType() == GenericTokenType.EOL) {
                  this.walkToken(token);
                } else if (token.getType() == GenericTokenType.WHITESPACE) {
                  this.walkToken(token);
                }
              }
            }
          }

          @Override
          protected void walkToken(final Token token) {
            final TextEdit textEdit = strategy.walkToken(token);
            edits.add(textEdit);

            strategy.setLastToken(token);
          }
        };
    walker.walkAst(topNode);

    return edits.stream().filter(Objects::nonNull).toList();
  }

  @Test
  void testNoTrimTrailingWhitespaceStatement() {
    final String code = "a    \n";
    final FormattingOptions options = new FormattingOptions(8, false, false, false, false);
    final List<TextEdit> edits = this.getEdits(code, options);
    assertThat(edits).isEmpty();
  }

  @Test
  void testTrimTrailingWhitespaceStatement() {
    final String code = "a  \n";
    final FormattingOptions options = new FormattingOptions(8, false, false, true, false);
    final List<TextEdit> edits = this.getEdits(code, options);
    assertThat(edits)
        .containsExactly(
            new TextEdit(
                new Range(new Position(1, 1), new Position(1, 3)),
                "",
                "no whitespace after allowed"));
  }

  @Test
  void testNoTrimTrailingWhitespaceComment() {
    final String code = "# comment  \n";
    final FormattingOptions options = new FormattingOptions(8, false, false, false, false);
    final List<TextEdit> edits = this.getEdits(code, options);
    assertThat(edits).isEmpty();
  }

  @Test
  void testTrimTrailingWhitespaceComment() {
    final String code = "# comment  \n";
    final FormattingOptions options = new FormattingOptions(8, false, false, true, false);
    final List<TextEdit> edits = this.getEdits(code, options);
    assertThat(edits)
        .containsExactly(
            new TextEdit(
                new Range(new Position(1, 9), new Position(1, 11)),
                "",
                "no whitespace after allowed"));
  }
}
