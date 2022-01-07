package nl.ramsolutions.sw.magik.analysis.helpers;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/**
 * Helper for SLOT nodes.
 */
public class SlotNodeHelper {

    private final AstNode node;

    /**
     * Constructor.
     * @param node Node to encapsulate.
     */
    public SlotNodeHelper(final AstNode node) {
        if (!node.is(MagikGrammar.SLOT)) {
            throw new IllegalArgumentException();
        }

        this.node = node;
    }

    public String getSlotName() {
        final AstNode identifierNode = node.getFirstDescendant(MagikGrammar.IDENTIFIER);
        return identifierNode.getTokenValue();
    }

}
