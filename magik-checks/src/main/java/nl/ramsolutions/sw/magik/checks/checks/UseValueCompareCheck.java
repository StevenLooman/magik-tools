package nl.ramsolutions.sw.magik.checks.checks;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import org.sonar.check.Rule;

/**
 * Check if `=` should be used where `_is` was used.
 */
@Rule(key = UseValueCompareCheck.CHECK_KEY)
public class UseValueCompareCheck extends MagikCheck {

    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final String CHECK_KEY = "UseValueCompare";
    private static final String MESSAGE = "Type '%s' should not be compare with _is.";

    @Override
    protected void walkPreEqualityExpression(final AstNode node) {
        if (isInstanceCompare(node)
            && (hasStringLiteral(node) || hasBigNumLiteral(node))) {
            final String message = String.format(MESSAGE, "string");
            this.addIssue(node, message);
        }
    }

    private boolean isInstanceCompare(final AstNode node) {
        return node.getChildren().get(1).getTokenValue().equals("_is")
            || node.getChildren().get(1).getTokenValue().equals("_isnt");
    }

    private boolean hasStringLiteral(final AstNode node) {
        final List<AstNode> children = node.getChildren();
        final AstNode left = children.get(0);
        final AstNode right = children.get(2);
        return left.is(MagikGrammar.ATOM)
            && left.getFirstChild(MagikGrammar.STRING) != null
            || right.is(MagikGrammar.ATOM)
            && right.getFirstChild(MagikGrammar.STRING) != null;
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    private boolean hasBigNumLiteral(final AstNode node) {
        final List<AstNode> children = node.getChildren();
        final AstNode left = children.get(0);
        final AstNode right = children.get(2);
        return left.is(MagikGrammar.ATOM)
            && left.getFirstChild(MagikGrammar.NUMBER) != null
            && !left.getFirstChild().getTokenValue().contains(".")
            && Long.parseLong(left.getFirstChild().getTokenValue()) > 1 << 29
            || right.is(MagikGrammar.ATOM)
            && right.getFirstChild().is(MagikGrammar.NUMBER)
            && !right.getFirstChild().getTokenValue().contains(".")
            && Long.parseLong(right.getFirstChild().getTokenValue()) > 1 << 29;
    }

}
