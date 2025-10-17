package nl.ramsolutions.sw.magik.analysis;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.TokenType;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.api.MagikOperator;
import nl.ramsolutions.sw.magik.api.MagikPunctuator;
import nl.ramsolutions.sw.moduledef.api.ModuleDefinitionGrammar;
import nl.ramsolutions.sw.productdef.api.ProductDefinitionGrammar;

/** {@link AstNode} query utility functions. */
public final class AstQuery {

  private AstQuery() {}

  /**
   * Test if {@link Token} value is one of the given values.
   *
   * @param token Token to test.
   * @param values Values to test against.
   * @return True if token has value, otherwise false.
   */
  public static boolean tokenIs(final @Nullable Token token, final String... values) {
    if (token == null) {
      return false;
    }

    final Set<String> valuesSet = Set.of(values);
    final String tokenValue = token.getOriginalValue().toLowerCase();
    return valuesSet.contains(tokenValue);
  }

  /**
   * Test if {@link Token} is of one of the given types.
   *
   * @param token Token to test.
   * @param types Types to test against.
   * @return True if token is of type, otherwise false.
   */
  public static boolean tokenIs(final @Nullable Token token, final TokenType... types) {
    if (token == null) {
      return false;
    }

    return Stream.of(types).anyMatch(type -> token.getType() == type);
  }

  /**
   * Test if {@link Token} starts with one of the given values.
   *
   * @param token Token to test.
   * @param values Values to test against.
   * @return True if token is of type and starts with value, otherwise false.
   */
  public static boolean tokenStartsWith(final @Nullable Token token, final String... values) {
    if (token == null) {
      return false;
    }

    final String tokenValue = token.getOriginalValue().toLowerCase();
    return Arrays.stream(values).map(String::toLowerCase).anyMatch(tokenValue::startsWith);
  }

  /**
   * Test if {@link Token}s of {@link AstNode} are on the same line.
   *
   * @param node1 {@link AstNode} to test.
   * @param node2 {@link AstNode} to test.
   * @return True if both {@link AstNode}s have a {@link Token} and {@link Token}s are on the same
   *     line, false otherwise.
   */
  public static boolean isOnSameLineThan(final AstNode node1, final @Nullable AstNode node2) {
    if (node2 == null) {
      return false;
    }

    final Token token1 = node1.getToken();
    final Token token2 = node2.getToken();
    if (token1 == null || token2 == null) {
      return false;
    }

    return token1.isOnSameLineThan(token2);
  }

  /**
   * Test of the parent of {@link node} is of one of the given types.
   *
   * @param node Node to get/test parent from.
   * @param types Types to test against.
   * @return True if node is of type, false otherwise.
   */
  public static boolean parentIs(final AstNode node, final AstNodeType... types) {
    final AstNode parentNode = node.getParent();
    if (parentNode == null) {
      return false;
    }

    return parentNode.is(types);
  }

  /**
   * Get self or any ancestor of {@link AstNode} which are of one of the given types, up to the
   * given stop type.
   *
   * <p>If node is of any of the given types, it is returned.
   *
   * @param node {@link AstNode} to query
   * @param stopType {@link AstNodeType} to stop at.
   * @param nodeTypes {@link AstNodeType}s to match
   * @return {@link AstNode} which matches query, {@code null} if not found.
   */
  @CheckForNull
  public static AstNode getSelfOrAncestorUpTo(
      final AstNode node, final AstNodeType stopType, final AstNodeType... nodeTypes) {
    AstNode current = node;
    while (current != null) {
      if (current.is(nodeTypes)) {
        return current;
      }

      if (current.is(stopType)) {
        return null;
      }

      current = current.getParent();
    }

    return null;
  }

  /**
   * Get the first ancestor of {@link AstNode} which is of one of the given types.
   *
   * @param node {@link AstNode} to query
   * @param nodeTypes {@link AstNodeType}s to match
   * @return {@link AstNode} which matches query, {@code null} if not found.
   */
  @CheckForNull
  public static AstNode getFirstAncestor(final AstNode node, final AstNodeType[]... nodeTypes) {
    final AstNodeType[] flattenedNodeTypes =
        Arrays.stream(nodeTypes).flatMap(Arrays::stream).distinct().toArray(AstNodeType[]::new);
    return node.getFirstAncestor(flattenedNodeTypes);
  }

