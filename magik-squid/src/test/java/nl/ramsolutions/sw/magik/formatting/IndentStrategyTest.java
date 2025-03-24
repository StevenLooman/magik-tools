package nl.ramsolutions.sw.magik.formatting;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import java.util.ArrayList;
import java.util.List;
import nl.ramsolutions.sw.magik.TextEdit;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.MagikAstWalker;
import nl.ramsolutions.sw.magik.parser.MagikParser;

/**
 * Base test class for {@link IndentStrategy}s.
 */
class IndentStrategyTest {

  protected List<TextEdit> getEdits(final String code) {
    final FormattingOptions options = new FormattingOptions(8, false, false, false, false);
    return this.getEdits(code, options);
  }

  protected List<TextEdit> getEdits(final String code, final FormattingOptions options) {
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
              if (trivia.isComment()) {
                this.walkToken(token);
              }

              strategy.setLastToken(token);
              if (!AstQuery.tokenIs(
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
            if (!AstQuery.tokenIs(
                token, GenericTokenType.WHITESPACE, GenericTokenType.EOL, GenericTokenType.EOF)) {
              this.lastTextToken = token;
            }
          }
        };
    walker.walkAst(topNode);

    return edits;
  }
}
