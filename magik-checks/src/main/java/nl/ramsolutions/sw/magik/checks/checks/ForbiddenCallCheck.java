package nl.ramsolutions.sw.magik.checks.checks;

import com.sonar.sslr.api.AstNode;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

/**
 * Check for forbidden calls.
 */
@Rule(key = ForbiddenCallCheck.CHECK_KEY)
public class ForbiddenCallCheck extends MagikCheck {

    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final String CHECK_KEY = "ForbiddenCall";

    private static final String MESSAGE = "Call '%s' is forbidden.";
    private static final String DEFAULT_FORBIDDEN_CALLS = "show(), print(), debug_print()";

    /**
     * List of forbidden calls, separated by ','.
     */
    @RuleProperty(
        key = "forbidden calls",
        defaultValue = "" + DEFAULT_FORBIDDEN_CALLS,
        description = "List of forbidden calls, separated by ','",
        type = "STRING")
    @SuppressWarnings("checkstyle:VisibilityModifier")
    public String forbiddenCalls = DEFAULT_FORBIDDEN_CALLS;

    private Set<String> getForbiddenCalls() {
        return Arrays.stream(this.forbiddenCalls.split(","))
            .map(String::trim)
            .collect(Collectors.toSet());
    }

    @Override
    protected void walkPreMethodInvocation(final AstNode node) {
        final AstNode identifierNode = node.getFirstChild(MagikGrammar.IDENTIFIER);
        if (identifierNode == null) {
            return;
        }

        final String identifier = "." + identifierNode.getTokenValue();
        if (!this.getForbiddenCalls().contains(identifier)) {
            return;
        }

        final String message = String.format(MESSAGE, identifier);
        this.addIssue(node, message);
    }

    @Override
    protected void walkPreProcedureInvocation(final AstNode node) {
        final AstNode parentNode = node.getParent();
        if (!parentNode.is(MagikGrammar.POSTFIX_EXPRESSION)) {
            return;
        }

        final String identifier = parentNode.getTokenValue() + "()";
        if (!this.getForbiddenCalls().contains(identifier)) {
            return;
        }

        final String message = String.format(MESSAGE, identifier);
        this.addIssue(node, message);
    }

}