  /**
   * Get the first child {@link Token} which is any of the given values (case ignored).
   *
   * @param node Node to check.
   * @param tokenValue {@link Token} value to check for.
   * @return The first child {@link Token} which is any of the given values (case ignored).
   */
  @CheckForNull
  public static Token getFirstChildToken(final AstNode node, final String... tokenValue) {
    // final Set<String> tokenValues = Set.of(tokenValue);
    // return node.getChildren().stream()
    //     .map(AstNode::getToken)
    //     .filter(Objects::nonNull)
    //     .filter(
    //         token -> {
    //           final String lowerTokenValue = token.getValue().toLowerCase();
    //           return tokenValues.contains(lowerTokenValue);
    //         })
    //     .findAny()
    //     .orElse(null);
    final List<Token> matchingChildTokens = AstQuery.getChildTokens(node, tokenValue);
    if (matchingChildTokens.isEmpty()) {
      return null;
    }

    return matchingChildTokens.get(0);
  }

  /**
   * Get the first child {@link Token} which is any of the given values (case ignored).
   *
   * @param node Node to check.
   * @param tokenValue {@link Token} value to check for.
   * @return The first child {@link Token} which is any of the given values (case ignored).
   */
  @CheckForNull
  public static List<Token> getChildTokens(final AstNode node, final String... tokenValue) {
    final Set<String> tokenValues = Set.of(tokenValue);
    return node.getChildren().stream()
        .filter(AstQuery::isTokenNode)
        .map(AstNode::getToken)
        .filter(
            token -> {
              final String lowerTokenValue = token.getValue().toLowerCase();
              return tokenValues.contains(lowerTokenValue);
            })
        .toList();
  }

  /**
   * Test if any child token is a specific {@link MagikOperator}.
   *
   * @param node Node to check.
   * @param operator Operator to check for.
   * @return True if any child token is the given operator, false otherwise.
   */
  public static boolean anyChildTokenIs(final AstNode node, final MagikOperator operator) {
    return AstQuery.getFirstChildToken(node, operator.getValue()) != null;
    // return node.getChildren().stream()
    //     .filter(childNode -> childNode.isNot(MagikGrammar.values()))
    //     .map(AstNode::getTokenValue)
    //     .anyMatch(tokenValue -> tokenValue.equalsIgnoreCase(operator.getValue()));
  }

  /**
   * Test if any child token is a specific {@link MagikPunctuator}.
   *
   * @param node Node to check.
   * @param punctuator Punctuator to check for.
   * @return True if any child token is the given punctuator, false otherwise.
   */
  public static boolean anyChildTokenIs(final AstNode node, final MagikPunctuator punctuator) {
    return AstQuery.getFirstChildToken(node, punctuator.getValue()) != null;
    // return node.getChildren().stream()
    //     .filter(childNode -> childNode.isNot(MagikGrammar.values()))
    //     .map(AstNode::getTokenValue)
    //     .anyMatch(tokenValue -> tokenValue.equalsIgnoreCase(punctuator.getValue()));
  }

  /**
   * Get the AstNodes which match a chain of {@link AstNodeType}s.
   *
   * @param node {@link AstNode} to query
   * @param nodeTypes Chain of {@link AstNodeType}s
   * @return {@link AstNode}s which match query
   */
  public static List<AstNode> getChildrenFromChain(
      final AstNode node, final AstNodeType... nodeTypes) {
    List<AstNode> nodes = List.of(node);
    // loop over chain
    for (final AstNodeType nodeType : nodeTypes) {
      // of current item in chain, find all children of so-far-matching nodes
      final List<AstNode> currentChildren = new ArrayList<>();
      for (final AstNode currentNode : nodes) {
        final List<AstNode> currentNodeChildren = currentNode.getChildren(nodeType);
        currentChildren.addAll(currentNodeChildren);
      }
      // loop
      nodes = currentChildren;
    }

    return nodes;
  }

  /**
   * Get the first AstNode which matches a chain of {@link AstNodeTypes}s. Tries to get {@code
   * getChildNode()} on each node, for each {@code nodeTypes}.
   *
   * @param node {@link AstNode} to query
   * @param nodeTypes Chain of {@link AstNodeType}s
   * @return {@link AstNode} which matches query, {@code null} if none is found.
   */
  @CheckForNull
  public static AstNode getFirstChildFromChain(final AstNode node, final AstNodeType... nodeTypes) {
    AstNode current = node;
    for (final AstNodeType nodeType : nodeTypes) {
      current = current.getFirstChild(nodeType);
      if (current == null) {
        return null;
      }
    }
    return current;
  }

