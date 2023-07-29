package nl.ramsolutions.sw.magik.typedchecks.checks;

import com.sonar.sslr.api.AstNode;
import java.util.ArrayList;
import java.util.Collection;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.LocalTypeReasoner;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResult;
import nl.ramsolutions.sw.magik.analysis.typing.types.Method;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;
import org.sonar.check.Rule;

/**
 * Check for UNDEFINED method invocation result.
 */
@Rule(key = UndefinedMethodResultTypedCheck.CHECK_KEY)
public class UndefinedMethodResultTypedCheck extends MagikTypedCheck {

    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final String CHECK_KEY = "UndefinedMethodResultTypedCheck";

    private static final String MESSAGE = "UNDEFINED results for method: %s";

    @Override
    @SuppressWarnings("checkstyle:NestedIfDepth")
    protected void walkPostMethodInvocation(final AstNode node) {
        final LocalTypeReasoner reasoner = this.getReasoner();

        final ExpressionResult result = reasoner.getNodeType(node);
        if (result == ExpressionResult.UNDEFINED) {
            final AstNode firstIdentifierNode = node.getFirstChild(MagikGrammar.IDENTIFIER);
            final AstNode issueNode = firstIdentifierNode != null
                ? firstIdentifierNode
                : node;

            final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(node);
            final String methodName = helper.getMethodName();

            AbstractType calledType = this.getTypeInvokedOn(node);
            final Collection<Method> methods = calledType.getMethods(methodName);
            if (!methods.isEmpty()) {
                // Set real called type.
                // TODO: Only first method, or should we test all methods?
                final Method method = new ArrayList<>(methods).get(0);
                calledType = method.getOwner();
            }
            final String fullMethodName = calledType.getFullName() + "." + methodName;

            final String message = String.format(MESSAGE, fullMethodName);
            this.addIssue(issueNode, message);
        }
    }

}
