package nl.ramsolutions.sw.magik.checks.checks;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import org.sonar.check.Rule;

/**
 * Check for undefined prefixed variables.
 */
@Rule(key = UndefinedVariableCheck.CHECK_KEY)
public class UndefinedVariableCheck extends MagikCheck {

    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final String CHECK_KEY = "UndefinedVariable";
    private static final String MESSAGE = "Variable '%s' is expected to be declared, but used as a global.";

    @Override
    protected void walkPostMagik(final AstNode node) {
        final GlobalScope globalScope = this.getMagikFile().getGlobalScope();
        globalScope.getSelfAndDescendantScopes().stream()
            .flatMap(scope -> scope.getScopeEntriesInScope().stream())
            .filter(scopeEntry -> scopeEntry.isType(ScopeEntry.Type.GLOBAL))
            .filter(scopeEntry -> this.isPrefixed(scopeEntry.getIdentifier()))
            .forEach(scopeEntry -> {
                AstNode scopeEntryNode = scopeEntry.getNode();
                String identifier = scopeEntry.getIdentifier();
                String message = String.format(MESSAGE, identifier);
                this.addIssue(scopeEntryNode, message);
            });
    }

    private boolean isPrefixed(final String identifier) {
        final String lowerCased = identifier.toLowerCase();
        return lowerCased.startsWith("l_")
            || lowerCased.startsWith("i_")
            || lowerCased.startsWith("p_")
            || lowerCased.startsWith("c_");
    }

}
