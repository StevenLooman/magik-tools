package nl.ramsolutions.sw.magik.typedchecks.checks;

import com.sonar.sslr.api.AstNode;
import java.util.Collection;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResult;
import nl.ramsolutions.sw.magik.analysis.typing.types.Method;
import nl.ramsolutions.sw.magik.analysis.typing.types.UndefinedType;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;
import org.sonar.check.Rule;

/**
 * Check for UNDEFINED method call result.
 */
@Rule(key = UndefinedMethodCallResultTypedCheck.CHECK_KEY)
public class UndefinedMethodCallResultTypedCheck extends MagikTypedCheck {

    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final String CHECK_KEY = "UndefinedMethodCallResult";

    private static final String MESSAGE = "UNDEFINED results for method: %s";

    @Override
    @SuppressWarnings("checkstyle:NestedIfDepth")
    protected void walkPostMethodInvocation(final AstNode node) {
        final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(node);
        final LocalTypeReasonerState reasonerState = this.getTypeReasonerState();
        final AbstractType receiverType = this.getTypeInvokedOn(node);
        if (receiverType == UndefinedType.INSTANCE) {
            // Don't bother with method invocations on UNDEFINED.
            return;
        }

        final ExpressionResult result = reasonerState.getNodeType(node);
        if (result.containsUndefined()) {
            final AstNode firstIdentifierNode = node.getFirstChild(MagikGrammar.IDENTIFIER);
            final AstNode issueNode = firstIdentifierNode != null
                ? firstIdentifierNode
                : node;

            final String methodName = helper.getMethodName();

            final Collection<Method> methods = receiverType.getMethods(methodName);
            final String receiverFullName = !methods.isEmpty()
                ? methods.iterator().next().getOwner().getFullName()
                : receiverType.getFullName();
            final String fullMethodName = receiverFullName + "." + methodName;

            final String message = String.format(MESSAGE, fullMethodName);
            this.addIssue(issueNode, message);
        }
    }

}
