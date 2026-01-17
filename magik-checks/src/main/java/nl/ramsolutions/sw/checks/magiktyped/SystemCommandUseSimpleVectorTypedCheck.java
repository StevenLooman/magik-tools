package nl.ramsolutions.sw.checks.magiktyped;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import org.sonar.check.Rule;

/** Test if referenced type is known. */
@Rule(key = SystemCommandUseSimpleVectorTypedCheck.CHECK_KEY)
public class SystemCommandUseSimpleVectorTypedCheck extends MagikTypedCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "SystemCommandUseSimpleVector";

  private static final String MESSAGE = "Use a simple_vector instead of a (concatenated) string";

  private static final List<String> TARGET_METHODS =
      List.of(
          "do_command()",
          "input_from_command()",
          "output_to_command()",
          "start_command()",
          "start_command_with_io()");
  private static final TypeString SW_SYSTEM = TypeString.ofIdentifier("system", "sw");

  @Override
  protected void walkPreMethodInvocation(final AstNode node) {
    final TypeString receiverTypeStr = this.getTypeInvokedOn(node).getWithoutGenerics();
    if (!receiverTypeStr.equals(SystemCommandUseSimpleVectorTypedCheck.SW_SYSTEM)) {
      return;
    }

    final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(node);
    final String methodName = helper.getMethodName();
    if (!SystemCommandUseSimpleVectorTypedCheck.TARGET_METHODS.contains(methodName)) {
      return;
    }

    // Get first argument type.
    final List<AstNode> argumentExpressionNodes = helper.getArgumentExpressionNodes();
    if (argumentExpressionNodes.isEmpty()) {
      return;
    }

    final LocalTypeReasonerState state = this.getTypeReasonerState();
    final AstNode argumentExpressionNode = argumentExpressionNodes.get(0);
    final ExpressionResultString result = state.getNodeType(argumentExpressionNode);
    final TypeString typeStr = result.get(0, TypeString.UNDEFINED);
    if (typeStr.getWithoutGenerics().equals(TypeString.SW_SIMPLE_VECTOR)) {
      return;
    }

    this.addIssue(node, SystemCommandUseSimpleVectorTypedCheck.MESSAGE);
  }
}
