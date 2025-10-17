package nl.ramsolutions.sw.moduledef.metrics;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.TokenType;
import com.sonar.sslr.api.Trivia;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import nl.ramsolutions.sw.ConfigurationLocator;
import nl.ramsolutions.sw.moduledef.ModuleDefVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Visitor that computes NCLOC_DATA_KEY and COMMENT_LINES_DATA_KEY metrics used by the DevCockpit.
 */
public class FileLinesVisitor extends ModuleDefVisitor {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationLocator.class);

  private final boolean ignoreHeaderComments;
  private boolean seenFirstToken;

  private final Set<Integer> linesOfDefinition = new HashSet<>();
  private final Set<Integer> linesOfComments = new HashSet<>();
  private final Set<Integer> executableLines = new HashSet<>();
  private final Set<Integer> nosonarLines = new HashSet<>();

  public FileLinesVisitor(final boolean ignoreHeaderComments) {
    this.ignoreHeaderComments = ignoreHeaderComments;
  }

  public Set<Integer> getLinesOfDefinition() {
    return Collections.unmodifiableSet(this.linesOfDefinition);
  }

  public Set<Integer> getLinesOfComments() {
    return Collections.unmodifiableSet(this.linesOfComments);
  }

  public Set<Integer> getExecutableLines() {
    return Collections.unmodifiableSet(this.executableLines);
  }

  public Set<Integer> getNosonarLines() {
    return Collections.unmodifiableSet(this.nosonarLines);
  }

  @Override
  protected void walkPreModuleDefinition(final AstNode node) {
    this.nosonarLines.clear();
    this.linesOfDefinition.clear();
    this.linesOfComments.clear();
    this.executableLines.clear();
    this.seenFirstToken = false;
  }

  @Override
  public void walkToken(final Token token) {
    final TokenType tokenType = token.getType();
    if (tokenType.equals(GenericTokenType.EOF)) {
      return;
    }

    // Process lines of code.
    final String[] tokenLines = token.getValue().split("\n", -1);
    for (int line = token.getLine(); line < token.getLine() + tokenLines.length; line++) {
      this.linesOfDefinition.add(line);
    }

    // Ignore file header comment.
    if (this.ignoreHeaderComments && !this.seenFirstToken) {
      this.seenFirstToken = true;
      return;
    }

    // Process comments.
    for (final Trivia trivia : token.getTrivia()) {
      if (trivia.isComment()) {
        this.visitComment(trivia);
      }
    }
  }

  private void visitComment(final Trivia trivia) {
    final String originalValue =
        FileLinesVisitor.getCommentContent(trivia.getToken().getOriginalValue());
    final String[] commentLines = originalValue.split("(\r)?\n|\r", -1);

    int line = trivia.getToken().getLine();
    for (final String commentLine : commentLines) {
      if (commentLine.contains("NOSONAR")) {
        this.linesOfComments.remove(line);
        this.nosonarLines.add(line);
      } else if (!commentLine.isBlank() && !this.nosonarLines.contains(line)) {
        this.linesOfComments.add(line);
      }

      line++;
    }
  }

  /**
   * Get contents of comment.
   *
   * @param comment Comment to get contents from.
   * @return Text of comment.
   */
  private static String getCommentContent(final String comment) {
    // Comments always starts with "#"
    return comment.substring(comment.indexOf('#'));
  }
}
