package org.stevenlooman.sw.magik.analysis;

import com.sonar.sslr.api.AstNode;

import java.util.List;

public class AstCompare {

  private AstCompare() {

  }

  public static boolean AstNodesEquals(AstNode left, AstNode right) {
    if (left.getType() != right.getType()) {
      return false;
    }

    String rightTokenValue = right.getTokenValue();
    String leftTokenValue = left.getTokenValue();
    if (leftTokenValue == null || rightTokenValue == null &&
        leftTokenValue != rightTokenValue) {
      return false;
    }

    if (!leftTokenValue.equals(rightTokenValue)) {
      return false;
    }

    List<AstNode> leftChildren = left.getChildren();
    List<AstNode> rightChildren = right.getChildren();
    if (leftChildren.size() != rightChildren.size()) {
      return false;
    }

    for (int i = 0; i < leftChildren.size(); ++i) {
      AstNode leftChild = leftChildren.get(i);
      AstNode rightChild = rightChildren.get(i);
      if (!AstNodesEquals(leftChild, rightChild)) {
        return false;
      }
    }

    return true;
  }

}
