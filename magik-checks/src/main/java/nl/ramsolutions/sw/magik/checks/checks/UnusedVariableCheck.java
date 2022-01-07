package nl.ramsolutions.sw.magik.checks.checks;

import com.sonar.sslr.api.AstNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.api.MagikOperator;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import org.sonar.check.Rule;

/**
 * Check for unused vairables.
 */
@Rule(key = UnusedVariableCheck.CHECK_KEY)
public class UnusedVariableCheck extends MagikCheck {

    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final String CHECK_KEY = "UnusedVariable";
    private static final String MESSAGE = "Remove the unused local variable \"%s\".";

    private boolean checkParameters;

    public UnusedVariableCheck() {
        this.checkParameters = false;
    }

    public UnusedVariableCheck(final boolean checkParameters) {
        this.checkParameters = checkParameters;
    }

    private boolean isAssignedToDirectly(final AstNode identifierNode) {
        final AstNode variableDefinitionNode = identifierNode.getParent();
        if (!variableDefinitionNode.is(MagikGrammar.VARIABLE_DEFINITION)) {
            return false;
        }

        if (variableDefinitionNode.getFirstChild(MagikGrammar.IDENTIFIER) != identifierNode) {
            return false;
        }

        return variableDefinitionNode.getTokens().stream()
            .anyMatch(token -> token.getValue().equals(MagikOperator.CHEVRON.getValue())
                            || token.getValue().equals(MagikOperator.BOOT_CHEVRON.getValue()));
    }

    private boolean isPartOfMultiVariableDefinition(final AstNode identifierNode) {
        final AstNode identifiersWithGatherNode = identifierNode.getParent();
        if (identifiersWithGatherNode == null
            || !identifiersWithGatherNode.is(MagikGrammar.IDENTIFIERS_WITH_GATHER)) {
            return false;
        }

        final AstNode multiVarDeclNode = identifiersWithGatherNode.getParent();
        if (multiVarDeclNode == null
            || !multiVarDeclNode.is(MagikGrammar.VARIABLE_DEFINITION_MULTI)) {
            return false;
        }

        return true;
    }

    private boolean isPartOfMultiAssignment(final AstNode identifierNode) {
        final AstNode atomNode = identifierNode.getParent();
        if (atomNode == null
            || !atomNode.is(MagikGrammar.ATOM)) {
            return false;
        }

        final AstNode expressionNode = atomNode.getParent();
        if (expressionNode == null
            || !expressionNode.is(MagikGrammar.EXPRESSION)) {
            return false;
        }

        final AstNode assignablesNode = expressionNode.getParent();
        return assignablesNode != null
            && assignablesNode.is(MagikGrammar.MULTIPLE_ASSIGNMENT_ASSIGNABLES);
    }

    private boolean anyNextSiblingUsedMultiVarDef(final AstNode identifierNode) {
        final GlobalScope globalScope = this.getMagikFile().getGlobalScope();
        final Scope scope = globalScope.getScopeForNode(identifierNode);
        Objects.requireNonNull(scope);
        AstNode sibling = identifierNode.getNextSibling();
        while (sibling != null) {
            if (!sibling.is(MagikGrammar.IDENTIFIER)) {
                sibling = sibling.getNextSibling();
                continue;
            }

            final String siblingIdentifier = sibling.getTokenValue();
            final ScopeEntry siblingEntry = scope.getScopeEntry(siblingIdentifier);
            if (siblingEntry != null
                    && !siblingEntry.getUsages().isEmpty()) {
                return true;
            }

            sibling = sibling.getNextSibling();
        }

        return false;
    }

    @Override
    protected void walkPostMagik(final AstNode node) {
        // Gather all scope entries to be checked
        final List<ScopeEntry> scopeEntries = this.getCheckableScopeEntries();

        // Remove all defined scope entries which are:
        // - part of a VARIABLE_DEFINITION_STATEMENT/MULTIPLE_ASSIGNMENT_STATEMENT
        // - any later identifier(s) of it is used
        // - but this one is not
        for (final ScopeEntry entry : new ArrayList<>(scopeEntries)) {
            final AstNode entryNode = entry.getNode();
            if (this.isPartOfMultiVariableDefinition(entryNode)
                && this.anyNextSiblingUsedMultiVarDef(entryNode)
                || this.isPartOfMultiAssignment(entryNode)) {
                scopeEntries.remove(entry);
            }
        }

        // Report all unused scope entries
        for (final ScopeEntry entry : scopeEntries) {
            if (!entry.getUsages().isEmpty()) {
                continue;
            }

            final AstNode entryNode = entry.getNode();
            final String name = entryNode.getTokenValue();
            final String message = String.format(MESSAGE, name);
            this.addIssue(entryNode, message);
        }
    }

    private List<ScopeEntry> getCheckableScopeEntries() {
        final List<ScopeEntry> scopeEntries = new ArrayList<>();
        final GlobalScope globalScope = this.getMagikFile().getGlobalScope();
        for (final Scope scope : globalScope.getSelfAndDescendantScopes()) {
            for (final ScopeEntry scopeEntry : scope.getScopeEntriesInScope()) {
                final AstNode scopeEntryNode = scopeEntry.getNode();

                // But not globals/dynamics which are assigned to directly
                if ((scopeEntry.isType(ScopeEntry.Type.GLOBAL) || scopeEntry.isType(ScopeEntry.Type.DYNAMIC))
                    && this.isAssignedToDirectly(scopeEntryNode)) {
                    continue;
                }

                // No parameters, unless forced to
                if (!this.checkParameters
                    && scopeEntry.isType(ScopeEntry.Type.PARAMETER)) {
                    continue;
                }

                scopeEntries.add(scopeEntry);
            }
        }
        return scopeEntries;
    }

}
