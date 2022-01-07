package nl.ramsolutions.sw.magik.checks.checks;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.magik.analysis.AstCompare;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import org.sonar.check.Rule;

/**
 * Check if left leg and right leg of comparison are equal.
 */
@Rule(key = LhsRhsComparatorEqualCheck.CHECK_KEY)
public class LhsRhsComparatorEqualCheck extends MagikCheck {

    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final String CHECK_KEY = "LhsRhsComparatorEqual";
    private static final String MESSAGE = "Left hand side and right hand side of comparator are equal.";

    @Override
    protected void walkPreOrExpression(final AstNode node) {
        this.checkComparison(node);
    }

    @Override
    protected void walkPreXorExpression(final AstNode node) {
        this.checkComparison(node);
    }

    @Override
    protected void walkPreAndExpression(final AstNode node) {
        this.checkComparison(node);
    }

    @Override
    protected void walkPreEqualityExpression(final AstNode node) {
        this.checkComparison(node);
    }

    @Override
    protected void walkPreRelationalExpression(final AstNode node) {
        this.checkComparison(node);
    }

    private void checkComparison(final AstNode node) {
        final AstNode leftHandSide = node.getFirstChild();
        final AstNode rightHandSide = node.getLastChild();
        if (AstCompare.astNodeEqualsRecursive(leftHandSide, rightHandSide)) {
            this.addIssue(node, MESSAGE);
        }
    }

}
