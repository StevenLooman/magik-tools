package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import nl.ramsolutions.sw.magik.analysis.helpers.PackageNodeHelper;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.api.MagikKeyword;

/**
 * {@code _global} parser.
 */
public class GlobalDefinitionParser {

    private final AstNode node;

    /**
     * Constructor.
     * @param node {@code _global} node.
     */
    public GlobalDefinitionParser(final AstNode node) {
        this.node = node;
    }

    /**
     * Test if node is a {@code _global}.
     * @param node Node to test
     * @return True if node is a {@code _global}, false otherwise.
     */
    public static boolean isGlobalDefinition(final AstNode node) {
        final AstNode modifier = node.getFirstChild(MagikGrammar.VARIABLE_DEFINITION_MODIFIER);
        return modifier != null
            && modifier.getTokenValue().equalsIgnoreCase(MagikKeyword.GLOBAL.getValue());
    }

    /**
     * Parse defitions.
     * @return List of parsed definitions.
     */
    public List<Definition> parseDefinitions() {
        final AstNode modifier = this.node.getFirstChild(MagikGrammar.VARIABLE_DEFINITION_MODIFIER);
        if (modifier == null
            || !modifier.getTokenValue().equalsIgnoreCase(MagikKeyword.GLOBAL.getValue())) {
            throw new IllegalStateException();
        }

        // Figure pakkage.
        final String pakkage = this.getCurrentPakkage();

        // Figure name.
        final AstNode variableDefinitionNode = this.node.getFirstChild(MagikGrammar.VARIABLE_DEFINITION);
        final AstNode identifierNode = variableDefinitionNode.getFirstChild(MagikGrammar.IDENTIFIER);
        final String name = identifierNode.getTokenValue();

        final GlobalDefinition globalDefinition = new GlobalDefinition(node, pakkage, name);
        return List.of(globalDefinition);
    }

    private String getCurrentPakkage() {
        final PackageNodeHelper helper = new PackageNodeHelper(this.node);
        return helper.getCurrentPackage();
    }

}
