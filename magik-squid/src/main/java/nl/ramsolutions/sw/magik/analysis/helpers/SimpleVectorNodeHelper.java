package nl.ramsolutions.sw.magik.analysis.helpers;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.util.List;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/**
 * Helper for SIMPLE_VECTOR nodes.
 */
public class SimpleVectorNodeHelper {

    private final AstNode node;

    /**
     * Constructor.
     * @param node Node to encapsulate.
     */
    public SimpleVectorNodeHelper(final AstNode node) {
        if (!node.is(MagikGrammar.SIMPLE_VECTOR)) {
            throw new IllegalArgumentException();
        }

        this.node = node;
    }

    /**
     * Helper method to get helper from EXPRESSION node.
     */
    @CheckForNull
    public static SimpleVectorNodeHelper fromExpressionSafe(final AstNode expressionNode) {
        if (!expressionNode.is(MagikGrammar.EXPRESSION)) {
            return null;
        }

        final AstNode atomNode = expressionNode.getFirstChild(MagikGrammar.ATOM);
        if (atomNode == null) {
            return null;
        }

        final AstNode simpleVectorNode = atomNode.getFirstChild(MagikGrammar.SIMPLE_VECTOR);
        if (simpleVectorNode == null) {
            return null;
        }

        return new SimpleVectorNodeHelper(simpleVectorNode);
    }

    /**
     * Get nth item from simple vector, with expecting type.
     * @param nth Nth item from simple vector.
     * @param type Expected (required) type.
     * @return Node of given type.
     */
    @CheckForNull
    public AstNode getNth(final int nth, final MagikGrammar type) {
        final List<AstNode> expressionNodes = this.node.getChildren(MagikGrammar.EXPRESSION);
        if (nth >= expressionNodes.size()) {
            return null;
        }
        final AstNode expressionNode = expressionNodes.get(nth);
        final AstNode atomNode = expressionNode.getFirstChild(MagikGrammar.ATOM);
        return atomNode.getFirstChild(type);
    }

}
