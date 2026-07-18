package nl.ramsolutions.sw.checks.magik;

import com.sonar.sslr.api.AstNode;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.checks.DisabledByDefault;
import nl.ramsolutions.sw.checks.MagikCheck;
import nl.ramsolutions.sw.magik.analysis.definitions.InheritanceDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.parser.TypeStringParser;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

/** Check if forbidden inheritance is used. */
@DisabledByDefault
@Rule(key = ForbiddenInheritanceCheck.CHECK_KEY)
public class ForbiddenInheritanceCheck extends MagikCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "ForbiddenInheritance";

  private static final String DEFAULT_FORBIDDEN_PARENTS = "";

  /** Forbidden parents to inherit from, separated by ','. */
  @RuleProperty(
      key = "forbidden parents",
      defaultValue = "" + DEFAULT_FORBIDDEN_PARENTS,
      description = "Forbidden parents to inherit from, separated by ','",
      type = "STRING")
  @SuppressWarnings("checkstyle:VisibilityModifier")
  public String forbiddenParents = DEFAULT_FORBIDDEN_PARENTS;

  @Override
  protected void walkPostMagik(final AstNode node) {
    if (this.forbiddenParents.isBlank()) {
      return;
    }

    final Set<TypeString> theForbiddenParents = this.getForbiddenParents();
    this.getMagikFile().getMagikDefinitions().stream()
        .filter(InheritanceDefinition.class::isInstance)
        .map(InheritanceDefinition.class::cast)
        .filter(edge -> theForbiddenParents.contains(edge.getParentTypeName()))
        .forEach(edge -> this.addIssue(edge.getNode(), "Forbidden parent"));
  }

  private Set<TypeString> getForbiddenParents() {
    return Arrays.stream(this.forbiddenParents.split(","))
        .map(TypeStringParser::parseTypeString)
        .collect(Collectors.toSet());
  }
}
