package nl.ramsolutions.sw.magik.checks.checks;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.api.MagikNumberParser;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import org.sonar.check.Rule;

/** Check if `=` should be used where `_is` was used. */
@Rule(key = UseValueCompareCheck.CHECK_KEY)
public class UseValueCompareCheck extends MagikCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "UseValueCompare";

  private static final String MESSAGE = "Type '%s' should not be compare with _is.";
  // Note that SW4 uses bignum from 1<<29. SW5 uses bignum from 1<<31.
  private static final int BIGNUM_LIMIT = 1 << 29;

  @Override
  protected void walkPreEqualityExpression(final AstNode node) {
    if (this.isInstanceCompare(node) && (this.hasStringLiteral(node) || this.hasNumLiteral(node))) {
      final String message = String.format(MESSAGE, "string");
      this.addIssue(node, message);
    }
  }

  private boolean isInstanceCompare(final AstNode node) {
    return node.getChildren().get(1).getTokenValue().equals("_is")
        || node.getChildren().get(1).getTokenValue().equals("_isnt");
  }

  private boolean hasStringLiteral(final AstNode node) {
    final List<AstNode> children = node.getChildren();
    final AstNode left = children.get(0);
    final AstNode right = children.get(2);
    return left.is(MagikGrammar.ATOM) && left.getFirstChild(MagikGrammar.STRING) != null
        || right.is(MagikGrammar.ATOM) && right.getFirstChild(MagikGrammar.STRING) != null;
  }

  private boolean hasNumLiteral(final AstNode node) {
    final List<AstNode> children = node.getChildren();
    final AstNode left = children.get(0);
    final AstNode right = children.get(2);
    return this.sideHasNumLiteral(left) || this.sideHasNumLiteral(right);
  }

  private boolean sideHasNumLiteral(final AstNode node) {
    if (node.isNot(MagikGrammar.ATOM)) {
      return false;
    }

    final AstNode numberNode = node.getFirstChild(MagikGrammar.NUMBER);
    if (numberNode == null) {
      return false;
    }

    final String tokenValue = numberNode.getTokenValue();
    final Number number = MagikNumberParser.parseMagikNumberSafe(tokenValue);
    if (number instanceof final Integer numberInt) {
      return numberInt > BIGNUM_LIMIT;
    } else if (number instanceof final Long numberLong) {
      return numberLong > BIGNUM_LIMIT;
    } else if (number instanceof Double) {
      return true;
    }

    return false;
  }
}
