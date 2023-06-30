package nl.ramsolutions.sw.magik.checks.checks;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Token;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

/**
 * Check for formatting errors.
 */
// TODO: Can we use FormattingWalker here?
@Rule(key = FormattingCheck.CHECK_KEY)
public class FormattingCheck extends MagikCheck {

    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final String CHECK_KEY = "Formatting";

    private static final String MESSAGE = "Improper formatting: %s.";
    private static final String DEFAULT_INDENT_CHARACTER = "tab";
    private static final int DEFAULT_TAB_WIDTH = 8;
    private static final Set<String> AUGMENTED_ASSIGNMENT_TOKENS = Set.of(
        "_is",
        "_isnt",
        "_andif",
        "_and",
        "_orif",
        "_or",
        "_xor",
        "_div",
        "_mod",
        "_cf",

        "+",
        "-",
        "*",
        "/",
        "**",
        "=",
        "~=");

    /**
     * The character used for indentation (tab/space).
     */
    @RuleProperty(
        key = "indent character",
        description = "The character used for indentation (tab/space)",
        defaultValue = "" + DEFAULT_INDENT_CHARACTER,
        type = "STRING")
    @SuppressWarnings("checkstyle:VisibilityModifier")
    public String indentCharacter = DEFAULT_INDENT_CHARACTER;

    /**
     * The width of a tab character.
     */
    @RuleProperty(
        key = "tab width",
        description = "The width of a tab character",
        defaultValue = "" + DEFAULT_TAB_WIDTH,
        type = "INTEGER")
    @SuppressWarnings("checkstyle:VisibilityModifier")
    public int tabWidth = DEFAULT_TAB_WIDTH;

    private String[] lines;
    private Token previousToken;
    private Token currentToken;
    private Token nextToken;

    @Override
    protected void walkPreMagik(final AstNode node) {
        final MagikFile magikFile = this.getMagikFile();
        this.lines = magikFile.getSourceLines();
        if (this.lines == null) {
            this.lines = new String[]{};
        }

        int lineNo = 1;
        final char indentChar = this.getIndentChar();
        final Pattern pattern = indentChar == '\t'
            ? Pattern.compile("^( +\t+)\\S+|( {" + this.tabWidth + "}).*", Pattern.MULTILINE)
            : Pattern.compile("^(\t).*", Pattern.MULTILINE);
        final String msg = indentChar == '\t'
            ? "Line must start with tabs"
            : "Line must start with spaces";
        for (final String line : this.lines) {
            final Matcher matcher = pattern.matcher(line);
            if (matcher.matches()) {
                final String message = String.format(MESSAGE, msg);
                final String group1 = matcher.group(1);
                final String group = group1 != null ? group1 : matcher.group(2);
                final int endColumn = group.length();
                this.addIssue(lineNo, 1, lineNo, endColumn, message);
            }

            lineNo += 1;
        }
    }

    @Override
    protected void walkPostMagik(final AstNode node) {
        // Process last token.
        this.previousToken = this.currentToken;
        this.currentToken = this.nextToken;
        this.nextToken = null;

        if (this.currentToken != null) {
            this.handleToken();
        }
    }

    @Override
    public void walkToken(final Token token) {
        this.previousToken = this.currentToken;
        this.currentToken = this.nextToken;
        this.nextToken = token;

        if (this.currentToken != null) {
            this.handleToken();
        }
    }

    private void handleToken() {
        // Don't overdo it...
        if (this.currentToken.getType() == GenericTokenType.EOF) {
            return;
        }

        // Don't care about pragma.
        if (this.isPragmaLine(this.currentToken)) {
            return;
        }

        final String value = this.currentToken.getValue();
        switch (value) {
            case ",":
                this.visitTokenComma(this.currentToken);
                break;

            case ".":
                this.visitTokenDot(this.currentToken);
                break;

            case "{", "[", "(":
                this.visitTokenBracketOpen(this.currentToken);
                break;

            case "}", "]", ")":
                this.visitTokenBracketClose(this.currentToken);
                break;

            //case "+":    // can be unary expression
            //case "-":    // can be unary expression
            case "*", "**", "/",
                 "=", "<", "<=", ">", ">=",
                 "_div", "_mod",
                 "_is", "_isnt",
                 "_or", "_orif", "_and", "_andif", "_xor",
                 "_cf":
                if (this.nextToken != null
                    && (this.nextToken.getValue().equals("<<") || this.nextToken.getValue().equals("^<<"))) {
                    // part 1 of augmented assignment
                    this.visitTokenAugmentedAssignmentExpression1(this.currentToken);
                } else {
                    this.visitTokenBinaryOperator(this.currentToken);
                }
                break;

            case "<<", "^<<":
                if (this.previousToken != null
                    && AUGMENTED_ASSIGNMENT_TOKENS.contains(this.previousToken.getValue())) {
                    // part 2 of augmented assignment
                    this.visitTokenAugmentedAssignmentExpression2(this.currentToken);
                } else {
                    this.visitTokenBinaryOperator(this.currentToken);
                }
                break;

            case "$":
                this.visitTokenTransmit(this.currentToken);
                break;

            default:
                break;
        }
    }

