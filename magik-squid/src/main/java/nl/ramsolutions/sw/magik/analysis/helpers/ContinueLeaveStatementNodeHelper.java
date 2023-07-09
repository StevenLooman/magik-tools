package nl.ramsolutions.sw.magik.analysis.helpers;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/**
 * Helper for CONTINUE_STATEMENT and LEAVE_STATEMENT nodes.
 */
public class ContinueLeaveStatementNodeHelper {

    private final AstNode node;

    /**
     * Constructor.
     * @param node Node to encapsulate.
     */
    public ContinueLeaveStatementNodeHelper(final AstNode node) {
        if (!node.is(MagikGrammar.CONTINUE_STATEMENT, MagikGrammar.LEAVE_STATEMENT)) {
            throw new IllegalArgumentException();
        }

        this.node = node;
    }

    /**
     * Get the related BODY node we're continuing from.
     * @return Body node.
     */
    @SuppressWarnings("checkstyle:NestedIfDepth")
    public AstNode getRelatedBodyNode() {
        final AstNode wantedLabelNode = this.node.getFirstChild(MagikGrammar.LABEL);
        if (wantedLabelNode != null) {
            final LabelNodeHelper labelHelper = new LabelNodeHelper(wantedLabelNode);
            final String wantedLabel = labelHelper.getIdentifier();

            // Find parent BODY node with label
            AstNode bodyNode = this.node.getFirstAncestor(MagikGrammar.BODY);
            while (bodyNode != null) {
                // Check if current BODY node has our wanted label.
                final AstNode bodyLabelNode = bodyNode.getPreviousSibling();
                if (bodyLabelNode != null
                    && bodyLabelNode.is(MagikGrammar.LABEL)) {
                    final LabelNodeHelper bodyLabelHelper = new LabelNodeHelper(bodyLabelNode);
                    final String bodyLabel = bodyLabelHelper.getIdentifier();
                    if (bodyLabel.equals(wantedLabel)) {
                        return bodyNode;
                    }
                }

                // Continue searching upwards.
                bodyNode = bodyNode.getFirstAncestor(MagikGrammar.BODY);
            }
        }

        // If no BODY node was found and statement is a LEAVE, then find nearest _loop.
        // Find nearest LOOP or BODY statement.
        final AstNode loopNode = node.getFirstAncestor(MagikGrammar.LOOP);
        if (loopNode != null) {
            return loopNode.getFirstChild(MagikGrammar.BODY);
        }

        if (node.is(MagikGrammar.RETURN_STATEMENT)) {
            // Find furthest BODY statement.
            final AstNode procedureNode = node.getFirstAncestor(
                MagikGrammar.PROCEDURE_DEFINITION, MagikGrammar.METHOD_DEFINITION);
            final AstNode bodyNode = procedureNode.getFirstChild(MagikGrammar.BODY);
            if (bodyNode != null) {
                return bodyNode;
            }
        }

        // Last resort, use nearest body.
        return node.getFirstAncestor(MagikGrammar.BODY);
    }

}
