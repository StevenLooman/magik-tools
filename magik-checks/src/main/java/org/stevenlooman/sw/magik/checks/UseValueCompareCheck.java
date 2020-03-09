package org.stevenlooman.sw.magik.checks;

import com.sonar.sslr.api.AstNode;
import org.sonar.check.Rule;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.api.MagikGrammar;

import java.util.List;

@Rule(key = UseValueCompareCheck.CHECK_KEY)
public class UseValueCompareCheck extends MagikCheck {
  private static final String MESSAGE = "Type '%s' should not be compare with _is.";
  public static final String CHECK_KEY = "UseValueCompare";

  @Override
  protected void walkPreEqualityExpression(AstNode node) {
    if (isInstanceCompare(node)
        && (hasStringLiteral(node) || hasBigNumLiteral(node))) {
      String message = String.format(MESSAGE, "string");
      addIssue(message, node);
    }
  }

  private boolean isInstanceCompare(AstNode node) {
    return node.getChildren().get(1).getTokenValue().equals("_is")
           || node.getChildren().get(1).getTokenValue().equals("_isnt");
  }

  private boolean hasStringLiteral(AstNode node) {
    List<AstNode> children = node.getChildren();
    AstNode left = children.get(0);
    AstNode right = children.get(2);
    return left.getType() == MagikGrammar.ATOM
           && left.getFirstChild(MagikGrammar.STRING) != null
           || right.getType() == MagikGrammar.ATOM
           && right.getFirstChild(MagikGrammar.STRING) != null;
  }

  private boolean hasBigNumLiteral(AstNode node) {
    List<AstNode> children = node.getChildren();
    AstNode left = children.get(0);
    AstNode right = children.get(2);
    return (left.getType() == MagikGrammar.ATOM
            && left.getFirstChild(MagikGrammar.NUMBER) != null
            && !left.getFirstChild().getTokenValue().contains(".")
            && Long.parseLong(left.getFirstChild().getTokenValue()) > 1 << 29)
           || (right.getType() == MagikGrammar.ATOM
               && right.getFirstChild().getType() == MagikGrammar.NUMBER
               && !right.getFirstChild().getTokenValue().contains(".")
               && Long.parseLong(right.getFirstChild().getTokenValue()) > 1 << 29);
  }

}
