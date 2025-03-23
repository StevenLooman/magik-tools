package nl.ramsolutions.sw.magik.formatting;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.magik.TextEdit;

/** Abstract formatting strategy. */
abstract class AbstractFormattingStrategy extends FormattingStategy {

  AbstractFormattingStrategy(final FormattingOptions options) {
    super(options);
  }

  List<TextEdit> walkWhitespaceToken(final Token token) {
    return Collections.emptyList();
  }

  List<TextEdit> walkCommentToken(final Token token) {
    return Collections.emptyList();
  }

  List<TextEdit> walkEolToken(final Token token) {
    return Collections.emptyList();
  }

  List<TextEdit> walkEofToken(final Token token) {
    return Collections.emptyList();
  }

  List<TextEdit> walkToken(final Token token) {
    return Collections.emptyList();
  }

  void walkPreNode(final AstNode node) {}

  void walkPostNode(final AstNode node) {}
}
