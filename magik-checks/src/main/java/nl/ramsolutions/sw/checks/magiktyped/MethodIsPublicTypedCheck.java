package nl.ramsolutions.sw.checks.magiktyped;

import com.sonar.sslr.api.AstNode;
import java.util.Collection;
import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.sonar.check.Rule;

/** Test if called method is not private. */
@Rule(key = UndefinedMethodCallResultTypedCheck.CHECK_KEY)
public class MethodIsPublicTypedCheck extends MagikTypedCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "UndefinedMethodCallResult";

  private static final String MESSAGE = "Called method is not public: %s";

  protected void walkPostMethodInvocation(final AstNode node) {
    final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(node);

    // Private methods can be invoked on self/super.
    final AstNode atomReceiverNode = helper.getReceiverNode();
    final AstNode childReceiverNode = atomReceiverNode.getFirstChild();
    if (childReceiverNode.is(MagikGrammar.SELF, MagikGrammar.SUPER)) {
      return;
    }

    // Add issue if method is not public.
    final TypeString receiverTypeStr = this.getTypeInvokedOnWithSelf(node);
    final String methodName = helper.getMethodName();
    final TypeStringResolver resolver = this.getTypeStringResolver();
    final Collection<MethodDefinition> methodDefinitions =
        resolver.getRespondingMethodDefinitions(receiverTypeStr, methodName);
    methodDefinitions.stream()
        .filter(methodDef -> methodDef.getModifiers().contains(MethodDefinition.Modifier.PRIVATE))
        .forEach(
            methodDef -> {
              final AstNode issueNode = helper.getMethodNameNode();
              final String methodDefName = methodDef.getName();
              final String message = String.format(MESSAGE, methodDefName);
              this.addIssue(issueNode, message);
            });
  }
}
