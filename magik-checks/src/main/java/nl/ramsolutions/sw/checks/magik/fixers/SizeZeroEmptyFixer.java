package nl.ramsolutions.sw.checks.magik.fixers;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.OpenedFile;
import nl.ramsolutions.sw.checks.CheckFixer;
import nl.ramsolutions.sw.checks.Issue;
import nl.ramsolutions.sw.checks.MagikCheck;
import nl.ramsolutions.sw.checks.magik.SizeZeroEmptyCheck;
import nl.ramsolutions.sw.magik.CodeAction;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.TextEdit;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/** Fixer for {@link SizeZeroEmptyCheck} issues. */
public class SizeZeroEmptyFixer extends CheckFixer {

  @Override
  public List<CodeAction> provideCodeActions(final OpenedFile openedFile, final Range range) {
    if (!(openedFile instanceof MagikFile magikFile)) {
      return Collections.emptyList();
    }

    // Run the check to get the violation(s).
    final MagikCheck check = new SizeZeroEmptyCheck();
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
              // Create a CodeAction to replace `.size = 0` with `.empty?`.
              final AstNode leftHandSide = node.getFirstChild();
              final AstNode rightHandSide = node.getLastChild();
              final String leftHandSideText =
                  leftHandSide.getTokens().stream()
                      .map(Token::getValue)
                      .collect(Collectors.joining());
              final String rightHandSideText =
                  rightHandSide.getTokens().stream()
                      .map(Token::getValue)
                      .collect(Collectors.joining());
              final String replacement =
                  leftHandSideText.endsWith(".size") && "0".equals(rightHandSideText)
                      ? leftHandSideText.substring(0, leftHandSideText.length() - 5) + ".empty?"
                      : rightHandSideText.substring(0, rightHandSideText.length() - 5) + ".empty?";

              final Range replacementRange = Range.fromTree(node);
              final TextEdit textEdit = new TextEdit(replacementRange, replacement);
              return new CodeAction("Replace size comparison with `.empty?`", textEdit);
            })
        .toList();
  }
}
