package nl.ramsolutions.sw.magik.formatting;

import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Token;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import nl.ramsolutions.sw.magik.TextEdit;
import nl.ramsolutions.sw.magik.analysis.AstQuery;

class TrailingWhitespaceStrategy extends FormattingStategy {

  TrailingWhitespaceStrategy(final FormattingOptions options) {
    super(options);
  }

  @CheckForNull
  TextEdit walkToken(final Token token) {
    if (!this.options.isTrimTrailingWhitespace()) {
      return null;
    }

    if (!AstQuery.tokenIs(this.lastToken, GenericTokenType.WHITESPACE)) {
      return null;
    }

    return this.editToken(this.lastToken, "", "no whitespace after allowed");
  }
}
