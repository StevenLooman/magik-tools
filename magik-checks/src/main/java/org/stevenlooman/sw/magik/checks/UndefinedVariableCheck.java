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

@Rule(key = UndefinedVariableCheck.CHECK_KEY)
public class UndefinedVariableCheck extends MagikCheck {

  public static final String CHECK_KEY = "UndefinedVariable";
  private static final String MESSAGE =
      "Variable %s is expected to be declared, but used as a global.";

  @Override
  public boolean isTemplatedCheck() {
    return false;
  }

  @Override
  public List<AstNodeType> subscribedTo() {
    return Arrays.asList(
      MagikGrammar.METHOD_DEFINITION,
      MagikGrammar.PROC_DEFINITION);
  }

  /**
   * Test if any global variable in the method/procedure scope has a prefix.
   * @param node Node to check.
   */
  @Override
  public void leaveNode(AstNode node) {
    GlobalScope globalScope = getContext().getGlobalScope();
    if (globalScope == null) {
      return;
    }

    AstNode bodyNode = node.getFirstChild(MagikGrammar.BODY);
    Scope procedureScope = globalScope.getScopeForNode(bodyNode);
    procedureScope.getSelfAndDescendantScopes().stream()
        .map(scope -> scope.getScopeEntries())
        .flatMap(scopeEntries -> scopeEntries.stream())
        .filter(scopeEntry -> scopeEntry.getType() == ScopeEntry.Type.GLOBAL)
        .filter(scopeEntry -> {
          String identifier = scopeEntry.getIdentifier().toLowerCase();
          return identifier.startsWith("l_")
                 || identifier.startsWith("i_")
                 || identifier.startsWith("p_")
                 || identifier.startsWith("c_");
        })
        .forEach(scopeEntry -> {
          AstNode scopeEntryNode = scopeEntry.getNode();
          String identifier = scopeEntry.getIdentifier();
          String message = String.format(MESSAGE, identifier);
          addIssue(message, scopeEntryNode);
        });
  }

}
