package org.stevenlooman.sw.magik.checks;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.analysis.scope.GlobalScope;
import org.stevenlooman.sw.magik.analysis.scope.Scope;
import org.stevenlooman.sw.magik.analysis.scope.ScopeEntry;
import org.stevenlooman.sw.magik.api.MagikGrammar;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Rule(key = ScopeCountCheck.CHECK_KEY)
public class ScopeCountCheck extends MagikCheck {

  public static final String CHECK_KEY = "ScopeCount";
  private static final String MESSAGE = "Too many variables in scope.";
  private static final int DEFAULT_MAX_SCOPE_COUNT = 20;
  @RuleProperty(
      key = "max scope count",
      defaultValue = "" + DEFAULT_MAX_SCOPE_COUNT,
      description = "Maximum number of entries in scope")
  public int maxScopeCount = DEFAULT_MAX_SCOPE_COUNT;

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
   * Test if there are too many entries in the method/procedure scope.
   * @param node Node to check.
   */
  @Override
  public void leaveNode(AstNode node) {
    GlobalScope globalScope = getContext().getGlobalScope();
    if (globalScope == null) {
      return;
    }

    Scope procedureScope = globalScope.getScopeForNode(node);
    List<ScopeEntry> procedureScopeEntries = procedureScope.getSelfAndDescendantScopes().stream()
        .map(scope -> scope.getScopeEntries())
        .flatMap(scopeEntries -> scopeEntries.stream())
        .filter(scopeEntry -> scopeEntry.getType() != ScopeEntry.Type.IMPORT)
        .collect(Collectors.toList());
    if (procedureScopeEntries.size() > maxScopeCount) {
      addIssue(MESSAGE, node);
    }
  }

}
