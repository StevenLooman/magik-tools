package nl.ramsolutions.sw.magik.metrics;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.TokenType;
import com.sonar.sslr.api.Trivia;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import nl.ramsolutions.sw.magik.MagikVisitor;
import nl.ramsolutions.sw.magik.api.MagikKeyword;
import nl.ramsolutions.sw.magik.api.MagikPunctuator;

/**
 * Visitor that computes NCLOC_DATA_KEY and COMMENT_LINES_DATA_KEY metrics used by the DevCockpit.
 */
public class FileLinesVisitor extends MagikVisitor {

  private static final Set<AstNodeType> NON_EXECUTABLE_TOKENS =
      Set.of(
          MagikKeyword.HANDLING,
          MagikKeyword.BLOCK,
          MagikKeyword.TRY,
          MagikKeyword.WHEN,
          MagikKeyword.ENDTRY,
          MagikKeyword.PROTECT,
          MagikKeyword.PROTECTION,
          MagikKeyword.ENDPROTECT,
          MagikKeyword.LOCK,
          MagikKeyword.ENDLOCK,
          MagikKeyword.CATCH,
          MagikKeyword.ENDCATCH,
          MagikKeyword.PROC,
          MagikKeyword.ENDPROC,
          MagikPunctuator.DOLLAR);

  private final boolean ignoreHeaderComments;
  private boolean seenFirstToken;

  private final Set<Integer> linesOfCode = new HashSet<>();
  private final Set<Integer> linesOfComments = new HashSet<>();
  private final Set<Integer> executableLines = new HashSet<>();
  private final Set<Integer> nosonarLines = new HashSet<>();

  public FileLinesVisitor(final boolean ignoreHeaderComments) {
    this.ignoreHeaderComments = ignoreHeaderComments;
  }

  public Set<Integer> getLinesOfCode() {
    return Collections.unmodifiableSet(this.linesOfCode);
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
  protected void walkPreMagik(final AstNode node) {
    this.nosonarLines.clear();
    this.linesOfCode.clear();
    this.linesOfComments.clear();
    this.executableLines.clear();
    this.seenFirstToken = false;
  }

  @Override
  public void walkToken(final Token token) {
    // process lines of code
    final String[] tokenLines = token.getValue().split("\n", -1);
    for (int line = token.getLine(); line < token.getLine() + tokenLines.length; line++) {
      this.linesOfCode.add(line);
    }

    // ignore file header comment
    if (this.ignoreHeaderComments && !this.seenFirstToken) {
      this.seenFirstToken = true;
      return;
    }

    // process comment
    for (final Trivia trivia : token.getTrivia()) {
      if (trivia.isComment()) {
        visitComment(trivia);
      }
    }
  }

  @Override
  protected void walkPreStatement(final AstNode node) {
    this.addIfExecutableLine(node);
  }

  @Override
  protected void walkPreExpression(final AstNode node) {
    this.addIfExecutableLine(node);
  }

  private void addIfExecutableLine(final AstNode node) {
    // process any executable nodes/tokens
    final TokenType tokenType = node.getToken().getType();
    if (!NON_EXECUTABLE_TOKENS.contains(tokenType)) {
      this.executableLines.add(node.getTokenLine());
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
