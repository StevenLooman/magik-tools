package org.stevenlooman.sw.magik.analysis;

import com.sonar.sslr.api.AstNode;

import org.stevenlooman.sw.magik.api.MagikGrammar;

import java.util.EnumSet;
import java.util.List;

public class AstCompare {

  /**
   * Flags to influence the compare functionality.
   * Values:
   *   {{IDENTIFIER_IGNORE_NAME}}: Ignore the name of the IDENTIFIER node.
   *   {{ONLY_AST}}: Only compare AST, ignoring tokens such as '(' and ')'.
   */
  enum Flags {
    IGNORE_IDENTIFIER_NAME,
  }

  private AstCompare() {

  }

  /**
   * Test if two nodes are equal to each other.
   * Compares token values and structure.
   * @param left Node to compare
   * @param right Node to compare
   * @return True if nodes are equal, false otherwise.
   */
  public static boolean astNodeEqualsRecursive(AstNode left, AstNode right) {
    EnumSet<Flags> flags = EnumSet.noneOf(Flags.class);
    return astNodeEqualsRecursive(left, right, flags);
  }

  /**
   * Test if two nodes are equal to each other.
   * Compares token values and structure.
   * @param left Node to compare
   * @param right Node to compare
   * @param flags Flags to influence comparison
   * @return True if nodes are equal, false otherwise.
   */
  public static boolean astNodeEqualsRecursive(AstNode left, AstNode right, EnumSet<Flags> flags) {
    // Compare nodes.
    if (!astNodeEquals(left, right, flags)) {
      return false;
    }

    // Compare children of nodes.
    List<AstNode> leftChildren = left.getChildren();
    List<AstNode> rightChildren = right.getChildren();
    if (leftChildren.size() != rightChildren.size()) {
      return false;
    }

    for (int i = 0; i < leftChildren.size(); ++i) {
      AstNode leftChild = leftChildren.get(i);
      AstNode rightChild = rightChildren.get(i);
      if (!astNodeEqualsRecursive(leftChild, rightChild, flags)) {
        return false;
      }
    }

    return true;
  }

  /**
   * Test if two nodes are equal to each other.
   * Compares token values.
   * @param left Node to compare
   * @param right Node to compare
   * @return True if nodes are equal, false otherwise.
   */
  public static boolean astNodeEquals(AstNode left, AstNode right) {
    EnumSet<Flags> flags = EnumSet.noneOf(Flags.class);
    return astNodeEquals(left, right, flags);
  }

  /**
   * Test if two nodes are equal to each other.
   * Compares token values.
   * @param left Node to compare
   * @param right Node to compare
   * @param flags Flags to influence comparison
   * @return True if nodes are equal, false otherwise.
   */
  public static boolean astNodeEquals(AstNode left, AstNode right, EnumSet<Flags> flags) {
    // Ensure same type.
    if (left.getType() != right.getType()) {
      return false;
    }

    // Ensure both have a value, or neither has a value.
    String leftTokenValue = left.getTokenValue();
    String rightTokenValue = right.getTokenValue();
    if (leftTokenValue == null && rightTokenValue != null
        || leftTokenValue != null && rightTokenValue == null) {
      return false;
    }

    if (flags.contains(Flags.IGNORE_IDENTIFIER_NAME)
        && isIdentifierNode(left)) {
      // Don't compare IDENTIFIERS; continue
    } else if (leftTokenValue != null && !leftTokenValue.equals(rightTokenValue)) {
      // Values have to be the same.
      return false;
    }

    return true;
  }

  private static boolean isIdentifierNode(AstNode node) {
    if (node.getType() == MagikGrammar.IDENTIFIER
        || (node.getParent() != null && node.getParent().getType() == MagikGrammar.IDENTIFIER)) {
      return true;
    }

    // Ensure there is something to recurse down to.
    if (!node.hasChildren()) {
      return false;
    }

    return isIdentifierNode(node.getFirstChild());
  }

}
