package org.stevenlooman.sw.magik.checks;

import com.sonar.sslr.api.AstNode;

import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.analysis.scope.GlobalScope;
import org.stevenlooman.sw.magik.analysis.scope.Scope;
import org.stevenlooman.sw.magik.analysis.scope.ScopeEntry;
import org.stevenlooman.sw.magik.api.MagikGrammar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Rule(key = VariableDeclarationUsageDistanceCheck.CHECK_KEY)
public class VariableDeclarationUsageDistanceCheck extends MagikCheck {
  public static final String CHECK_KEY = "VariableDeclarationUsageDistance";
  private static final String MESSAGE =
      "Distance of declared variable (%s) to first usage is too long (%s/%s).";

  private static final int DEFAULT_MAX_DISTANCE = 5;
  @RuleProperty(
      key = "max distance",
      defaultValue = "" + DEFAULT_MAX_DISTANCE,
      description = "Maximum distance between declaration and usage")
  public int maxDistance = DEFAULT_MAX_DISTANCE;

  private static final boolean DEFAULT_IGNORE_CONSTANTS = true;
  @RuleProperty(
      key = "ignore constants",
      defaultValue = "" + DEFAULT_IGNORE_CONSTANTS,
      description = "Ignore declared constants")
  public boolean ignoreConstants = DEFAULT_IGNORE_CONSTANTS;

  private Set<AstNode> seenNodes = new HashSet<>();
  private List<AstNode> statementNodes = new ArrayList<>();

  @Override
  protected void walkPostMethodDefinition(AstNode node) {
    this.seenNodes.clear();
  }

  @Override
  protected void walkPreStatement(AstNode node) {
    // Record all statements in a list, flattening.
    statementNodes.add(node);
  }

  @Override
  protected void walkPreIdentifier(AstNode node) {
    // Only test variable usages, i.e., parent is an ATOM node.
    if (node.getParent().getType() != MagikGrammar.ATOM) {
      return;
    }

    // Don't test if left hand side of a ASSIGNMENT_EXPRESSION.
    AstNode atomNode = node.getParent();
    if (atomNode.getParent().getType() == MagikGrammar.ASSIGNMENT_EXPRESSION
        && atomNode.getParent().getChildren().get(0) == atomNode) {
      return;
    }

    // Test only METHOD_DEFINITION/PROC_DEFINITION.
    if (node.getFirstAncestor(MagikGrammar.METHOD_DEFINITION) == null
        && node.getFirstAncestor(MagikGrammar.PROC_DEFINITION) == null) {
      return;
    }

    // Get the scope where it is used.
    GlobalScope globalScope = getContext().getGlobalScope();
    Scope scope = globalScope.getScopeForNode(node);
    if (scope == globalScope) {
      // No scope found.
      return;
    }

    // Only test locals/local definitions.
    String identifier = node.getTokenValue();
    ScopeEntry entry = scope.getScopeEntry(identifier);
    if (entry.getType() != ScopeEntry.Type.LOCAL
        && entry.getType() != ScopeEntry.Type.DEFINITION) {
      return;
    }

    // Only test the first use.
    AstNode declarationNode = entry.getNode();
    if (declarationNode == node
        || this.seenNodes.contains(declarationNode)) {
      return;
    }
    this.seenNodes.add(declarationNode);

    int distance = distanceBetweenStatements(declarationNode, node);
    if (distance > maxDistance) {
      String message = String.format(MESSAGE, identifier, distance, maxDistance);
      addIssue(message, node);
    }
  }

  @Override
  public void leaveFile(AstNode node) {
    this.seenNodes.clear();
  }

  private int distanceBetweenStatements(AstNode declarationNode, AstNode usageNode) {
    AstNode declarationStatementNode = declarationNode.getFirstAncestor(MagikGrammar.STATEMENT);
    int declarationIndex = this.statementNodes.indexOf(declarationStatementNode);
    AstNode usageStatementNode = usageNode.getFirstAncestor(MagikGrammar.STATEMENT);
    int usageIndex = this.statementNodes.indexOf(usageStatementNode);
    return usageIndex - declarationIndex;
  }

}
