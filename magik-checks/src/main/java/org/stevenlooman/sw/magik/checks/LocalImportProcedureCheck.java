package org.stevenlooman.sw.magik.checks;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import org.sonar.check.Rule;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.analysis.scope.GlobalScope;
import org.stevenlooman.sw.magik.analysis.scope.Scope;
import org.stevenlooman.sw.magik.analysis.scope.ScopeEntry;
import org.stevenlooman.sw.magik.api.MagikGrammar;

import java.util.Arrays;
import java.util.List;

@Rule(key = LocalImportProcedureCheck.CHECK_KEY)
public class LocalImportProcedureCheck extends MagikCheck {

  private static final String MESSAGE =
      "Possibly meant _import instead of _local.";
  public static final String CHECK_KEY = "LocalImportProcedure";

  @Override
  public boolean isTemplatedCheck() {
    return false;
  }

  @Override
  public List<AstNodeType> subscribedTo() {
    return Arrays.asList(
        MagikGrammar.METHOD_DEFINITION);
  }

  @Override
  public void visitNode(AstNode node) {
    List<AstNode> procDefNodes = node.getDescendants(MagikGrammar.PROC_DEFINITION);
    if (procDefNodes.isEmpty()) {
      return;
    }

    // Get scope.
    GlobalScope globalScope = getContext().getGlobalScope();

    // get all proc scopes
    for (AstNode procDefNode: procDefNodes) {
      Scope procScope = globalScope.getScopeForNode(procDefNode);

      // get all parent scopes from procedure
      List<Scope> parentScopes = procScope.getAncestorScopes();

      // get procedure scope and all child scopes from procedure
      List<Scope> childScopes = procScope.getSelfAndDescendantScopes();

      // see if any overlap
      for (Scope scope: childScopes) {
        for (ScopeEntry scopeEntry: scope.getScopeEntries()) {
          boolean found = false;
          if (scopeEntry.getType() != ScopeEntry.Type.LOCAL
              && scopeEntry.getType() != ScopeEntry.Type.DEFINITION) {
            continue;
          }

          String identifier = scopeEntry.getIdentifier();
          for (Scope parentScope: parentScopes) {
            if (parentScope.getScopeEntry(identifier) != null) {
              addIssue(MESSAGE, scopeEntry.getNode());
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

  }

}

