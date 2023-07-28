package nl.ramsolutions.sw.magik.analysis.helpers;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/**
 * Helper for PACKAGE nodes.
 */
public class PackageNodeHelper {

    private static final String DEFAULT_PACKAGE = "user";

    private final AstNode node;

    /**
     * Constructor.
     * @param node Node to encapsulate.
     */
    public PackageNodeHelper(final AstNode node) {
        // Any node will do.
        this.node = node;
    }

    /**
     * Get the current package from the current node.
     * @return Name of current package or "user" if none was found.
     */
    public String getCurrentPackage() {
        // Find our parent node under MAGIK node.
        final AstNode magikNode = this.node.getFirstAncestor(MagikGrammar.MAGIK);
        AstNode topNode = this.node;
        while (topNode.getParent() != null
               && topNode.getParent() != magikNode) {
            topNode = topNode.getParent();
        }

        // Try to find PACKAGE_SPECIFICATION node via previous siblings.
        AstNode siblingNode = topNode;
        while (siblingNode != null) {
            if (siblingNode.is(MagikGrammar.PACKAGE_SPECIFICATION)) {
                final AstNode identifierNode = siblingNode.getFirstChild(MagikGrammar.PACKAGE_IDENTIFIER);
                return identifierNode.getTokenValue();
            }

            siblingNode = siblingNode.getPreviousSibling();
        }

        // Default to package.
        return PackageNodeHelper.DEFAULT_PACKAGE;
    }

}
