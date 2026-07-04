package nl.ramsolutions.sw.checks.magik;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.checks.MagikCheck;
import org.sonar.check.Rule;

/** Check if file starts with a {@code #% text_encoding = ...} comment. */
@Rule(key = FileMustStartWithTextEncodingCheck.CHECK_KEY)
public class FileMustStartWithTextEncodingCheck extends MagikCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "FileMustStartWithTextEncoding";

  private static final String MESSAGE =
      "File must start with a text encoding comment (#% text_encoding = ...).";
  private static final String ENCODING_LINE = "#% text_encoding =";

  @Override
  protected void walkPreMagik(final AstNode node) {
    if (!this.startsWithTextEncoding()) {
      this.addFileIssue(MESSAGE);
    }
  }

  private boolean startsWithTextEncoding() {
    final String[] sourceLines = this.getMagikFile().getSourceLines();
    if (sourceLines == null || sourceLines.length == 0) {
      return false;
    }

    // The encoding comment is only honoured on the first line (see FileCharsetDeterminer).
    return sourceLines[0].startsWith(ENCODING_LINE);
  }
}
