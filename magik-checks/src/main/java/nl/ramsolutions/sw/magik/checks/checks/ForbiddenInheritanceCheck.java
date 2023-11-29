package nl.ramsolutions.sw.magik.checks.checks;

import com.sonar.sslr.api.AstNode;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.definitions.Definition;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.checks.DisabledByDefault;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.parser.TypeStringParser;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

/**
 * Check if forbidden inheritance is used.
 */
@DisabledByDefault
@Rule(key = ForbiddenInheritanceCheck.CHECK_KEY)
public class ForbiddenInheritanceCheck extends MagikCheck {

    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final String CHECK_KEY = "ForbiddenInheritance";

    private static final String DEFAULT_FORBIDDEN_PARENTS = "";

    /**
     * Forbidden parents to inhertit from, separated by ','.
     */
    @RuleProperty(
        key = "forbidden parents",
        defaultValue = "" + DEFAULT_FORBIDDEN_PARENTS,
        description = "Forbidden parents to inhertit from, separated by ','",
        type = "STRING")
    @SuppressWarnings("checkstyle:VisibilityModifier")
    public String forbiddenParents = DEFAULT_FORBIDDEN_PARENTS;

    @Override
    protected void walkPostMagik(final AstNode node) {
        if (this.forbiddenParents.isBlank()) {
            return;
        }

        final MagikFile magikFile = this.getMagikFile();
        magikFile.getDefinitions().stream()
            .filter(definition -> definition instanceof ExemplarDefinition)
            .filter(this::isForbiddenParent)
            .forEach(definition -> this.addIssue(definition.getNode(), "Forbidden parent"));
    }

    private boolean isForbiddenParent(final Definition definition) {
        final ExemplarDefinition exemplarDefinition = (ExemplarDefinition) definition;
        final List<TypeString> parents = exemplarDefinition.getParents();
        final Set<TypeString> theForbiddenParents = this.getForbiddenParents();
        return theForbiddenParents.stream().anyMatch(parents::contains);
    }

    private Set<TypeString> getForbiddenParents() {
        return Arrays.stream(this.forbiddenParents.split(","))
            .map(TypeStringParser::parseTypeString)
            .collect(Collectors.toSet());
    }

}
