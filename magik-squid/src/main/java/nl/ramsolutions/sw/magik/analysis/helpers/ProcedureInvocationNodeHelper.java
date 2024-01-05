package nl.ramsolutions.sw.magik.analysis.helpers;

import com.sonar.sslr.api.AstNode;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/**
 * Helper for PROCEDURE_INVOCATION nodes.
 */
public class ProcedureInvocationNodeHelper {

    private final AstNode node;

    /**
     * Constructor.
     * @param node Node to encapsulate.
     */
    public ProcedureInvocationNodeHelper(final AstNode node) {
        if (!node.is(MagikGrammar.PROCEDURE_INVOCATION)) {
            throw new IllegalArgumentException();
        }

        this.node = node;
    }

    /**
     * Get invoked identifier. This does not check if the identifier is a global, variable, or ...
     * @return Name of identifier.
     */
    @CheckForNull
    public String getInvokedIdentifier() {
        AstNode previousSibling = node.getPreviousSibling();
        if (previousSibling == null) {
            return null;
        }

        return previousSibling.getTokenValue();
    }

    /**
     * Check if node is a PROCEDURE_INVOCATION node with name.
     * Not that this does not handle any type resolution, so this is primarily used for:
     * - def_package
     * - def_slotted_exemplar
     * - ...
     * @param procedureName Procedure name to match.
     * @return true if node is a PROCEDURE_INVOCATION with given name, false otherwise.
     */
    public boolean isProcedureInvocationOf(final String procedureName) {
        String identifier = this.getInvokedIdentifier();
        return procedureName.equalsIgnoreCase(identifier);
    }

    /**
     * Get ARGUMENTS -> ARGUMENT -> EXPRESSION nodes.
     * @return
     */
    public List<AstNode> getArgumentExpressionNodes() {
        final AstNode argumentsNode = this.node.getFirstChild(MagikGrammar.ARGUMENTS);
        if (argumentsNode == null) {
            return Collections.emptyList();
        }

        return argumentsNode.getChildren(MagikGrammar.ARGUMENT).stream()
            .map(AstNode::getFirstChild)
            .collect(Collectors.toList());
    }

}
