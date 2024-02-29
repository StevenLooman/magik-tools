package nl.ramsolutions.sw.magik.checks.checks;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.ProcedureDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

/** Check for maximum scope count. */
@Rule(key = ScopeCountCheck.CHECK_KEY)
public class ScopeCountCheck extends MagikCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "ScopeCount";

  private static final String MESSAGE = "Too many variables in scope (%s/%s).";
  private static final int DEFAULT_MAX_SCOPE_COUNT = 25;

  /** Maximum number of entries in scope. */
  @RuleProperty(
      key = "max scope count",
      defaultValue = "" + DEFAULT_MAX_SCOPE_COUNT,
      description = "Maximum number of entries in scope",
      type = "INTEGER")
  @SuppressWarnings("checkstyle:VisibilityModifier")
  public int maxScopeCount = DEFAULT_MAX_SCOPE_COUNT;

  @Override
  protected void walkPostMethodDefinition(final AstNode node) {
    this.checkScopeCount(node);
  }

  @Override
  protected void walkPostProcedureDefinition(final AstNode node) {
    this.checkScopeCount(node);
  }

  /**
   * Test if there are too many entries in the method/procedure scope.
   *
   * @param node Node to check.
   */
  private void checkScopeCount(final AstNode node) {
    final GlobalScope globalScope = this.getMagikFile().getGlobalScope();
    if (globalScope == null) {
      return;
    }

    final AstNode bodyNode = node.getFirstChild(MagikGrammar.BODY);
    if (bodyNode == null) {
      // Probably a SYNTAX_ERROR
      return;
    }

    final Scope procedureScope = globalScope.getScopeForNode(bodyNode);
    Objects.requireNonNull(procedureScope);
    final List<ScopeEntry> procedureScopeEntries =
        procedureScope.getSelfAndDescendantScopes().stream()
            .flatMap(scope -> scope.getScopeEntriesInScope().stream())
            .filter(scopeEntry -> !scopeEntry.isType(ScopeEntry.Type.IMPORT))
            .collect(Collectors.toList());
    final int scopeCount = procedureScopeEntries.size();
    if (scopeCount > this.maxScopeCount) {
      final String message = String.format(MESSAGE, scopeCount, this.maxScopeCount);
      final AstNode markedNode;
      if (node.is(MagikGrammar.METHOD_DEFINITION)) {
        final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(node);
        markedNode = helper.getMethodNameNode();
      } else {
        final ProcedureDefinitionNodeHelper helper = new ProcedureDefinitionNodeHelper(node);
        markedNode = helper.getProcedureNode();
      }
      this.addIssue(markedNode, message);
    }
  }
}
