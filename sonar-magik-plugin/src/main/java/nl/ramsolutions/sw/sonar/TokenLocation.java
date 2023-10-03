package nl.ramsolutions.sw.sonar;

import com.sonar.sslr.api.Token;

/**
 * Token location.
 */
public class TokenLocation {

    private final Token token;

    /**
     * Constructor.
     * @param token Token to wrap.
     */
    public TokenLocation(final Token token) {
        this.token = token;
    }

    /**
     * Get line.
     * @return Line.
     */
    public int line() {
        return this.token.getLine();
    }

    /**
     * Get column.
     * @return Column.
     */
    public int column() {
        return this.token.getColumn();
    }

    /**
     * Get end line.
     * @return End line.
     */
    public int endLine() {
        final String tokenValue = this.token.getOriginalValue();
        final String[] lines = tokenValue.split("\\r\\n|\\n|\\r");
        return this.token.getLine() + lines.length - 1;
    }

    /**
     * Get end column.
     *
     * Try to handle syntax errors (which might end in \r|\n|\r\n) gracefully.
     * @return End column.
     */
    public int endColumn() {
        final String tokenValue = this.token.getOriginalValue().stripTrailing();
        int endLineOffset = this.token.getColumn() + tokenValue.length();
        if (this.endLine() != this.token.getLine()) {
            final String[] lines = tokenValue.split("\\r\\n|\\n|\\r");
            endLineOffset = lines[lines.length - 1].length() - 1;
        }

        return endLineOffset;
    }

    /**
     * Get (original) value of token.
     * @return Value of token.
     */
    public String getValue() {
        return this.token.getOriginalValue();
    }

    /**
     * Get token.
     * @return Token.
     */
    public Token token() {
        return this.token;
    }

}
