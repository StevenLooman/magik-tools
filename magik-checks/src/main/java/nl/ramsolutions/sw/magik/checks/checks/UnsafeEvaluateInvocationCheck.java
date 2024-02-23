package nl.ramsolutions.sw.magik.checks.checks;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import org.sonar.check.Rule;

/**
 * Check for `unsafe_evaluate()` method invocations.
 */
@Rule(key = UnsafeEvaluateInvocationCheck.CHECK_KEY)
public class UnsafeEvaluateInvocationCheck extends MagikCheck {

    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final String CHECK_KEY = "UnsafeEvaluateInvocation";

    private static final String METHOD_NAME = "unsafe_evaluate()";
    private static final String MESSAGE = "Method '" + METHOD_NAME + "' is unsafe to use.";

    @Override
    protected void walkPreMethodInvocation(final AstNode node) {
        final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(node);
        final String methodName = helper.getMethodName();
        if (!methodName.equals(METHOD_NAME)) {
            return;
        }

        this.addIssue(node, MESSAGE);
    }

}
