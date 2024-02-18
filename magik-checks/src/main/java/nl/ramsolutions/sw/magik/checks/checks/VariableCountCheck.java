package nl.ramsolutions.sw.magik.checks.checks;

import com.sonar.sslr.api.AstNode;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.ProcedureDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

/** Check number of variables in method/procedure. */
@Rule(key = VariableCountCheck.CHECK_KEY)
public class VariableCountCheck extends MagikCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "VariableCount";

  private static final int DEFAULT_MAXIMUM_VARIABLE_COUNT = 8;
  private static final String MESSAGE =
      "Method contains more variables than than permitted (%s/%s).";

  /** Maximum length of method in lines, without whitelines and comment lines. */
  @RuleProperty(
      key = "maximum variable count",
      defaultValue = "" + DEFAULT_MAXIMUM_VARIABLE_COUNT,
      description = "Maximum number of variables in method",
      type = "INTEGER")
  @SuppressWarnings("checkstyle:VisibilityModifier")
  public int maximumVariableCount = DEFAULT_MAXIMUM_VARIABLE_COUNT;

  @Override
  protected void walkPreMethodDefinition(final AstNode node) {
    this.checkDefinition(node);
  }

  @Override
  protected void walkPreProcedureDefinition(final AstNode node) {
    this.checkDefinition(node);
  }

  private void checkDefinition(final AstNode node) {
    final AstNode body = node.getFirstChild(MagikGrammar.BODY);
    if (body == null) {
      // Possibly a syntax error.
      return;
    }

    final MagikFile magikFile = this.getMagikFile();
    final GlobalScope globalScope = magikFile.getGlobalScope();
    final Scope scope = globalScope.getScopeForNode(body);
    if (scope == null) {
      return;
    }

    final long variableCount =
        scope.getScopeEntriesInScope().stream()
            .filter(
                scopeEntry ->
                    scopeEntry.isType(
                        ScopeEntry.Type.CONSTANT,
                        ScopeEntry.Type.DEFINITION,
                        ScopeEntry.Type.LOCAL))
            .collect(Collectors.counting());
    if (variableCount > this.maximumVariableCount) {
      final String message = String.format(MESSAGE, variableCount, this.maximumVariableCount);
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
