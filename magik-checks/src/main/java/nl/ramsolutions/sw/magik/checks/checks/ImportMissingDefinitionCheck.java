package nl.ramsolutions.sw.magik.checks.checks;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry.Type;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import org.sonar.check.Rule;

/**
 * Check for missing _imports.
 */
@Rule(key = ImportMissingDefinitionCheck.CHECK_KEY)
public class ImportMissingDefinitionCheck extends MagikCheck {

    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final String CHECK_KEY = "ImportMissingDefinition";
    private static final String MESSAGE = "_import '%s' is missing definition.";

    @Override
    protected void walkPreVariableDefinitionStatement(AstNode node) {
        // Ensure it is an _import (and not a _dynamic _import).
        final boolean isImport = node.getChildren(MagikGrammar.VARIABLE_DEFINITION_MODIFIER).stream()
            .allMatch(modifierNode -> modifierNode.getTokenValue().equalsIgnoreCase("_import"));
        if (!isImport) {
            return;
        }

        // Ready the scope.
        final MagikFile magikFile = this.getMagikFile();
        final GlobalScope globalScope = magikFile.getGlobalScope();
        final Scope scope = globalScope.getScopeForNode(node);

        // Iterate over all defined variables and check if parent definition exists.
        node.getChildren(MagikGrammar.VARIABLE_DEFINITION).stream()
            .map(variableDefinitionNode ->
                variableDefinitionNode.getFirstChild(MagikGrammar.IDENTIFIER))
            .forEach(identifierNode -> {
                final String identifier = identifierNode.getTokenValue();
                final ScopeEntry scopeEntry = scope.getScopeEntry(identifier);
                if (scopeEntry.isType(Type.IMPORT)
                    && scopeEntry.getImportedEntry() == null) {
                    final String originalIdentifier = identifierNode.getTokenOriginalValue();
                    final String message = String.format(MESSAGE, originalIdentifier);
                    this.addIssue(identifierNode, message);
                }
            });
    }

}