    private boolean isPragmaLine(final Token token) {
        final String line = this.getLineFor(token).trim();
        return line.startsWith("_pragma");
    }

    private String getLineFor(final Token token) {
        final int lineNo = token.getLine();
        return this.lines[lineNo - 1];
    }

    private Character charBefore(final Token token) {
        if (this.previousToken != null
            && this.previousToken.getLine() != token.getLine()) {
            return null;
        }

        final String line = this.getLineFor(token);
        int prevColumn = token.getColumn() - 1;
        // special case: '% ', cheat by getting the %
        if (this.previousToken.getValue().equals("% ")) {
            prevColumn -= 1;
        }
        if (prevColumn < 0) {
            return null;
        }
        return line.charAt(prevColumn);
    }

    private Character charAfter(final Token token) {
        final String line = getLineFor(token);
        final int nextColumn = token.getColumn() + token.getValue().length();
        if (line.length() <= nextColumn) {
            return null;
        }
        final char charAfter = line.charAt(nextColumn);
        if (charAfter == '\r') {
            return null;
        }
        return charAfter;
    }

    /**
     * Require a non-whitespace-character before.
     * If only whites-space is encountered before, this is accepted as well.
     * @param token Token to test
     */
    private void requireNonWhitespaceBefore(final Token token) {
        final Character prev = this.charBefore(token);
        if (prev != null
            && Character.isWhitespace(prev)) {
            String msg = String.format(MESSAGE, "no whitespace before allowed");
            this.addIssue(token, msg);
        }
    }

    /**
     * Require a whitespace-character before.
     * @param token Token to test
     */
    private void requireWhitespaceBefore(final Token token) {
        final Character prev = this.charBefore(token);
        if (prev != null
            && !Character.isWhitespace(prev)) {
            final String msg = String.format(MESSAGE, "whitespace before required");
            this.addIssue(token, msg);
        }
    }

    /**
     * Require a non-whitespace-character after.
     * @param token Token to test
     */
    private void requireNonWhitespaceAfter(final Token token) {
        final Character next = this.charAfter(token);
        if (next != null
            && Character.isWhitespace(next)) {
            final String msg = String.format(MESSAGE, "no whitespace after allowed");
            this.addIssue(token, msg);
        }
    }

    /**
     * Require a whitespace-character after.
     * @param token Token to test
     */
    private void requireWhitespaceAfter(final Token token) {
        final Character next = this.charAfter(token);
        if (next != null
            && !Character.isWhitespace(next)) {
            final String msg = String.format(MESSAGE, "whitespace after required");
            this.addIssue(token, msg);
        }
    }

    /**
     * Require an empty line after.
     * @param token Token to test
     */
    private void requireEmptyLineAfter(final Token token) {
        if (this.nextToken != null
            && this.nextToken.getType() != GenericTokenType.EOF
            && this.getLineFor(token).startsWith("$")
            && token.getLine() + 1 == this.nextToken.getLine()) {
            final String msg = String.format(MESSAGE, "empty line after required");
            this.addIssue(token, msg);
        }
    }

    private void visitTokenAugmentedAssignmentExpression1(final Token token) {
        this.requireWhitespaceBefore(token);
        this.requireNonWhitespaceAfter(token);
    }

    private void visitTokenAugmentedAssignmentExpression2(final Token token) {
        this.requireWhitespaceAfter(token);
    }

    private void visitTokenComma(final Token token) {
        this.requireNonWhitespaceBefore(token);
        this.requireWhitespaceAfter(token);
    }

    private void visitTokenDot(final Token token) {
        this.requireNonWhitespaceAfter(token);
    }

    private void visitTokenBracketOpen(final Token token) {
        this.requireNonWhitespaceAfter(token);
    }

    private void visitTokenBracketClose(final Token token) {
        this.requireNonWhitespaceBefore(token);
    }

    private void visitTokenBinaryOperator(final Token token) {
        this.requireWhitespaceBefore(token);
        this.requireWhitespaceAfter(token);
    }

    private void visitTokenTransmit(final Token token) {
        this.requireEmptyLineAfter(token);
    }

    private char getIndentChar() {
        if (this.indentCharacter.equals(DEFAULT_INDENT_CHARACTER)) {
            return '\t';
        }

        return ' ';
    }

}
