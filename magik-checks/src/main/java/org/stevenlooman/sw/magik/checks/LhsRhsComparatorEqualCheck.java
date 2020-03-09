package org.stevenlooman.sw.magik.checks;

import com.sonar.sslr.api.AstNode;
import org.sonar.check.Rule;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.analysis.AstCompare;

@Rule(key = LhsRhsComparatorEqualCheck.CHECK_KEY)
public class LhsRhsComparatorEqualCheck extends MagikCheck {

  private static final String MESSAGE =
      "Left hand side and right hand side of comparator are equal.";
  public static final String CHECK_KEY = "LhsRhsComparatorEqual";

  @Override
  protected void walkPreOrExpression(AstNode node) {
    checkComparison(node);
  }

  @Override
  protected void walkPreXorExpression(AstNode node) {
    checkComparison(node);
  }

  @Override
  protected void walkPreAndExpression(AstNode node) {
    checkComparison(node);
  }

  @Override
  protected void walkPreEqualityExpression(AstNode node) {
    checkComparison(node);
  }

  @Override
  protected void walkPreRelationalExpression(AstNode node) {
    checkComparison(node);
  }

  private void checkComparison(AstNode node) {
    AstNode leftHandSide = node.getFirstChild();
    AstNode rightHandSide = node.getLastChild();
    if (AstCompare.astNodeEqualsRecursive(leftHandSide, rightHandSide)) {
      addIssue(MESSAGE, node);
    }
  }

}
