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
        // only one statement in if body
        final List<AstNode> bodyNodes = AstQuery.getChildrenFromChain(node, MagikGrammar.BODY);
        if (bodyNodes.size() != 1) {
            return;
        }
        final AstNode bodyNode = bodyNodes.get(0);
        if (bodyNode.getChildren(MagikGrammar.STATEMENT).size() != 1) {
            return;
        }

        // statement is an if statement
        final List<AstNode> bodyIfNodes = AstQuery.getChildrenFromChain(bodyNode,
            MagikGrammar.STATEMENT,
            MagikGrammar.EXPRESSION_STATEMENT,
            MagikGrammar.EXPRESSION,
            MagikGrammar.ATOM,
            MagikGrammar.IF);
        if (bodyIfNodes.size() != 1) {
            return;
        }

        // has no elif or else
        final AstNode ifNode = bodyIfNodes.get(0);
        final List<AstNode> elifNodes = ifNode.getChildren(MagikGrammar.ELIF);
        if (!elifNodes.isEmpty()) {
            return;
        }
        final AstNode elseNode = ifNode.getFirstChild(MagikGrammar.ELSE);
        if (elseNode != null) {
            return;
        }

        this.addIssue(ifNode, MESSAGE);
    }

    private void testIfElseIf(final AstNode node) {
        // only one statement in else body
        final List<AstNode> bodyNodes =
            AstQuery.getChildrenFromChain(node, MagikGrammar.ELSE, MagikGrammar.BODY);
        if (bodyNodes.size() != 1) {
            return;
        }
        final AstNode bodyNode = bodyNodes.get(0);
        if (bodyNode.getChildren(MagikGrammar.STATEMENT).size() != 1) {
            return;
        }

        // statement is an if statement
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
