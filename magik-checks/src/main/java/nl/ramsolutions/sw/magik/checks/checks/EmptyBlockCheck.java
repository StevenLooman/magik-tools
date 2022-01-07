package nl.ramsolutions.sw.magik.checks.checks;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import org.sonar.check.Rule;

/**
 * Check for empty bodies.
 */
@Rule(key = EmptyBlockCheck.CHECK_KEY)
public class EmptyBlockCheck extends MagikCheck {

    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final String CHECK_KEY = "EmptyBlock";
    private static final String MESSAGE = "Block is empty.";

    @Override
    protected void walkPreBody(final AstNode node) {
        // Ensure not in abstract method.
        if (this.isAbstractMethodBody(node)) {
            return;
        }

        final boolean hasChildren = node.getChildren().stream()
            .filter(childNode -> !childNode.getTokenValue().trim().isEmpty())
            .anyMatch(childNode -> true);
        if (!hasChildren) {
            final AstNode parentNode = node.getParent();
            this.addIssue(parentNode, MESSAGE);
        }
    }

    private boolean isAbstractMethodBody(final AstNode node) {
        final AstNode parentNode = node.getParent();
        if (parentNode == null
            || parentNode.isNot(MagikGrammar.METHOD_DEFINITION)) {
            return false;
        }
        final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(parentNode);
        return helper.isAbstractMethod();
    }

}
