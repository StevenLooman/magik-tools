package org.stevenlooman.sw.magik.analysis;

import com.sonar.sslr.api.AstNode;

import java.util.List;

public class AstCompare {

  private AstCompare() {

  }

  /**
   * Test if two nodes are equal to each other.
   * Compares structure and token values.
   * @param left Node to compare
   * @param right Node to compare
   * @return True if nodes are equal, false otherwise.
   */
  public static boolean astNodesEquals(AstNode left, AstNode right) {
    if (left.getType() != right.getType()) {
      return false;
    }

    String rightTokenValue = right.getTokenValue();
    String leftTokenValue = left.getTokenValue();
    if (leftTokenValue == null || rightTokenValue == null
        && leftTokenValue != rightTokenValue) {
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
      if (!astNodesEquals(leftChild, rightChild)) {
        return false;
      }
    }

    return true;
  }

}
