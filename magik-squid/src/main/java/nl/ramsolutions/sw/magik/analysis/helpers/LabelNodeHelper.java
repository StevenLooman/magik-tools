package nl.ramsolutions.sw.magik.analysis.helpers;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/**
 * Helper for LABEL nodes.
 */
public class LabelNodeHelper {

    private final AstNode node;

    /**
     * Constructor.
     * @param node Node to encapsulate.
     */
    public LabelNodeHelper(final AstNode node) {
        if (!node.is(MagikGrammar.LABEL)) {
            throw new IllegalArgumentException();
        }

        this.node = node;
    }

    /**
     * Get label identifier.
     * @return Label identifier.
     */
    public String getIdentifier() {
        return this.node.getChildren().get(1).getTokenValue();
    }

}
