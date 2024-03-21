package nl.ramsolutions.sw.magik.typedchecks.checks;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResult;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.types.UndefinedType;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;
import org.sonar.check.Rule;

/** Check to test if used type is deprecaed. */
@Rule(key = DeprecatedTypeUsageTypedCheck.CHECK_KEY)
public class DeprecatedTypeUsageTypedCheck extends MagikTypedCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "DeprecatedTypeUsage";

  private static final String DEPRECATED_TOPIC = "deprecated";
  private static final String MESSAGE = "Used type '%s' is deprecated";

  @Override
  protected void walkPostIdentifier(final AstNode node) {
    final AstNode parent = node.getParent();
    if (!parent.is(MagikGrammar.ATOM)) {
      return;
    }

    final LocalTypeReasonerState state = this.getTypeReasonerState();
    final ExpressionResult result = state.getNodeType(parent);
    final AbstractType type = result.get(0, UndefinedType.INSTANCE);
    if (type == UndefinedType.INSTANCE) {
      return;
    }

    if (!type.getTopics().contains(DeprecatedTypeUsageTypedCheck.DEPRECATED_TOPIC)) {
      return;
    }

    final TypeString typeString = type.getTypeString();
    final String typeStringStr = typeString.getFullString();
    final String message = String.format(MESSAGE, typeStringStr);
    this.addIssue(node, message);
  }
}
