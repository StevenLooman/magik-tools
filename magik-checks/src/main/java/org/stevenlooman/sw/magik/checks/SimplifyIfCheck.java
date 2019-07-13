package org.stevenlooman.sw.magik.checks;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import org.sonar.check.Rule;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.analysis.AstQuery;
import org.stevenlooman.sw.magik.api.MagikGrammar;

import java.util.Arrays;
import java.util.List;

@Rule(key = SimplifyIfCheck.CHECK_KEY)
public class SimplifyIfCheck extends MagikCheck {
  public static final String CHECK_KEY = "SimplifyIf";
  private static final String MESSAGE =
      "You can simplify this if by using _elif or combining guards..";

  @Override
  public List<AstNodeType> subscribedTo() {
    return Arrays.asList(
        MagikGrammar.IF
    );
  }

  @Override
  public void visitNode(AstNode node) {
    testIfIf(node);
    testIfElseIf(node);
  }

  private void testIfIf(AstNode node) {
    // only one statement in if body
    List<AstNode> bodyNodes = AstQuery.getFromChain(node, MagikGrammar.BODY);
    if (bodyNodes.size() != 1) {
      return;
    }
    AstNode bodyNode = bodyNodes.get(0);
    if (bodyNode.getChildren().size() != 1) {
      return;
    }

    // statement is an if statement
    List<AstNode> bodyIfNodes = AstQuery.getFromChain(bodyNode,
        MagikGrammar.STATEMENT, MagikGrammar.EXPRESSION_STATEMENT, MagikGrammar.EXPRESSION, MagikGrammar.ATOM, MagikGrammar.IF);
    if (bodyIfNodes.size() != 1) {
      return;
    }

    // has no elif or else
    AstNode ifNode = bodyIfNodes.get(0);
    List<AstNode> elifNodes = ifNode.getChildren(MagikGrammar.ELIF);
    if (!elifNodes.isEmpty()) {
      return;
    }
    AstNode elseNode = ifNode.getFirstChild(MagikGrammar.ELSE);
    if (elseNode != null) {
      return;
    }

    addIssue(MESSAGE, ifNode);
  }

  private void testIfElseIf(AstNode node) {
    // only one statement in else body
    List<AstNode> bodyNodes = AstQuery.getFromChain(node, MagikGrammar.ELSE, MagikGrammar.BODY);
    if (bodyNodes.size() != 1) {
      return;
    }
    AstNode bodyNode = bodyNodes.get(0);
    if (bodyNode.getChildren().size() != 1) {
      return;
    }

    // statement is an if statement
    List<AstNode> elseIfNodes = AstQuery.getFromChain(bodyNode,
        MagikGrammar.STATEMENT, MagikGrammar.EXPRESSION_STATEMENT, MagikGrammar.EXPRESSION, MagikGrammar.ATOM, MagikGrammar.IF);
    if (elseIfNodes.size() != 1) {
      return;
    }

    AstNode elseIfNode = elseIfNodes.get(0);
    addIssue(MESSAGE, elseIfNode);
  }

}
