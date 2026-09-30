package nl.ramsolutions.sw.checks.magiktyped;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.util.Collection;
import java.util.Set;
import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ITypeStringDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.Pragma;
import nl.ramsolutions.sw.magik.analysis.definitions.ProcedureDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import org.sonar.check.Rule;

/** Check to test if a used type, procedure or global is deprecated. */
@Rule(key = DeprecatedTypeUsageTypedCheck.CHECK_KEY)
public class DeprecatedTypeUsageTypedCheck extends MagikTypedCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "DeprecatedTypeUsage";

  private static final String MESSAGE = "Used %s '%s' is deprecated";

  @Override
  protected void walkPostIdentifier(final AstNode node) {
    // Judge the name, not its value: a global holding a deprecated instance is not one.
    final Collection<ITypeStringDefinition> definitions = this.getGlobalReferenceDefinitions(node);
    // On a collision, report only when every definition is deprecated.
    if (definitions.isEmpty()
        || !definitions.stream().allMatch(DeprecatedTypeUsageTypedCheck::isDeprecated)) {
      return;
    }

    final String kind = DeprecatedTypeUsageTypedCheck.getKind(definitions);
    final ITypeStringDefinition definition = definitions.iterator().next();
    final TypeString typeStr = definition.getTypeString();
    final String typeStringStr = typeStr.getFullString();
    final String message = MESSAGE.formatted(kind, typeStringStr);
    this.addIssue(node, message);
  }

  private static String getKind(final Collection<ITypeStringDefinition> definitions) {
    if (definitions.stream().allMatch(ExemplarDefinition.class::isInstance)) {
      return "type";
    }

    if (definitions.stream().allMatch(ProcedureDefinition.class::isInstance)) {
      return "procedure";
    }

    return "global";
  }

  private static boolean isDeprecated(final ITypeStringDefinition definition) {
    final Pragma pragma = DeprecatedTypeUsageTypedCheck.getPragma(definition);
    if (pragma == null) {
      return false;
    }

    final Set<String> classifyLevels = pragma.getClassifyLevels();
    return classifyLevels.contains(Pragma.CLASSIFY_LEVEL_DEPRECATED);
  }

  @CheckForNull
  private static Pragma getPragma(final ITypeStringDefinition definition) {
    if (definition instanceof ExemplarDefinition exemplarDef) {
      return exemplarDef.getPragma();
    }

    if (definition instanceof ProcedureDefinition procedureDef) {
      return procedureDef.getPragma();
    }

    if (definition instanceof GlobalDefinition globalDef) {
      return globalDef.getPragma();
    }

    return null;
  }
}
