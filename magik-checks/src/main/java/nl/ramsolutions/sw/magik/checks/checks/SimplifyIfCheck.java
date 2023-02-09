package nl.ramsolutions.sw.magik.checks.checks;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import org.sonar.check.Rule;

/**
 * Check if if-statement can be simplified.
 */
@Rule(key = SimplifyIfCheck.CHECK_KEY)
public class SimplifyIfCheck extends MagikCheck {

    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final String CHECK_KEY = "SimplifyIf";
    private static final String MESSAGE = "You can simplify this if by using _elif or combining guards.";

    @Override
    protected void walkPreIf(final AstNode node) {
        this.testIfIf(node);
        this.testIfElseIf(node);
    }

    private void testIfIf(final AstNode node) {
        // Has no elif or else child nodes.
        if (node.hasDirectChildren(MagikGrammar.ELIF, MagikGrammar.ELSE)) {
            return;
        }

        // Only one statement in if body.
        final AstNode bodyNode = node.getFirstChild(MagikGrammar.BODY);
        if (bodyNode == null) {
            return;
        }

        final List<AstNode> statementNodes = bodyNode.getChildren(MagikGrammar.STATEMENT);
        if (statementNodes.size() != 1) {
            return;
        }

        // Statement is an if statement.
        final List<AstNode> bodyIfNodes = AstQuery.getChildrenFromChain(bodyNode,
            MagikGrammar.STATEMENT,
            MagikGrammar.EXPRESSION_STATEMENT,
            MagikGrammar.EXPRESSION,
            MagikGrammar.ATOM,
            MagikGrammar.IF);
        if (bodyIfNodes.size() != 1) {
            return;
        }

        // Has no elif or else.
        final AstNode bodyIfNode = bodyIfNodes.get(0);
        final List<AstNode> bodyIfElifElseNodes = bodyIfNode.getChildren(MagikGrammar.ELIF, MagikGrammar.ELSE);
        if (!bodyIfElifElseNodes.isEmpty()) {
            return;
        }

        this.addIssue(bodyIfNode, MESSAGE);
    }

    private void testIfElseIf(final AstNode node) {
        final AstNode elseNode = node.getFirstChild(MagikGrammar.ELSE);
        if (elseNode == null) {
            return;
        }

        // Only one statement in else body.
        final AstNode bodyNode = elseNode.getFirstChild(MagikGrammar.BODY);
        final List<AstNode> statementNodes = bodyNode.getChildren(MagikGrammar.STATEMENT);
        if (statementNodes.size() != 1) {
            return;
        }

        // Statement is an if statement.
        final List<AstNode> elseIfNodes = AstQuery.getChildrenFromChain(bodyNode,
            MagikGrammar.STATEMENT,
            MagikGrammar.EXPRESSION_STATEMENT,
            MagikGrammar.EXPRESSION,
            MagikGrammar.ATOM,
            MagikGrammar.IF);
        if (elseIfNodes.size() != 1) {
            return;
        }

        final AstNode elseIfNode = elseIfNodes.get(0);
        this.addIssue(elseIfNode, MESSAGE);
    }

}
