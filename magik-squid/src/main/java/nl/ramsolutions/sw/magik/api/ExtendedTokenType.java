package nl.ramsolutions.sw.magik.api;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.TokenType;

/**
 * Extended token types.
 */
@SuppressWarnings("checkstyle:JavadocVariable")
public enum ExtendedTokenType implements TokenType {

    WHITESPACE,
    STATEMENT_SEPARATOR,
    OTHER,
    SYNTAX_ERROR;

    @Override
    public String getName() {
        return this.name();
    }

    @Override
    public String getValue() {
        return this.name();
    }

    @Override
    public boolean hasToBeSkippedFromAst(final AstNode node) {
        return false;
    }

}
