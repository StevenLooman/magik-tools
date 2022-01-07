package nl.ramsolutions.sw.magik.checks.checks;

import com.sonar.sslr.api.AstNode;
import java.util.Objects;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry.Type;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.api.MagikKeyword;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import org.sonar.check.Rule;

/**
 * Check for hiding of variables.
 */
@Rule(key = HidesVariableCheck.CHECK_KEY)
public class HidesVariableCheck extends MagikCheck {

    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final String CHECK_KEY = "HidesVariable";
    private static final String MESSAGE = "Variable definition hides another variable with the same name.";

    @Override
    protected void walkPostVariableDefinition(final AstNode node) {
        final MagikFile magikFile = this.getMagikFile();
        final GlobalScope globalScope = magikFile.getGlobalScope();
        Objects.requireNonNull(globalScope);
        final Scope scope = globalScope.getScopeForNode(node);
        Objects.requireNonNull(scope);

        final AstNode identifierNode = node.getFirstChild(MagikGrammar.IDENTIFIER);
        this.checkIdentifier(scope, identifierNode);
    }

    @Override
    protected void walkPostVariableDefinitionMulti(final AstNode node) {
        final MagikFile magikFile = this.getMagikFile();
        final GlobalScope globalScope = magikFile.getGlobalScope();
        Objects.requireNonNull(globalScope);
        final Scope scope = globalScope.getScopeForNode(node);

        final AstNode identifiersNode = node.getFirstChild(MagikGrammar.IDENTIFIERS_WITH_GATHER);
        for (final AstNode identifierNode : identifiersNode.getChildren(MagikGrammar.IDENTIFIER)) {
            this.checkIdentifier(scope, identifierNode);
        }
    }

    private void checkIdentifier(final Scope scope, final AstNode identifierNode) {
        // Dont check _imports.
        final AstNode varDefStatementNode = AstQuery.getParentFromChain(
            identifierNode,
            MagikGrammar.VARIABLE_DEFINITION, MagikGrammar.VARIABLE_DEFINITION_STATEMENT);
        if (varDefStatementNode != null) {
            final String tokenValue = varDefStatementNode.getTokenValue();
            if (tokenValue != null
                && tokenValue.equalsIgnoreCase(MagikKeyword.IMPORT.getValue())) {
                return;
            }
        }

        final String identifier = identifierNode.getTokenValue();
        for (final Scope ancestorScope : scope.getAncestorScopes()) {
            final ScopeEntry scopeEntry = ancestorScope.getScopeEntry(identifier);
            if (scopeEntry != null
                && scopeEntry.isType(Type.LOCAL)) {
                this.addIssue(identifierNode, MESSAGE);
            }
        }
    }

}
