package nl.ramsolutions.sw.magik.analysis;

import com.sonar.sslr.api.AstNode;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/**
 * AstNode compare tools.
 */
public final class AstCompare {

    /**
     * Flags to influence the compare functionality.
     * Values:
     *     {{IDENTIFIER_IGNORE_NAME}}: Ignore the name of the IDENTIFIER node.
     *     {{ONLY_AST}}: Only compare AST, ignoring tokens such as '(' and ')'.
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
    public static boolean astNodeEqualsRecursive(final AstNode left, final AstNode right) {
        final EnumSet<Flags> flags = EnumSet.noneOf(Flags.class);
        return AstCompare.astNodeEqualsRecursive(left, right, flags);
    }

    /**
     * Test if two nodes are equal to each other.
     * Compares token values and structure.
     * @param left Node to compare
     * @param right Node to compare
     * @param flags Flags to influence comparison
     * @return True if nodes are equal, false otherwise.
     */
    public static boolean astNodeEqualsRecursive(final AstNode left, final AstNode right, final Set<Flags> flags) {
        // Compare nodes.
        if (!AstCompare.astNodeEquals(left, right, flags)) {
            return false;
        }

        // Compare children of nodes.
        final List<AstNode> leftChildren = left.getChildren();
        final List<AstNode> rightChildren = right.getChildren();
        if (leftChildren.size() != rightChildren.size()) {
            return false;
        }

        for (int i = 0; i < leftChildren.size(); ++i) {
            final AstNode leftChild = leftChildren.get(i);
            final AstNode rightChild = rightChildren.get(i);
            if (!AstCompare.astNodeEqualsRecursive(leftChild, rightChild, flags)) {
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
    public static boolean astNodeEquals(final AstNode left, final AstNode right) {
        final EnumSet<Flags> flags = EnumSet.noneOf(Flags.class);
        return AstCompare.astNodeEquals(left, right, flags);
    }

    /**
     * Test if two nodes are equal to each other.
     * Compares token values.
     * @param left Node to compare
     * @param right Node to compare
     * @param flags Flags to influence comparison
     * @return True if nodes are equal, false otherwise.
     */
    @SuppressWarnings("checkstyle:EmptyBlock")
    public static boolean astNodeEquals(final AstNode left, final AstNode right, final Set<Flags> flags) {
        // Ensure same type.
        if (left.getType() != right.getType()) {
            return false;
        }

        // Ensure both have a value, or neither has a value.
        final String leftTokenValue = left.getTokenValue();
        final String rightTokenValue = right.getTokenValue();
        if (leftTokenValue == null && rightTokenValue != null
            || leftTokenValue != null && rightTokenValue == null) {
            return false;
        }

        if (flags.contains(Flags.IGNORE_IDENTIFIER_NAME)
            && AstCompare.isIdentifierNode(left)) {
            // Don't compare IDENTIFIERS; continue
        } else if (leftTokenValue != null
                   && !leftTokenValue.equals(rightTokenValue)) {
            // Values have to be the same.
            return false;
        }

        return true;
    }

    private static boolean isIdentifierNode(final AstNode node) {
        if (node.is(MagikGrammar.IDENTIFIER)
            || node.getParent() != null
            && node.getParent().is(MagikGrammar.IDENTIFIER)) {
            return true;
        }

        // Ensure there is something to recurse down to.
        if (!node.hasChildren()) {
            return false;
        }

        return AstCompare.isIdentifierNode(node.getFirstChild());
    }

}
