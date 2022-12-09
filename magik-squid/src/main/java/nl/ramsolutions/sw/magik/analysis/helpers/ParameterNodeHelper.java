package nl.ramsolutions.sw.magik.analysis.helpers;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.api.MagikKeyword;

/**
 * Helper for PARAMETER nodes.
 */
public class ParameterNodeHelper {

    private final AstNode node;

    /**
     * Constructor.
     * @param node Node to encapsulate.
     */
    public ParameterNodeHelper(final AstNode node) {
        if (!node.is(MagikGrammar.PARAMETER)) {
            throw new IllegalArgumentException();
        }

        this.node = node;
    }

    /**
     * Test if parameter is an {@code _optional} parameter.
     * @return True if optional, false otherwise.
     */
    public boolean isOptionalParameter() {
        if (this.hasModifier(MagikKeyword.OPTIONAL)) {
            return true;
        }
        if (this.hasModifier(MagikKeyword.GATHER)) {
            return false;
        }
        if (this.node.getParent().is(MagikGrammar.ASSIGNMENT_PARAMETER)) {
            return false;
        }

        final AstNode parametersNode = this.node.getParent();
        if (parametersNode.isNot(MagikGrammar.PARAMETERS)) {
            throw new IllegalStateException();
        }

        AstNode previousSiblingNode = this.node.getPreviousSibling();
        while (previousSiblingNode != null) {
            if (previousSiblingNode.is(MagikGrammar.PARAMETER)) {
                final ParameterNodeHelper helper = new ParameterNodeHelper(previousSiblingNode);
                if (helper.hasModifier(MagikKeyword.OPTIONAL)) {
                    return true;
                }
            }

            previousSiblingNode = previousSiblingNode.getPreviousSibling();
        }

        return false;
    }

    /**
     * Test if parameter is an {@code _gather} parameter.
     * @return True if gather, false otherwise.
     */
    public boolean isGatherParameter() {
        return this.hasModifier(MagikKeyword.GATHER);
    }

    public boolean hasModifier(final MagikKeyword modifier) {
        return this.node.getFirstChild(MagikGrammar.PARAMETER_MODIFIER) != null
            && this.node.getTokenValue().equalsIgnoreCase(modifier.getValue());
    }

}
