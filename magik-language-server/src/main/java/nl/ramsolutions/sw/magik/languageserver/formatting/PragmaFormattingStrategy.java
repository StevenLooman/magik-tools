package nl.ramsolutions.sw.magik.languageserver.formatting;

import com.sonar.sslr.api.Token;
import org.eclipse.lsp4j.FormattingOptions;
import org.eclipse.lsp4j.TextEdit;

/**
 * Pragma formatting strategy.
 */
class PragmaFormattingStrategy extends FormattingStrategy {

    PragmaFormattingStrategy(final FormattingOptions options) {
        super(options);
    }

    @Override
    public TextEdit walkWhitespaceToken(final Token token) {
        TextEdit textEdit = null;
        if (this.lastTextToken != null
            && this.lastTextToken.getOriginalValue().equals(",")) {
            if (!token.getOriginalValue().equals(" ")) {
                // Require whitespace after ",".
                textEdit = this.editToken(token, " ");
            }
        } else {
            // Pragma's don't have whitespace otherwise.
            textEdit = this.editToken(token, "");
        }
        return textEdit;
    }

    @Override
    TextEdit walkEolToken(final Token token) {
        // Pragma's don't have newlines.
        return this.editToken(token, "");
    }

    @Override
    TextEdit walkToken(final Token token) {
        TextEdit textEdit = null;
        if (this.lastToken != null
            && this.lastToken.getOriginalValue().equals(",")) {
            textEdit = this.insertBeforeToken(token, " ");
        }
        return textEdit;
    }

}
