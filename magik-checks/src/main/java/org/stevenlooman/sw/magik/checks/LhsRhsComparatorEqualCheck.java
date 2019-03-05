package org.stevenlooman.sw.magik.checks;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import org.sonar.check.Rule;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.analysis.AstCompare;
import org.stevenlooman.sw.magik.api.MagikGrammar;

import java.util.Arrays;
import java.util.List;

@Rule(key = LhsRhsComparatorEqualCheck.CHECK_KEY)
public class LhsRhsComparatorEqualCheck extends MagikCheck {

  private static final String MESSAGE = "Left hand side and right hand side of comparator are equal.";
  public static final String CHECK_KEY = "LhsRhsComparatorEqual";

  @Override
  public List<AstNodeType> subscribedTo() {
    return Arrays.asList(
        MagikGrammar.OR_EXPRESSION,
        MagikGrammar.XOR_EXPRESSION,
        MagikGrammar.AND_EXPRESSION,
        MagikGrammar.EQUALITY_EXPRESSION,
        MagikGrammar.RELATIONAL_EXPRESSION);
  }

  @Override
  public void visitNode(AstNode node) {
    AstNode leftHandSide = node.getFirstChild();
    AstNode rightHandSide = node.getLastChild();
    if (AstCompare.AstNodesEquals(leftHandSide, rightHandSide)) {
      addIssue(MESSAGE, node);
    }
  }

}
