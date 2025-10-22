package nl.ramsolutions.sw.checks.magiktyped;

import com.sonar.sslr.api.AstNode;
import java.util.Collection;
import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.Pragma;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import org.sonar.check.Rule;

/** Check to test if used method is deprecaed. */
@Rule(key = DeprecatedMethodUsageTypedCheck.CHECK_KEY)
public class DeprecatedMethodUsageTypedCheck extends MagikTypedCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "DeprecatedMethodUsage";

  private static final String MESSAGE = "Used method '%s' is deprecated";

  @Override
  protected void walkPostMethodInvocation(final AstNode node) {
    // Get type.
    final TypeString calledTypeStr = this.getTypeInvokedOn(node);
    if (calledTypeStr.isUndefined()) {
      // Cannot give any useful information, so abort.
      return;
    }

    // Get method.
    final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(node);
    final String methodName = helper.getMethodName();
    final TypeStringResolver resolver = this.getTypeStringResolver();
    final Collection<MethodDefinition> methodDefs =
        resolver.getRespondingMethodDefinitions(calledTypeStr, methodName);

    // Add issue if method is deprecated.
    methodDefs.stream()
        .filter(
            methodDef ->
                methodDef.getPragma() != null
                    && methodDef
                        .getPragma()
                        .getClassifyLevels()
                        .contains(Pragma.CLASSIFY_LEVEL_DEPRECATED))
        .forEach(
            methodDef -> {
              final String fullName = calledTypeStr.getFullString() + "." + methodName;
              final String message = MESSAGE.formatted(fullName);
              this.addIssue(node, message);
            });
  }
}
