package nl.ramsolutions.sw.checks.magiktyped;

import com.sonar.sslr.api.AstNode;
import java.util.Collection;
import java.util.Set;
import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.Pragma;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
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
    // On a collision, report only when every definition is deprecated.
    final Collection<ExemplarDefinition> exemplarDefs = resolver.getExemplarDefinitions(typeStr);
    if (exemplarDefs.isEmpty()
        || !exemplarDefs.stream().allMatch(DeprecatedTypeUsageTypedCheck::isDeprecated)) {
      return;
    }

    final String typeStringStr = typeStr.getFullString();
    final String message = MESSAGE.formatted(typeStringStr);
    this.addIssue(node, message);
  }

  private static boolean isDeprecated(final ExemplarDefinition exemplarDefinition) {
    final Pragma pragma = exemplarDefinition.getPragma();
    if (pragma == null) {
      return false;
    }

    final Set<String> classifyLevels = pragma.getClassifyLevels();
    return classifyLevels.contains(Pragma.CLASSIFY_LEVEL_DEPRECATED);
  }
}
