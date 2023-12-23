package nl.ramsolutions.sw.magik.checks.checks;

import com.sonar.sslr.api.AstNode;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

/**
 * Check for used globals which are forbidden.
 */
@Rule(key = ForbiddenGlobalUsageCheck.CHECK_KEY)
public class ForbiddenGlobalUsageCheck extends MagikCheck {

    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final String CHECK_KEY = "ForbiddenGlobalUsage";
    private static final String MESSAGE = "Usage of global '%s' is forbidden.";

    private static final String DEFAULT_FORBIDDEN_GLOBALS = "!current_grs!,sw:!current_grs!";

    /**
     * List of forbidden calls, separated by ','.
     */
    @RuleProperty(
        key = "forbidden globals",
        defaultValue = "" + DEFAULT_FORBIDDEN_GLOBALS,
        description = "List of forbidden globals, separated by ','",
        type = "STRING")
    @SuppressWarnings("checkstyle:VisibilityModifier")
    public String forbiddenGlobals = DEFAULT_FORBIDDEN_GLOBALS;

    private Set<String> getForbiddenGlobals() {
        return Arrays.stream(this.forbiddenGlobals.split(","))
            .map(String::trim)
            .collect(Collectors.toSet());
    }

    @Override
    protected void walkPostVariableDefinition(final AstNode node) {
        node.getChildren(MagikGrammar.IDENTIFIER).stream()
            .forEach(this::checkIdentifier);
    }

    @Override
    protected void walkPostVariableDefinitionMulti(final AstNode node) {
        final AstNode identifiersNode = node.getFirstChild(MagikGrammar.IDENTIFIERS_WITH_GATHER);
        identifiersNode.getChildren(MagikGrammar.IDENTIFIER)
            .forEach(this::checkIdentifier);
    }

    @Override
    protected void walkPostAtom(final AstNode node) {
        final AstNode identifierNode = node.getFirstChild(MagikGrammar.IDENTIFIER);
        if (identifierNode == null) {
            return;
        }
        this.checkIdentifier(identifierNode);
    }

    private void checkIdentifier(final AstNode node) {
        // Get bare identifier, without package.
        final String tokenValue = node.getTokenValue();
        final int index = tokenValue.indexOf(':');
        final String identifier = index != -1
            ? tokenValue.substring(index + 1)
            : tokenValue;

        // Ensure global/dynamic.
        final GlobalScope globalScope = this.getMagikFile().getGlobalScope();
        final Scope scope = globalScope.getScopeForNode(node);
        Objects.requireNonNull(scope);
        final ScopeEntry scopeEntry = scope.getLocalScopeEntry(identifier);
        if (scopeEntry == null) {
            // Robustness.
            return;
        }

        // Even though the variable can be a DEFINITION, treat it as a global
        // when it starts with a "!". Developers are more likely to incline it
        // is a global.
        if (!scopeEntry.isType(ScopeEntry.Type.GLOBAL, ScopeEntry.Type.DYNAMIC)
            && !identifier.startsWith("!")) {
            return;
        }

        final Set<String> globals = this.getForbiddenGlobals();
        if (globals.contains(identifier)) {
            final String message = String.format(MESSAGE, identifier);
            this.addIssue(node, message);
        }
    }

}
