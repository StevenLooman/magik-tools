package nl.ramsolutions.sw.magik.analysis.helpers;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/**
 * Helper for EXPRESSION nodes.
 */
public class ExpressionNodeHelper {

    private final AstNode node;

    /**
     * Constructor.
     * @param node Node to encapsulate.
     */
    public ExpressionNodeHelper(final AstNode node) {
        if (!node.is(MagikGrammar.EXPRESSION)) {
            throw new IllegalArgumentException();
        }

        this.node = node;
    }

    /**
     * Get a constant (literal) from the encapsulated expresion.
     * @return Constant (literal) value, if found.
     */
    @CheckForNull
    public String getConstant() {
        final AstNode atomNode = node.getFirstChild(MagikGrammar.ATOM);
        if (atomNode == null) {
            return null;
        }

        final AstNode valueNode = atomNode.getFirstChild(
            MagikGrammar.NUMBER,
            MagikGrammar.SYMBOL,
            MagikGrammar.STRING,
            MagikGrammar.CHARACTER,
            MagikGrammar.REGEXP,
            MagikGrammar.GLOBAL_REF);
        if (valueNode == null) {
            return null;
        }

        return valueNode.getTokens().stream()
            .map(Token::getValue)
            .collect(Collectors.joining());
    }

}
