package nl.ramsolutions.sw.magik.typedchecks.checks;

import com.sonar.sslr.api.AstNode;
import java.util.Collection;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.Method;
import nl.ramsolutions.sw.magik.analysis.typing.types.UndefinedType;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;
import org.sonar.check.Rule;

/** Check to test if used method is deprecaed. */
@Rule(key = DeprecatedMethodUsageTypedCheck.CHECK_KEY)
public class DeprecatedMethodUsageTypedCheck extends MagikTypedCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "DeprecatedMethodUsage";

  private static final String DEPRECATED_TOPIC = "deprecated";
  private static final String MESSAGE = "Used method '%s' is deprecated";

  @Override
  protected void walkPostMethodInvocation(final AstNode node) {
    // Get type.
    final AbstractType calledType = this.getTypeInvokedOn(node);
    if (calledType == UndefinedType.INSTANCE) {
      // Cannot give any useful information, so abort.
      return;
    }

    // Get method.
    final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(node);
    final String methodName = helper.getMethodName();
    final Collection<Method> methods = calledType.getMethods(methodName);

    // Add issue if method is deprecated.
    methods.stream()
        .filter(
            method -> method.getTopics().contains(DeprecatedMethodUsageTypedCheck.DEPRECATED_TOPIC))
        .forEach(
            method -> {
              final String fullName = calledType.getFullName() + "." + methodName;
              final String message = String.format(MESSAGE, fullName);
              this.addIssue(node, message);
            });
  }
}
