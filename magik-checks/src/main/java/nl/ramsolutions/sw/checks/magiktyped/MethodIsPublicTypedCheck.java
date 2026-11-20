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
@Rule(key = MethodIsPublicTypedCheck.CHECK_KEY)
public class MethodIsPublicTypedCheck extends MagikTypedCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "MethodIsPublic";

  // Two spaces before `on`, as a session writes it.
  private static final String MESSAGE = "Cannot invoke private method %s  on object %s";

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
    final boolean privateMethod =
        methodDefinitions.stream()
            .anyMatch(
                methodDef -> methodDef.getModifiers().contains(MethodDefinition.Modifier.PRIVATE));
    if (!privateMethod) {
      return;
    }

    final AstNode issueNode = helper.getMethodNameNode();
    final String typeName = receiverTypeStr.getFullString();
    final String message = MESSAGE.formatted(methodName, typeName);
    this.addIssue(issueNode, message);
  }
}
