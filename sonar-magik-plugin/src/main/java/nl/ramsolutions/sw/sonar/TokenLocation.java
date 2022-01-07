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
        final String[] lines = this.token.getValue().split("\\r\\n|\\n|\\r");
        return this.token.getLine() + lines.length - 1;
    }

    /**
     * Get end column.
     * @return End column.
     */
    public int endColumn() {
        int endLineOffset = this.token.getColumn() + this.token.getValue().length();
        if (endLine() != this.token.getLine()) {
            final String[] lines = this.token.getValue().split("\\r\\n|\\n|\\r");
            endLineOffset = lines[lines.length - 1].length();
        }

        return endLineOffset;
    }

    /**
     * Get value of token.
     * @return Value of token.
     */
    public String getValue() {
        return this.token.getValue();
    }

    /**
     * Get token.
     * @return Token.
     */
    public Token token() {
        return this.token;
    }

}
