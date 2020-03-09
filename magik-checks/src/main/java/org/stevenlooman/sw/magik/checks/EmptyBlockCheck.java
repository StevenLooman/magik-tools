package org.stevenlooman.sw.magik.checks;

import com.sonar.sslr.api.AstNode;
import org.sonar.check.Rule;
import org.stevenlooman.sw.magik.MagikCheck;

@Rule(key = EmptyBlockCheck.CHECK_KEY)
public class EmptyBlockCheck extends MagikCheck {

  private static final String MESSAGE = "Block is empty.";
  public static final String CHECK_KEY = "EmptyBlock";


  @Override
  protected void walkPreBody(AstNode node) {
    boolean hasChildren = node.getChildren().stream()
        .filter(childNode -> !childNode.getTokenValue().trim().isEmpty())
        .anyMatch(childNode -> true);
    if (!hasChildren) {
      AstNode parentNode = node.getParent();
      addIssue(MESSAGE, parentNode);
    }
  }

}
