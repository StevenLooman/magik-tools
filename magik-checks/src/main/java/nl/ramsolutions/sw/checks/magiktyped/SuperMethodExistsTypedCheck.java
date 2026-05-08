package nl.ramsolutions.sw.checks.magiktyped;

import com.sonar.sslr.api.AstNode;
import java.util.Collection;
import java.util.Objects;
import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.sonar.check.Rule;

/** Check if method exists on super type. */
@Rule(key = SuperMethodExistsTypedCheck.CHECK_KEY)
public class SuperMethodExistsTypedCheck extends MagikTypedCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "SuperMethodExists";

  private static final String MESSAGE = "Unknown method on super: %s";

  @Override
  protected void walkPostMethodInvocation(final AstNode node) {
    // Check if receiver is _super.
    final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(node);
    final AstNode receiverNode = helper.getReceiverNode();
    if (receiverNode == null || !receiverNode.is(MagikGrammar.ATOM)) {
      return;
    }
    final AstNode superNode = receiverNode.getFirstChild(MagikGrammar.SUPER);
    if (superNode == null) {
      return;
    }

    // Get super type from reasoner state.
    final LocalTypeReasonerState state = this.getTypeReasonerState();
    final ExpressionResultString superResult = state.getNodeType(receiverNode);
    final TypeString superTypeStr = superResult.get(0, TypeString.UNDEFINED);
    if (superTypeStr.isUndefined()) {
      return;
    }

    // Get method name.
    final String methodName = helper.getMethodName();

    // Check if method exists on super type.
    final TypeString combinedTypeString = TypeString.combine(superTypeStr);
    Objects.requireNonNull(combinedTypeString);
    final TypeStringResolver resolver = this.getTypeStringResolver();
    for (final TypeString typeString : combinedTypeString.getCombinedTypes()) {
      final Collection<MethodDefinition> methodDefs =
          resolver.getRespondingMethodDefinitions(typeString, methodName);

      if (methodDefs.isEmpty()) {
        final String fullName = typeString.getFullString() + "." + methodName;
        final String message = MESSAGE.formatted(fullName);
        final AstNode firstIdentifierNode = node.getFirstChild(MagikGrammar.IDENTIFIER);
        final AstNode issueNode = firstIdentifierNode != null ? firstIdentifierNode : node;
        this.addIssue(issueNode, message);
      }
    }
  }
}
