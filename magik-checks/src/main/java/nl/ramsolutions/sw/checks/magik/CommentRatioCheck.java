package nl.ramsolutions.sw.checks.magik;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.checks.MagikCheck;
import nl.ramsolutions.sw.magik.metrics.FileLinesVisitor;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

/** Check that the comment density is above a threshold. */
@Rule(key = CommentRatioCheck.CHECK_KEY)
public class CommentRatioCheck extends MagikCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "CommentRatio";

  private static final int DEFAULT_MIN_COMMENT_PERCENTAGE = 25;
  private static final String MESSAGE = "The comment density is below the threshold (%d%%/%d%%).";

  /** Minimum comment percentage. */
  @RuleProperty(
      key = "min comment percentage",
      defaultValue = "" + DEFAULT_MIN_COMMENT_PERCENTAGE,
      description = "Minimum comment percentage",
      type = "INTEGER")
  @SuppressWarnings("checkstyle:VisibilityModifier")
  public int minCommentPercentage = DEFAULT_MIN_COMMENT_PERCENTAGE;

  @Override
  protected void walkPostMagik(final AstNode node) {
    // Mirror the SonarQube comment density metric: header comments are ignored, and the density
    // is comment_lines / (lines_of_code + comment_lines).
    final FileLinesVisitor visitor = new FileLinesVisitor(true);
    visitor.walkAst(node);

    final int linesOfCode = visitor.getLinesOfCode().size();
    final int linesOfComments = visitor.getLinesOfComments().size();
    if (linesOfCode == 0) {
      return;
    }

    final int commentPercentage = linesOfComments * 100 / (linesOfCode + linesOfComments);
    if (commentPercentage < this.minCommentPercentage) {
      final String message = MESSAGE.formatted(commentPercentage, this.minCommentPercentage);
      this.addFileIssue(message);
    }
  }
}
