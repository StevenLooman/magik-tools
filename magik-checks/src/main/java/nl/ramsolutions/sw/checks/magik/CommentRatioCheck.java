package nl.ramsolutions.sw.checks.magik;

import com.sonar.sslr.api.AstNode;
import java.util.HashSet;
import java.util.Set;
import nl.ramsolutions.sw.checks.MagikCheck;
import nl.ramsolutions.sw.magik.metrics.FileLinesVisitor;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

/** Check that the comment density is above a threshold. */
@Rule(key = CommentRatioCheck.CHECK_KEY)
public class CommentRatioCheck extends MagikCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "CommentRatio";

  private static final int DEFAULT_MINIMUM_COMMENT_PERCENTAGE = 25;
  private static final String MESSAGE = "The comment density is below the threshold (%d%%/%d%%).";

  /** Minimum comment percentage. */
  @RuleProperty(
      key = "minimum comment percentage",
      defaultValue = "" + DEFAULT_MINIMUM_COMMENT_PERCENTAGE,
      description = "Minimum comment percentage",
      type = "INTEGER")
  @SuppressWarnings("checkstyle:VisibilityModifier")
  public int minimumCommentPercentage = DEFAULT_MINIMUM_COMMENT_PERCENTAGE;

  @Override
  protected void walkPostMagik(final AstNode node) {
    final FileLinesVisitor visitor = new FileLinesVisitor(false);
    visitor.walkAst(node);

    final int linesOfCode = visitor.getLinesOfCode().size();
    final int linesOfComments = visitor.getLinesOfComments().size();
    if (linesOfCode == 0) {
      return;
    }

    final Set<Integer> allLines = new HashSet<>();
    allLines.addAll(visitor.getLinesOfCode());
    allLines.addAll(visitor.getLinesOfComments());
    final int totalLines = allLines.size();
    final int commentPercentage = linesOfComments * 100 / totalLines;
    if (commentPercentage < this.minimumCommentPercentage) {
      final String message = MESSAGE.formatted(commentPercentage, this.minimumCommentPercentage);
      this.addFileIssue(message);
    }
  }
}
