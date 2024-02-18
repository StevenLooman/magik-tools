package nl.ramsolutions.sw.magik.checks.checks;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import org.sonar.check.Rule;

/** Check if _import was meant to be used instead of _local. */
@Rule(key = LocalImportProcedureCheck.CHECK_KEY)
public class LocalImportProcedureCheck extends MagikCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "LocalImportProcedure";

  private static final String MESSAGE = "Possibly meant _import instead of _local.";

  @Override
  public void walkPreMethodDefinition(final AstNode node) {
    final List<AstNode> procDefNodes = node.getDescendants(MagikGrammar.PROCEDURE_DEFINITION);
    if (procDefNodes.isEmpty()) {
      return;
    }

    // Get scope.
    final GlobalScope globalScope = this.getMagikFile().getGlobalScope();

    // Get all proc scopes.
    for (final AstNode procDefNode : procDefNodes) {
      this.checkProcDefNode(globalScope, procDefNode);
    }
  }

  @SuppressWarnings("checkstyle:NestedForDepth")
  private void checkProcDefNode(final GlobalScope globalScope, final AstNode procDefNode) {
    final AstNode bodyNode = procDefNode.getFirstChild(MagikGrammar.BODY);
    if (bodyNode == null) {
      return;
    }
    final Scope procScope = globalScope.getScopeForNode(bodyNode);
    if (procScope == null) {
      return;
    }

    // Get all parent scopes from procedure.
    final List<Scope> parentScopes = procScope.getAncestorScopes();

    // Get procedure scope and all child scopes from procedure.
    final List<Scope> childScopes = procScope.getSelfAndDescendantScopes();

    // Test all scopes for overlap.
    for (final Scope scope : childScopes) {
      this.checkScope(scope, parentScopes);
    }
  }

  private void checkScope(final Scope scope, final List<Scope> parentScopes) {
    // See if any ScopeEntry overlaps.
    for (final ScopeEntry scopeEntry : scope.getScopeEntriesInScope()) {
      boolean found = false;

      // Only test LOCAL/DEFINITION ScopeEntry.
      if (!scopeEntry.isType(ScopeEntry.Type.LOCAL)
          && !scopeEntry.isType(ScopeEntry.Type.DEFINITION)) {
        return;
      }

      // Test for overlap.
      final String identifier = scopeEntry.getIdentifier();
      for (final Scope parentScope : parentScopes) {
        if (parentScope.getScopeEntry(identifier) != null) {
          this.addIssue(scopeEntry.getDefinitionNode(), MESSAGE);
          found = true;
        }
      }

      if (found) {
        // stop checking this scopeEntry, issue already reported
        break;
      }
    }
  }
}
