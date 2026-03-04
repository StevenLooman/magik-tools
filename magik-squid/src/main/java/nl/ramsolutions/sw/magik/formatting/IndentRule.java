package nl.ramsolutions.sw.magik.formatting;

import com.sonar.sslr.api.AstNodeType;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import nl.ramsolutions.sw.magik.api.MagikKeyword;

/**
 * An indentation rule that defines how to indent tokens matching a specific pattern.
 *
 * <p>Rules are evaluated in order. The first rule whose matcher returns true for a given token
 * determines the indentation for that token.
 *
 * @param matcher The matcher that determines when this rule applies.
 * @param anchor The anchor type that determines the reference point.
 * @param offsetMultiplier The number of indent levels to add (0 = align, 1 = indent).
 */
public record IndentRule(NodeMatcher matcher, AnchorType anchor, int offsetMultiplier) {

  /** Compact constructor with validation. */
  public IndentRule {
    Objects.requireNonNull(matcher, "matcher cannot be null");
    Objects.requireNonNull(anchor, "anchor cannot be null");
    if (offsetMultiplier < 0) {
      throw new IllegalArgumentException(
          "offsetMultiplier cannot be negative: " + offsetMultiplier);
    }
  }

  /** Create a rule with offset 0 (align with anchor). */
  public static IndentRule align(final NodeMatcher matcher, final AnchorType anchor) {
    return new IndentRule(matcher, anchor, 0);
  }

  /** Create a rule with offset 1 (indent one level from anchor). */
  public static IndentRule indent(final NodeMatcher matcher, final AnchorType anchor) {
    return new IndentRule(matcher, anchor, 1);
  }

  // ===== Bulk rule creation methods =====

  /** Create align rules for multiple keywords with the same anchor. */
  public static List<IndentRule> alignKeywords(
      final AnchorType anchor, final MagikKeyword... keywords) {
    return Arrays.stream(keywords).map(kw -> align(NodeMatcher.tokenIs(kw), anchor)).toList();
  }

  /** Create indent rules for multiple parent node types with the same anchor. */
  public static List<IndentRule> indentParents(
      final AnchorType anchor, final AstNodeType... nodeTypes) {
    return Arrays.stream(nodeTypes)
        .map(type -> indent(NodeMatcher.parentIs(type), anchor))
        .toList();
  }

  /** Create align rules for multiple parent node types with the same anchor. */
  public static List<IndentRule> alignParents(
      final AnchorType anchor, final AstNodeType... nodeTypes) {
    return Arrays.stream(nodeTypes).map(type -> align(NodeMatcher.parentIs(type), anchor)).toList();
  }

  @Override
  public String toString() {
    return "IndentRule["
        + this.matcher
        + " -> "
        + this.anchor
        + " + "
        + this.offsetMultiplier
        + "]";
  }
}
