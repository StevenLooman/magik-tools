package nl.ramsolutions.sw.magik.formatting;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.TokenType;
import com.sonar.sslr.api.Trivia;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.TextEdit;
import nl.ramsolutions.sw.magik.analysis.MagikAstWalker;
import nl.ramsolutions.sw.magik.parser.MagikParser;
import org.junit.jupiter.api.Test;

class TabbedIndentStrategyTest {

  static boolean tokenIs(final @Nullable Token token, final TokenType... types) {
    if (token == null) {
      return false;
    }

    return Stream.of(types).anyMatch(type -> token.getType() == type);
  }

  private List<TextEdit> getEdits(final String code, final FormattingOptions options) {
    final List<TextEdit> edits = new ArrayList<>();
    final IndentStrategy strategy = new TabbedIndentStrategy(options);
    final MagikParser parser = new MagikParser();
    final AstNode topNode = parser.parse(code);
    final MagikAstWalker walker =
        new MagikAstWalker() {

          private AstNode currentNode;
          private Token lastTextToken;

          @Override
          protected void walkPreDefault(final AstNode node) {
            this.currentNode = node;
          }

          @Override
          protected void walkPostDefault(AstNode node) {
            this.currentNode = this.currentNode.getParent();
          }

          @Override
          protected void walkTrivia(Trivia trivia) {
            for (final Token token : trivia.getTokens()) {
              strategy.setLastToken(token);
              if (!tokenIs(
                  token, GenericTokenType.WHITESPACE, GenericTokenType.EOL, GenericTokenType.EOF)) {
                this.lastTextToken = token;
              }
            }
          }

          @Override
          protected void walkToken(final Token token) {
            // Mimic MagikFormattingStrategy, calling only indentFor() on new lines.
            if (this.lastTextToken != null && !token.isOnSameLineThan(this.lastTextToken)) {
              final TextEdit edit = strategy.ensureIndenting(token, this.currentNode);
              if (edit != null) {
                edits.add(edit);
              }
            }

            strategy.setLastToken(token);
            if (!tokenIs(
                token, GenericTokenType.WHITESPACE, GenericTokenType.EOL, GenericTokenType.EOF)) {
              this.lastTextToken = token;
            }
          }
        };
    walker.walkAst(topNode);

    return edits;
  }

  @Test
  void test1() {
    final FormattingOptions options = new FormattingOptions(8, false, false, false, false);
    final String code =
        """
        _proc()
        	_self()
        _endproc
        """;

    final List<TextEdit> edits = this.getEdits(code, options);
    assertThat(edits).isEmpty();
  }
}
