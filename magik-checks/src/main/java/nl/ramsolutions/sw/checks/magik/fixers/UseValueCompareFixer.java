package nl.ramsolutions.sw.checks.magik.fixers;

import com.sonar.sslr.api.AstNode;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import nl.ramsolutions.sw.OpenedFile;
import nl.ramsolutions.sw.checks.CheckFixer;
import nl.ramsolutions.sw.checks.Issue;
import nl.ramsolutions.sw.checks.MagikCheck;
import nl.ramsolutions.sw.checks.magik.UseValueCompareCheck;
import nl.ramsolutions.sw.magik.CodeAction;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.TextEdit;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/** Fixer for {@link UseValueCompareCheck} issues. */
public class UseValueCompareFixer extends CheckFixer {

  @Override
  public List<CodeAction> provideCodeActions(final OpenedFile openedFile, final Range range) {
    if (!(openedFile instanceof MagikFile magikFile)) {
      return Collections.emptyList();
    }

    // Run the check to get the violation(s).
    final MagikCheck check = new UseValueCompareCheck();
    final List<Issue> issues = check.scanFileForIssues(openedFile);
    final AstNode topNode = magikFile.getTopNode();
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
                  "Replace `%s` operator with `%s` operator"
                      .formatted(operator, replacementOperator),
                  new TextEdit(operatorRange, replacementOperator));
            })
        .toList();
  }
}
