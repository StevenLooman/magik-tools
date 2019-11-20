package org.stevenlooman.sw.magik.checks;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.api.Token;

import org.sonar.check.Rule;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.MagikVisitorContext;
import org.stevenlooman.sw.magik.parser.MagikParser;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;

@Rule(key = FormattingCheck.CHECK_KEY)
public class FormattingCheck extends MagikCheck {
  public static final String CHECK_KEY = "Formatting";
  private static final String MESSAGE = "Improper formatting: %s.";
  private static final Integer TAB_WIDTH = 8;

  private String[] lines;
  private Token previousToken;
  private Token currentToken;
  private Token nextToken;

  private static final Set<String> AUGMENTED_ASSIGNMENT_TOKENS = new HashSet<String>(
      Arrays.asList(
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
        "~="
  ));

  @Override
  public boolean isTemplatedCheck() {
    return false;
  }

  @Override
  public List<AstNodeType> subscribedTo() {
    return Arrays.asList();
  }

  @Override
  public void visitFile(@Nullable AstNode node) {
    MagikVisitorContext context = getContext();
    lines = context.fileContentLines();
    if (lines == null) {
      lines = new String[]{};
    }

    int lineNo = 1;
    for (String line : lines) {
      if (line.matches("^[ ]{" + TAB_WIDTH + "}.*")
          || line.matches("^[ ]+[\t]+.*")) {
        String message = String.format(MESSAGE, "Line must start with tabs");
        addIssue(message, lineNo, 1);
      }
      lineNo += 1;
    }
  }

  @Override
  public void leaveFile(@Nullable AstNode node) {
    // process last token
    previousToken = currentToken;
    currentToken = nextToken;
    nextToken = null;

    if (currentToken != null) {
      handleToken();
    }
  }

  @Override
  public void visitToken(Token token) {
    if (token.getType() == MagikParser.UtilityTokenType.SYNTAX_ERROR) {
      return;
    }

    previousToken = currentToken;
    currentToken = nextToken;
    nextToken = token;

    if (currentToken != null) {
      handleToken();
    }
  }

  private void handleToken() {
    // don't care about pragma
    if (isPragmaLine(currentToken)) {
      return;
    }

    String value = currentToken.getValue();
    switch (value) {
      case ",":
        visitTokenComma(currentToken);
        break;

      case ".":
        visitTokenDot(currentToken);
        break;

      case "{":
      case "[":
      case "(":
        visitTokenBracketOpen(currentToken);
        break;

      case "}":
      case "]":
      case ")":
        visitTokenBracketClose(currentToken);
        break;

      //case "+":  // can be unary expression
      //case "-":  // can be unary expression
      case "*":
      case "**":
      case "/":
      case "=":
      case "<":
      case "<=":
      case ">":
      case ">=":
      case "_div":
      case "_mod":
      case "_is":
      case "_isnt":
      case "_or":
      case "_orif":
      case "_and":
      case "_andif":
      case "_xor":
      case "_cf":
        if (nextToken != null
            && (nextToken.getValue().equals("<<")
                || nextToken.getValue().equals("^<<"))) {
          // part 1 of augmented assignment
          visitTokenAugmentedAssignmentExpression1(currentToken);
        } else {
          visitTokenBinaryOperator(currentToken);
        }
        break;

      case "<<":
      case "^<<":
        if (previousToken != null
            && AUGMENTED_ASSIGNMENT_TOKENS.contains(previousToken.getValue())) {
          // part 2 of augmented assignment
          visitTokenAugmentedAssignmentExpression2(currentToken);
        } else {
          visitTokenBinaryOperator(currentToken);
        }
        break;

      case "$":
        visitTokenTransmit(currentToken);
        break;

      default:
        break;
    }
  }

  private boolean isPragmaLine(Token token) {
    String line = getLineFor(token).trim();
    return line.startsWith("_pragma");
  }

  private String getLineFor(Token token) {
    int lineNo = token.getLine();
    return lines[lineNo - 1];
  }

  private Character charBefore(Token token) {
    if (previousToken != null
        && previousToken.getLine() != token.getLine()) {
      return null;
    }

    String line = getLineFor(token);
    int prevColumn = token.getColumn() - 1;
    // special case: '% ', cheat by getting the %
    if (previousToken.getValue().equals("% ")) {
      prevColumn -= 1;
    }
    if (prevColumn < 0) {
      return null;
    }
    return line.charAt(prevColumn);
  }

  private Character charAfter(Token token) {
    String line = getLineFor(token);
    int nextColumn = token.getColumn() + token.getValue().length();
    if (line.length() <= nextColumn) {
      return null;
    }
    char charAfter = line.charAt(nextColumn);
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
  private void requireNonWhitespaceBefore(Token token) {
    Character prev = charBefore(token);
    if (prev != null
        && Character.isWhitespace(prev)) {
      String msg = String.format(MESSAGE, "no whitespace before allowed");
      addIssue(msg, token);
    }
  }

  /**
   * Require a whitespace-character before.
   * @param token Token to test
   */
  private void requireWhitespaceBefore(Token token) {
    Character prev = charBefore(token);
    if (prev != null
        && !Character.isWhitespace(prev)) {
      String msg = String.format(MESSAGE, "whitespace before required");
      addIssue(msg, token);
    }
  }

  /**
   * Require a non-whitespace-character after.
   * @param token Token to test
   */
  private void requireNonWhitespaceAfter(Token token) {
    Character next = charAfter(token);
    if (next != null
        && Character.isWhitespace(next)) {
      String msg = String.format(MESSAGE, "no whitespace after allowed");
      addIssue(msg, token);
    }
  }

  /**
   * Require a whitespace-character after.
   * @param token Token to test
   */
  private void requireWhitespaceAfter(Token token) {
    Character next = charAfter(token);
    if (next != null
        && !Character.isWhitespace(next)) {
      String msg = String.format(MESSAGE, "whitespace after required");
      addIssue(msg, token);
    }
  }

  /**
   * Require an empty line after.
   * @param token Token to test
   */
  private void requireEmptyLineAfter(Token token) {
    if (nextToken != null
        && getLineFor(token).equals("$")
        && token.getLine() + 1 == nextToken.getLine()) {
      String msg = String.format(MESSAGE, "empty line after required");
      addIssue(msg, token);
    }
  }

  private void visitTokenAugmentedAssignmentExpression1(Token token) {
    requireWhitespaceBefore(token);
    requireNonWhitespaceAfter(token);
  }

  private void visitTokenAugmentedAssignmentExpression2(Token token) {
    requireWhitespaceAfter(token);
  }

  private void visitTokenComma(Token token) {
    requireNonWhitespaceBefore(token);
    requireWhitespaceAfter(token);
  }

  private void visitTokenDot(Token token) {
    requireNonWhitespaceAfter(token);
  }

  private void visitTokenBracketOpen(Token token) {
    requireNonWhitespaceAfter(token);
  }

  private void visitTokenBracketClose(Token token) {
    requireNonWhitespaceBefore(token);
  }

  private void visitTokenBinaryOperator(Token token) {
    requireWhitespaceBefore(token);
    requireWhitespaceAfter(token);
  }

  private void visitTokenTransmit(Token token) {
    requireEmptyLineAfter(token);
  }

}
