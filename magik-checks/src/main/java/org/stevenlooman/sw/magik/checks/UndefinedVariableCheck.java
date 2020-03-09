package org.stevenlooman.sw.magik.checks;

import com.sonar.sslr.api.AstNode;
import org.sonar.check.Rule;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.analysis.scope.GlobalScope;
import org.stevenlooman.sw.magik.analysis.scope.Scope;
import org.stevenlooman.sw.magik.analysis.scope.ScopeEntry;
import org.stevenlooman.sw.magik.api.MagikGrammar;

@Rule(key = UndefinedVariableCheck.CHECK_KEY)
public class UndefinedVariableCheck extends MagikCheck {

  public static final String CHECK_KEY = "UndefinedVariable";
  private static final String MESSAGE =
      "Variable '%s' is expected to be declared, but used as a global.";

  @Override
  protected void walkPostMethodDefinition(AstNode node) {
    checkForPrefixedGlobals(node);
  }

  @Override
  protected void walkPostProcDefinition(AstNode node) {
    checkForPrefixedGlobals(node);
  }

  /**
   * Test if any global variable in the method/procedure scope has a prefix.
   * @param node Node to check.
   */
  private void checkForPrefixedGlobals(AstNode node) {
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
        .filter(scopeEntry -> isPrefixed(scopeEntry.getIdentifier()))
        .forEach(scopeEntry -> {
          AstNode scopeEntryNode = scopeEntry.getNode();
          String identifier = scopeEntry.getIdentifier();
          String message = String.format(MESSAGE, identifier);
          addIssue(message, scopeEntryNode);
        });
  }

  private boolean isPrefixed(String identifier) {
    String lowerCased = identifier.toLowerCase();
    return lowerCased.startsWith("l_")
           || lowerCased.startsWith("i_")
           || lowerCased.startsWith("p_")
           || lowerCased.startsWith("c_");
  }

}
