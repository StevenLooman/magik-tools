package org.stevenlooman.sw.magik.checks;

import com.sonar.sslr.api.AstNode;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.analysis.scope.GlobalScope;
import org.stevenlooman.sw.magik.analysis.scope.Scope;
import org.stevenlooman.sw.magik.analysis.scope.ScopeEntry;
import org.stevenlooman.sw.magik.api.MagikGrammar;

import java.util.List;
import java.util.stream.Collectors;

@Rule(key = ScopeCountCheck.CHECK_KEY)
public class ScopeCountCheck extends MagikCheck {

  public static final String CHECK_KEY = "ScopeCount";
  private static final String MESSAGE = "Too many variables in scope (%s/%s).";
  private static final int DEFAULT_MAX_SCOPE_COUNT = 25;

  @RuleProperty(
      key = "max scope count",
      defaultValue = "" + DEFAULT_MAX_SCOPE_COUNT,
      description = "Maximum number of entries in scope")
  public int maxScopeCount = DEFAULT_MAX_SCOPE_COUNT;

  @Override
  protected void walkPostMethodDefinition(AstNode node) {
    checkScopeCount(node);
  }

  @Override
  protected void walkPostProcDefinition(AstNode node) {
    checkScopeCount(node);
  }

  /**
   * Test if there are too many entries in the method/procedure scope.
   * @param node Node to check.
   */
  private void checkScopeCount(AstNode node) {
    GlobalScope globalScope = getContext().getGlobalScope();
    if (globalScope == null) {
      return;
    }

    AstNode bodyNode = node.getFirstChild(MagikGrammar.BODY);
    Scope procedureScope = globalScope.getScopeForNode(bodyNode);
    List<ScopeEntry> procedureScopeEntries = procedureScope.getSelfAndDescendantScopes().stream()
        .map(scope -> scope.getScopeEntries())
        .flatMap(scopeEntries -> scopeEntries.stream())
        .filter(scopeEntry -> scopeEntry.getType() != ScopeEntry.Type.IMPORT)
        .collect(Collectors.toList());
    int scopeCount = procedureScopeEntries.size();
    if (scopeCount > maxScopeCount) {
      String message = String.format(MESSAGE, scopeCount, maxScopeCount);
      addIssue(message, node);
    }
  }

}
