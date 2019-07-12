package org.stevenlooman.sw.magik.checks;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.api.Token;
import org.sonar.check.Rule;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.MagikVisitorContext;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;

@Rule(key = FormattingCheck.CHECK_KEY)
public class FormattingCheck extends MagikCheck {
  public static final String CHECK_KEY = "FormattingCheck";
  private static final String MESSAGE = "Improper formatting: %s.";

  private String[] lines;
  private Token lastToken;

  private static final Set<String> AUGMENTED_ASSIGNMENT_TOKENS = new HashSet<String>() {
    {
      add("_is");
      add("_isnt");
      add("_andif");
      add("_and");
      add("_orif");
      add("_or");
      add("_xor");
      add("_div");
      add("_mod");
      add("_cf");

      add("+");
      add("-");
      add("*");
      add("/");
      add("**");
      add("=");
      add("~=");
    }
  };

  @Override
  public List<AstNodeType> subscribedTo() {
    return Arrays.asList();
  }

  @Override
  public void visitFile(@Nullable AstNode node) {
    MagikVisitorContext context = getContext();
    String fileContents = context.fileContent();
    if (fileContents != null) {
      lines = fileContents.split("\n");
    }
  }

  @Override
  public void visitToken(Token token) {
    if (lines == null) {
      return;
    }

    // don't care about pragma
    if (isPragmaLine(token)) {
      return;
    }

    String value = token.getValue();
    switch (value) {
      case ",":
        visitTokenComma(token);
        break;

      case ".":
        visitTokenDot(token);
        break;

      case "{":
      case "[":
      case "(":
        visitTokenBracketOpen(token);
        break;

      case "}":
      case "]":
      case ")":
        visitTokenBracketClose(token);
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
        visitTokenBinaryOperator(token);
        break;

      case "<<":
      case "^<<":
        if (isAugmentedAssignmentExpression(token)) {
          visitTokenAugmentedAssignmentExpression(token);
        } else {
          visitTokenBinaryOperator(token);
        }
        break;

      default:
        break;
    }

    lastToken = token;
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
    if (lastToken != null
        && lastToken.getLine() != token.getLine()) {
      return null;
    }

    String line = getLineFor(token);
    int prevColumn = token.getColumn() - 1;
    // special case: '% ', cheat by getting the %
    if (lastToken.getValue().equals("% ")) {
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

  private boolean isAugmentedAssignmentExpression(Token token) {
    String previousTokenValue = lastToken.getValue();
    return AUGMENTED_ASSIGNMENT_TOKENS.contains(previousTokenValue);
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
      addIssue(msg, token.getLine(), token.getColumn() + token.getOriginalValue().length());
    }
  }

  /**
   * Require a whitespace-character after
   * @param token Token to test
   */
  private void requireWhitespaceAfter(Token token) {
    Character next = charAfter(token);
    if (next != null
        && !Character.isWhitespace(next)) {
      String msg = String.format(MESSAGE, "whitespace after required");
      addIssue(msg, token.getLine(), token.getColumn() + token.getOriginalValue().length());
    }
  }

  private void visitTokenAugmentedAssignmentExpression(Token token) {
    requireNonWhitespaceBefore(token);
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

}
