package org.stevenlooman.sw.magik.checks;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import org.sonar.check.Rule;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.api.MagikGrammar;

import java.util.Arrays;
import java.util.List;

@Rule(key = EmptyBlockCheck.CHECK_KEY)
public class EmptyBlockCheck extends MagikCheck {

  private static final String MESSAGE = "Block is empty.";
  public static final String CHECK_KEY = "EmptyBlock";

  @Override
  public boolean isTemplatedCheck() {
    return false;
  }

  @Override
  public List<AstNodeType> subscribedTo() {
    return Arrays.asList(
        MagikGrammar.BLOCK,
        MagikGrammar.IF,
        MagikGrammar.ELIF,
        MagikGrammar.ELSE,
        MagikGrammar.LOOP,
        MagikGrammar.PROTECT,
        MagikGrammar.PROTECTION,
        MagikGrammar.TRY,
        MagikGrammar.CATCH,
        MagikGrammar.LOCK);
  }

  @Override
  public void visitNode(AstNode node) {
    List<AstNode> bodyNodes = node.getChildren(MagikGrammar.BODY);
    for (AstNode bodyNode : bodyNodes) {
      boolean hasChildren = bodyNode.getChildren().stream()
          .filter(childNode -> !childNode.getTokenValue().trim().isEmpty())
          .anyMatch(childNode -> true);
      if (!hasChildren) {
        addIssue(MESSAGE, node);
      }
    }
  }

}
