package nl.ramsolutions.sw.magik.formatting;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.api.Token;
import java.util.Arrays;
import java.util.Objects;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.api.MagikKeyword;
import nl.ramsolutions.sw.magik.api.MagikPunctuator;

/**
 * Matcher interface for indentation rules.
 *
 * <p>Each matcher determines whether an indentation rule applies to a given token and its context.
 * Matchers are evaluated in order, and the first matching rule determines the indentation.
 *
 * <p>Implementations:
 *
 * <ul>
 *   <li>{@link TokenIsMatcher} - Matches specific keywords or punctuators
 *   <li>{@link ParentIsMatcher} - Matches when the context node is of a specific type
 *   <li>{@link AnyMatcher} - Matches any token (used as fallback)
 * </ul>
 */
public sealed interface NodeMatcher
    permits NodeMatcher.TokenIsMatcher,
        NodeMatcher.ParentIsMatcher,
        NodeMatcher.TokenAndParentMatcher,
        NodeMatcher.AnyMatcher {

  /**
   * Tests whether this matcher matches the given token in its context.
   *
   * @param token The token being indented.
   * @param contextNode The AST node providing context for indentation (typically the parent node
   *     relevant for indentation).
   * @return True if this matcher matches, false otherwise.
   */
  boolean matches(Token token, AstNode contextNode);

  // ========== Factory methods ==========

  /**
   * Creates a matcher that matches when the token is one of the specified keywords.
   *
   * @param keywords The keywords to match.
   * @return A matcher for the specified keywords.
   */
  static TokenIsMatcher tokenIs(final MagikKeyword... keywords) {
    return new TokenIsMatcher(keywords, new MagikPunctuator[0]);
  }

  /**
   * Creates a matcher that matches when the token is one of the specified punctuators.
   *
   * @param punctuators The punctuators to match.
   * @return A matcher for the specified punctuators.
   */
  static TokenIsMatcher tokenIs(final MagikPunctuator... punctuators) {
    return new TokenIsMatcher(new MagikKeyword[0], punctuators);
  }

  /**
   * Creates a matcher that matches when the context node is one of the specified types.
   *
   * @param nodeTypes The node types to match.
   * @return A matcher for the specified node types.
   */
  static ParentIsMatcher parentIs(final AstNodeType... nodeTypes) {
    return new ParentIsMatcher(nodeTypes);
  }

  /**
   * Creates a matcher that matches any token (used as fallback).
   *
   * @return A matcher that always matches.
   */
  static AnyMatcher any() {
    return AnyMatcher.INSTANCE;
  }

  /**
   * Creates a matcher that matches when both the token matches AND the parent matches.
   *
   * @param tokenMatcher The token matcher.
   * @param parentMatcher The parent matcher.
   * @return A combined matcher.
   */
  static TokenAndParentMatcher tokenAndParent(
      final TokenIsMatcher tokenMatcher, final ParentIsMatcher parentMatcher) {
    return new TokenAndParentMatcher(tokenMatcher, parentMatcher);
  }

  // ========== Matcher implementations ==========

  /**
   * Matches when the token is one of the specified keywords or punctuators.
   *
   * <p>This is used for rules like "closing keywords align with parent" where the rule applies to
   * specific tokens like {@code _endblock}, {@code _endif}, {@code )}, {@code }}, etc.
   */
  final class TokenIsMatcher implements NodeMatcher {
    private final MagikKeyword[] keywords;
    private final MagikPunctuator[] punctuators;

    TokenIsMatcher(final MagikKeyword[] keywords, final MagikPunctuator[] punctuators) {
      this.keywords = keywords.clone();
      this.punctuators = punctuators.clone();
    }

    @Override
    public boolean matches(final Token token, final AstNode contextNode) {
      // Check keywords
      if (this.keywords.length > 0) {
        final String[] keywordValues =
            Arrays.stream(this.keywords).map(MagikKeyword::getValue).toArray(String[]::new);
        if (AstQuery.tokenIs(token, keywordValues)) {
          return true;
        }
      }

      // Check punctuators
      if (this.punctuators.length > 0) {
        final String[] punctuatorValues =
            Arrays.stream(this.punctuators).map(MagikPunctuator::getValue).toArray(String[]::new);
        if (AstQuery.tokenIs(token, punctuatorValues)) {
          return true;
        }
      }

      return false;
    }

    /** Get the keywords this matcher checks for. */
    public MagikKeyword[] getKeywords() {
      return this.keywords.clone();
    }

    /** Get the punctuators this matcher checks for. */
    public MagikPunctuator[] getPunctuators() {
      return this.punctuators.clone();
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder("tokenIs(");
      if (this.keywords.length > 0) {
        sb.append(Arrays.toString(this.keywords));
      }
      if (this.punctuators.length > 0) {
        if (this.keywords.length > 0) {
          sb.append(", ");
        }
        sb.append(Arrays.toString(this.punctuators));
      }
      sb.append(")");
      return sb.toString();
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof TokenIsMatcher other)) {
        return false;
      }
      return Arrays.equals(this.keywords, other.keywords)
          && Arrays.equals(this.punctuators, other.punctuators);
    }

    @Override
    public int hashCode() {
      return Objects.hash(Arrays.hashCode(this.keywords), Arrays.hashCode(this.punctuators));
    }
  }

  /**
   * Matches when the context node (parent) is one of the specified types.
   *
   * <p>This is used for rules like "body content is indented from parent" where the rule applies
   * based on the containing construct (e.g., inside a BLOCK, IF, METHOD_DEFINITION, etc.).
   */
  final class ParentIsMatcher implements NodeMatcher {
    private final AstNodeType[] nodeTypes;

    ParentIsMatcher(final AstNodeType[] nodeTypes) {
      this.nodeTypes = nodeTypes.clone();
    }

    @Override
    public boolean matches(final Token token, final AstNode contextNode) {
      if (contextNode == null) {
        return false;
      }
      return contextNode.is(this.nodeTypes);
    }

    /** Get the node types this matcher checks for. */
    public AstNodeType[] getNodeTypes() {
      return this.nodeTypes.clone();
    }

    @Override
    public String toString() {
      return "parentIs(" + Arrays.toString(this.nodeTypes) + ")";
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof ParentIsMatcher other)) {
        return false;
      }
      return Arrays.equals(this.nodeTypes, other.nodeTypes);
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(this.nodeTypes);
    }
  }

  /**
   * Matches when both the token matches AND the parent context matches.
   *
   * <p>This is used for rules that need to check both what token it is and what context it's in.
   * For example, "closing paren in TUPLE aligns with line start" requires checking both that the
   * token is ) and that the parent is TUPLE.
   */
  final class TokenAndParentMatcher implements NodeMatcher {
    private final TokenIsMatcher tokenMatcher;
    private final ParentIsMatcher parentMatcher;

    TokenAndParentMatcher(final TokenIsMatcher tokenMatcher, final ParentIsMatcher parentMatcher) {
      this.tokenMatcher = Objects.requireNonNull(tokenMatcher);
      this.parentMatcher = Objects.requireNonNull(parentMatcher);
    }

    @Override
    public boolean matches(final Token token, final AstNode contextNode) {
      return this.tokenMatcher.matches(token, contextNode)
          && this.parentMatcher.matches(token, contextNode);
    }

    @Override
    public String toString() {
      return "tokenAndParent(" + this.tokenMatcher + ", " + this.parentMatcher + ")";
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof TokenAndParentMatcher other)) {
        return false;
      }
      return this.tokenMatcher.equals(other.tokenMatcher)
          && this.parentMatcher.equals(other.parentMatcher);
    }

    @Override
    public int hashCode() {
      return Objects.hash(this.tokenMatcher, this.parentMatcher);
    }
  }

  /**
   * Matches any token. Used as a fallback rule at the end of the rule list.
   *
   * <p>This ensures that every token gets some indentation handling, even if no specific rule
   * matches.
   */
  final class AnyMatcher implements NodeMatcher {
    static final AnyMatcher INSTANCE = new AnyMatcher();

    private AnyMatcher() {}

    @Override
    public boolean matches(final Token token, final AstNode contextNode) {
      return true;
    }

    @Override
    public String toString() {
      return "any()";
    }
  }
}