  /**
   * Get a child {@link AstNode} from chain, but only if each node has one child and the type.
   *
   * @param node {@link AstNode} to query
   * @param nodeTypes Chain of {@link AstNodeType}s
   * @return {@link AstNode} which matches query, {@code null} if not is found.
   */
  @CheckForNull
  public static AstNode getOnlyFromChain(final AstNode node, final AstNodeType... nodeTypes) {
    AstNode current = node;
    for (final AstNodeType nodeType : nodeTypes) {
      if (current.getChildren().size() != 1) {
        return null;
      }

      current = current.getFirstChild(nodeType);
      if (current == null) {
        return null;
      }
    }
    return current;
  }

  /**
   * Get a parent {@link AstNode} from chain, but only if each node matches the type.
   *
   * @param node {@link AstNode} to query.
   * @param nodeTypes Chain of {@link AstNodeType}s.
   * @return {@link AstNode} which matches query, {@code null} if not found.
   */
  @CheckForNull
  public static AstNode getParentFromChain(final AstNode node, final AstNodeType... nodeTypes) {
    AstNode current = node;
    for (final AstNodeType nodeType : nodeTypes) {
      current = current.getParent();
      if (current == null || !current.is(nodeType)) {
        return null;
      }
    }
    return current;
  }

  /**
   * Get the node in {@code topNode} before {@code position}.
   *
   * @param topNode Top node.
   * @param position Position for node.
   * @return Token-Node before position.
   */
  @CheckForNull
  public static AstNode nodeBefore(final AstNode topNode, final Position position) {
    final List<AstNode> nodes =
        AstQuery.dfs(topNode)
            .filter(AstQuery::isTokenNode)
            .filter(
                node -> {
                  final Token token = node.getToken();
                  final Position endPosition = Position.fromTokenEnd(token);
                  return endPosition.isBefore(position) || endPosition.equals(position);
                })
            .toList();
    if (nodes.isEmpty()) {
      return null;
    }

    return nodes.get(nodes.size() - 1);
  }

  /**
   * Get the token node in {@code topNode} at {@code position}.
   *
   * @param topNode Top node.
   * @param position Position for node.
   * @return Token node at position.
   */
  @CheckForNull
  public static AstNode nodeAt(final AstNode topNode, final Position position) {
    final List<AstNode> nodes =
        AstQuery.dfs(topNode)
            .filter(AstQuery::isTokenNode)
            .filter(
                node -> {
                  final Token token = node.getToken();
                  final Range range = new Range(token);
                  final Position rangeEndPosition = range.getEndPosition();
                  return !range.positionIsBeforeSelf(position)
                      && !range.positionIsAfterSelf(position)
                      && !position.equals(rangeEndPosition);
                })
            .toList();
    if (nodes.isEmpty()) {
      return null;
    }

    return nodes.get(0);
  }

  /**
   * Get the (token) node in {@code node} at {@code position} of a specific type.
   *
   * @param topNode Top node.
   * @param position Position for node.
   * @param nodeTypes Node type to look for.
   * @return Token-Node at position.
   */
  @CheckForNull
  public static AstNode nodeAt(
      final AstNode topNode, final Position position, final AstNodeType... nodeTypes) {
    final AstNode node = AstQuery.nodeAt(topNode, position);
    if (node == null) {
      return null;
    }

    final AstNode parentNode = node.getParent();
    if (parentNode.isNot(nodeTypes)) {
      return null;
    }

    return node;
  }

  /**
   * Get the node in {@code topNode} after {@code position}.
   *
   * @param topNode Top node.
   * @param position Position for node.
   * @return Node after position.
   */
  @CheckForNull
  public static AstNode nodeAfter(final AstNode topNode, final Position position) {
    final List<AstNode> nodes =
        AstQuery.dfs(topNode)
            .filter(AstQuery::isTokenNode)
            .filter(
                node -> {
                  final Token token = node.getToken();
                  final Position startPosition = Position.fromTokenStart(token);
                  final Position endPosition = Position.fromTokenEnd(token);
                  return position.equals(startPosition) || position.isBefore(endPosition);
                })
            .toList();
    if (nodes.isEmpty()) {
      return null;
    }

    return nodes.get(0);
  }

