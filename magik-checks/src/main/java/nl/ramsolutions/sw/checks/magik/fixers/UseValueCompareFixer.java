package nl.ramsolutions.sw.checks.magik.fixers;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import java.util.Objects;
import nl.ramsolutions.sw.checks.Issue;
import nl.ramsolutions.sw.checks.MagikCheck;
import nl.ramsolutions.sw.checks.MagikCheckFixer;
import nl.ramsolutions.sw.checks.magik.UseValueCompareCheck;
import nl.ramsolutions.sw.magik.CodeAction;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.TextEdit;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/** Fixer for {@link UseValueCompareCheck} issues. */
public class UseValueCompareFixer extends MagikCheckFixer {

  @Override
  public List<CodeAction> provideCodeActions(final MagikFile file, final Range range) {
    // Run the check to get the violation(s).
    final MagikCheck check = new UseValueCompareCheck();
    final List<Issue> issues = check.scanFileForIssues(file);
    final AstNode topNode = file.getTopNode();
    return issues.stream()
        .map(Issue::range)
        .filter(issueRange -> range.overlapsWith(issueRange))
        .map(
            issueRange ->
                AstQuery.nodeSurrounding(
                    topNode, issueRange.getStartPosition(), MagikGrammar.EQUALITY_EXPRESSION))
        .filter(Objects::nonNull)
        .map(
            node -> {
              // Create a CodeAction to replace `_is`/`_isnt` with `=`/`<>`.
              final String operator = node.getChildren().get(1).getTokenValue();
              final String replacementOperator = operator.equals("_is") ? "=" : "<>";
              final Range operatorRange = Range.fromTree(node.getChildren().get(1));
              return new CodeAction(
                  String.format(
                      "Replace `%s` operator with `%s` operator", operator, replacementOperator),
                  new TextEdit(operatorRange, replacementOperator));
            })
        .toList();
  }
}
