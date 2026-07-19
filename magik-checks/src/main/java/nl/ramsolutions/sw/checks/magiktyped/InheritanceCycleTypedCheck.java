package nl.ramsolutions.sw.checks.magiktyped;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import org.sonar.check.Rule;

/** Check for inheritance cycles. */
@Rule(key = InheritanceCycleTypedCheck.CHECK_KEY)
public class InheritanceCycleTypedCheck extends MagikTypedCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "InheritanceCycle";

  private static final String MESSAGE = "Type %s is part of an inheritance cycle.";

  @Override
  protected void walkPostMagik(final AstNode node) {
    final TypeStringResolver resolver = this.getTypeStringResolver();
    this.getMagikFile().getMagikDefinitions().stream()
        .filter(ExemplarDefinition.class::isInstance)
        .map(ExemplarDefinition.class::cast)
        .forEach(exemplarDefinition -> this.checkExemplar(resolver, exemplarDefinition));
  }

  private void checkExemplar(
      final TypeStringResolver resolver, final ExemplarDefinition exemplarDefinition) {
    // Canonicalize through the package-use hierarchy: a locally-parsed exemplar's own
    // TypeString may not carry the same package as the (already-resolved) TypeStrings the
    // ancestor walk yields, so compare against the resolved identity, not the raw one.
    final TypeString declaredTypeStr = exemplarDefinition.getTypeString();
    final ExemplarDefinition resolvedExemplarDefinition =
        resolver.getExemplarDefinition(declaredTypeStr);
    final TypeString typeStr =
        resolvedExemplarDefinition != null
            ? resolvedExemplarDefinition.getTypeString()
            : declaredTypeStr;
    // getAllAncestors is cycle-safe (bounded by a seen-set); a type in its own ancestor set is in
    // a cycle. Report on this file's node so a two-file cycle surfaces from both files, rather
    // than from neither.
    if (resolver.getAllAncestors(typeStr).contains(typeStr)) {
      final String message = MESSAGE.formatted(typeStr.getFullString());
      this.addIssue(exemplarDefinition.getNode(), message);
    }
  }
}