  /**
   * Get the {@link AstNode} surrounding {@code position}.
   *
   * @param topNode Top node.
   * @param position Position to look at.
   * @return 'Finest' node containing position.
   */
  @CheckForNull
  public static AstNode nodeSurrounding(final AstNode topNode, final Position position) {
    final List<AstNode> nodes =
        AstQuery.dfs(topNode)
            .filter(AstQuery::isGrammarNode)
            .filter(node -> node.getToken() != null && node.getLastToken() != null)
            .filter(
                node -> {
                  final Token firstToken = node.getToken();
                  final int firstLine = firstToken.getLine();
                  final int firstColumn = firstToken.getColumn();

                  final Token lastToken = node.getLastToken();
                  final int lastLine = lastToken.getLine();
                  final int lastColumn =
                      lastToken.getColumn() + lastToken.getOriginalValue().length();

                  return (position.getLine() > firstLine
                          || position.getLine() == firstLine && position.getColumn() >= firstColumn)
                      && (position.getLine() < lastLine
                          || position.getLine() == lastLine && position.getColumn() <= lastColumn);
                })
            .collect(Collectors.toList());
    if (nodes.isEmpty()) {
      return null;
    }

    if (nodes.size() > 1 && nodes.get(0).is(MagikGrammar.MAGIK)) {
      nodes.remove(0);
    }

    return nodes.get(0);
  }

  /**
   * Get the {@link AstNode} surrounding {@code position} of a specific type.
   *
   * @param topNode Top node.
   * @param position Position to look at.
   * @param nodeTypes Wanted node types.
   * @return 'Finest' node containing position.
   */
  @CheckForNull
  public static AstNode nodeSurrounding(
      final AstNode topNode, final Position position, final AstNodeType... nodeTypes) {
    final List<AstNodeType> nodeTypesList = List.of(nodeTypes);
    final List<AstNode> nodes =
        AstQuery.dfs(topNode)
            .filter(AstQuery::isGrammarNode)
            .filter(node -> node.getToken() != null && node.getLastToken() != null)
            .filter(
                node -> {
                  final Token firstToken = node.getToken();
                  final int firstLine = firstToken.getLine();
                  final int firstColumn = firstToken.getColumn();

                  final Token lastToken = node.getLastToken();
                  final int lastLine = lastToken.getLine();
                  final int lastColumn =
                      lastToken.getColumn() + lastToken.getOriginalValue().length();

                  return (position.getLine() > firstLine
                          || position.getLine() == firstLine && position.getColumn() >= firstColumn)
                      && (position.getLine() < lastLine
                          || position.getLine() == lastLine && position.getColumn() <= lastColumn);
                })
            .filter(node -> nodeTypesList.contains(node.getType()))
            .toList();
    if (nodes.isEmpty()) {
      return null;
    }

    return nodes.get(0);
  }

  /**
   * Get a depth first search stream for node.
   *
   * @param node Node to get stream for.
   * @return Depth first search stream.
   */
  public static Stream<AstNode> dfs(final AstNode node) {
    final Stream<AstNode> parentStream = Stream.of(node);
    final Stream<AstNode> childStream = node.getChildren().stream();
    return Stream.concat(parentStream, childStream.flatMap(AstQuery::dfs));
  }

  /**
   * Get a depth first search stream for node, for which predicate applies.
   *
   * @param node Node to get stream for.
   * @param predicate Predicate to test node with.
   * @return Depth first search stream tested against predicate.
   */
  public static Stream<AstNode> dfs(final AstNode node, final Predicate<AstNode> predicate) {
    final Stream<AstNode> parentStream = Stream.of(node).filter(predicate);
    final Stream<AstNode> childStream = node.getChildren().stream().filter(predicate);
    return Stream.concat(
        parentStream, childStream.flatMap(childNode -> AstQuery.dfs(childNode, predicate)));
  }

  private static boolean isGrammarNode(final AstNode node) {
    return node.is(ProductDefinitionGrammar.values())
        || node.is(ModuleDefinitionGrammar.values())
        || node.is(MagikGrammar.values());
  }

  private static boolean isTokenNode(final AstNode node) {
    return node.isNot(ProductDefinitionGrammar.values())
        && node.isNot(ModuleDefinitionGrammar.values())
        && node.isNot(MagikGrammar.values());
  }
}
