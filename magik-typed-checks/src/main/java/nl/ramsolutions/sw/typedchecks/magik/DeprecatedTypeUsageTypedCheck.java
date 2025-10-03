package nl.ramsolutions.sw.typedchecks.magik;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.Pragma;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.typedchecks.MagikTypedCheck;
import org.sonar.check.Rule;

/** Check to test if used type is deprecaed. */
@Rule(key = DeprecatedTypeUsageTypedCheck.CHECK_KEY)
public class DeprecatedTypeUsageTypedCheck extends MagikTypedCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "DeprecatedTypeUsage";

  private static final String MESSAGE = "Used type '%s' is deprecated";

  @Override
  protected void walkPostIdentifier(final AstNode node) {
    final AstNode parent = node.getParent();
    if (!parent.is(MagikGrammar.ATOM)) {
      return;
    }

    final LocalTypeReasonerState state = this.getTypeReasonerState();
    final ExpressionResultString result = state.getNodeType(parent);
    final TypeString typeStr = result.get(0, TypeString.UNDEFINED);
    if (typeStr.isUndefined()) {
      return;
    }

    final TypeStringResolver resolver = this.getTypeStringResolver();
    final ExemplarDefinition exemplarDef = resolver.getExemplarDefinition(typeStr);
    if (exemplarDef == null) {
      return;
    }
    final Pragma pragma = exemplarDef.getPragma();
    if (pragma == null) {
      return;
    }

    if (!pragma.getClassifyLevels().contains(Pragma.CLASSIFY_LEVEL_DEPRECATED)) {
      return;
    }

    final String typeStringStr = typeStr.getFullString();
    final String message = String.format(MESSAGE, typeStringStr);
    this.addIssue(node, message);
  }
}
