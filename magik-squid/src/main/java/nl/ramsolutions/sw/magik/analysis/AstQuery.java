package nl.ramsolutions.sw.magik.analysis;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.api.Token;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/**
 * AstNode query utility functions.
 */
public final class AstQuery {

    private AstQuery() {
    }

    /**
     * Get the AstNodes which match a chain of {@link AstNodeType}s.
     * @param node {@link AstNode} to query
     * @param nodeTypes Chain of {@link AstNodeType}s
     * @return {@link AstNode}s which match query
     */
    public static List<AstNode> getChildrenFromChain(final AstNode node, final AstNodeType... nodeTypes) {
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
     * Get the first AstNode which matches a chain of {@link AstNodeTypes}s.
     * Tries to get {@code getChildNode()} on each node, for each {@code nodeTypes}.
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
     * @param node {@link AstNode} to query.
     * @param nodeTypes Chain of {@link AstNodeType}s.
     * @return {@link AstNode} which matches query, {@code null} if not found.
     */
    @CheckForNull
    public static AstNode getParentFromChain(final AstNode node, final AstNodeType... nodeTypes) {
        AstNode current = node;
        for (final AstNodeType nodeType : nodeTypes) {
            current = current.getParent();
            if (current == null
                || !current.is(nodeType)) {
                return null;
            }
        }
        return current;
    }

    /**
     * Get the node in {@code topNode} before {@code position}.
     * @param topNode Top node.
     * @param position Position for node.
     * @return Token-Node before position.
     */
    @CheckForNull
    public static AstNode nodeBefore(final AstNode topNode, final Position position) {
        final List<AstNode> nodes = AstQuery.dfs(topNode)
            .filter(node -> node.isNot(MagikGrammar.values()))
            .filter(node -> {
                final Token token = node.getToken();
                final Range range = new Range(token);
                return position.isBeforeRange(range);
            })
            .collect(Collectors.toList());
        if (nodes.isEmpty()) {
            return null;
        }
        return nodes.get(nodes.size() - 1);
    }

    /**
     * Get the node in {@code topNode} at {@code position}.
     * @param topNode Top node.
     * @param position Position for node.
     * @return Token-Node at position.
     */
    @CheckForNull
    public static AstNode nodeAt(final AstNode topNode, final Position position) {
        final List<AstNode> nodes = AstQuery.dfs(topNode)
            .filter(node -> node.isNot(MagikGrammar.values()))
            .filter(node -> {
                final Token token = node.getToken();
                final Range range = new Range(token);
                return !position.isBeforeRange(range)
                    && !position.isAfterRange(range);
            })
            .collect(Collectors.toList());
        if (nodes.isEmpty()) {
            return null;
        }
        return nodes.get(0);
    }

    /**
     * Get the (token) node in {@code node} at {@code position} of a specific type.
     * @param topNode Top node.
     * @param position Position for node.
     * @param nodeTypes Node type to look for.
     * @return Token-Node at position.
     */
    @CheckForNull
    public static AstNode nodeAt(final AstNode topNode, final Position position, final AstNodeType... nodeTypes) {
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
     * @param topNode Top node.
     * @param position Position for node.
     * @return Node after position.
     */
    @CheckForNull
    public static AstNode nodeAfter(final AstNode topNode, final Position position) {
        final List<AstNode> nodes = AstQuery.dfs(topNode)
            .filter(node -> node.isNot(MagikGrammar.values()))
            .filter(node -> {
                final Token token = node.getToken();
                final Range range = new Range(token);
                return position.isAfterRange(range);
            })
            .collect(Collectors.toList());
        if (nodes.isEmpty()) {
            return null;
        }
        return nodes.get(0);
    }

    /**
     * Get the {@link AstNode} surrounding {@code position}.
     * @param topNode Top node.
     * @param position Position to look at.
     * @return 'Finest' node containing position.
     */
    @CheckForNull
    public static AstNode nodeSurrounding(final AstNode topNode, final Position position) {
        final List<AstNode> nodes = AstQuery.dfs(topNode)
            .filter(node -> node.is(MagikGrammar.values()))
            .filter(node -> node.getToken() != null && node.getLastToken() != null)
            .filter(node -> {
                final Token firstToken = node.getToken();
                final int firstLine = firstToken.getLine();
                final int firstColumn = firstToken.getColumn();

                final Token lastToken = node.getLastToken();
                final int lastLine = lastToken.getLine();
                final int lastColumn = lastToken.getColumn() + lastToken.getOriginalValue().length();

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
     * @param topNode Top node.
     * @param position Position to look at.
     * @param nodeTypes Wanted node types.
     * @return 'Finest' node containing position.
     */
    @CheckForNull
    public static AstNode nodeSurrounding(
            final AstNode topNode, final Position position, final AstNodeType... nodeTypes) {
        final List<AstNodeType> nodeTypesList = List.of(nodeTypes);
        final List<AstNode> nodes = AstQuery.dfs(topNode)
            .filter(node -> node.is(MagikGrammar.values()))
            .filter(node -> node.getToken() != null && node.getLastToken() != null)
            .filter(node -> {
                final Token firstToken = node.getToken();
                final int firstLine = firstToken.getLine();
                final int firstColumn = firstToken.getColumn();

                final Token lastToken = node.getLastToken();
                final int lastLine = lastToken.getLine();
                final int lastColumn = lastToken.getColumn() + lastToken.getOriginalValue().length();

                return (position.getLine() > firstLine
                        || position.getLine() == firstLine && position.getColumn() >= firstColumn)
                        && (position.getLine() < lastLine
                            || position.getLine() == lastLine && position.getColumn() <= lastColumn);
            })
            .filter(node -> nodeTypesList.contains(node.getType()))
            .collect(Collectors.toList());
        if (nodes.isEmpty()) {
            return null;
        }
        return nodes.get(0);
    }

    /**
     * Get a depth first search stream for node.
     * @param node Node to get stream for.
     * @return Depth first search stream.
     */
    public static Stream<AstNode> dfs(final AstNode node) {
        final Stream<AstNode> parentStream = Stream.of(node);
        final Stream<AstNode> childStream = node.getChildren().stream();
        return Stream.concat(
            parentStream,
            childStream.flatMap(AstQuery::dfs));
    }

    /**
     * Get a depth first search stream for node, for which predicate applies.
     * @param node Node to get stream for.
     * @param predicate Predicate to test node with.
     * @return Depth first search stream tested against predicate.
     */
    public static Stream<AstNode> dfs(final AstNode node, final Predicate<AstNode> predicate) {
        final Stream<AstNode> parentStream = Stream.of(node)
            .filter(predicate);
        final Stream<AstNode> childStream = node.getChildren().stream()
            .filter(predicate);
        return Stream.concat(
            parentStream,
            childStream.flatMap(childNode -> AstQuery.dfs(childNode, predicate)));
    }

}
