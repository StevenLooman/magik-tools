package nl.ramsolutions.sw.magik.checks.checks;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import org.sonar.check.Rule;

/** Check if file starts with a _package-statement. */
@Rule(key = FileWithoutPackageStatementCheck.CHECK_KEY)
public class FileWithoutPackageStatementCheck extends MagikCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "FileWithoutPackageStatement";

  private static final String MESSAGE = "File has no package-statement.";

  @Override
  protected void walkPostMagik(final AstNode node) {
    if (!hasPackageStatement(node)) {
      this.addFileIssue(MESSAGE);
    }
  }

  private boolean hasPackageStatement(final AstNode node) {
    final List<AstNode> children = node.getChildren();
    if (children.isEmpty()) {
      return false;
    }

    return children.get(0).is(MagikGrammar.PACKAGE_SPECIFICATION);
  }
}
