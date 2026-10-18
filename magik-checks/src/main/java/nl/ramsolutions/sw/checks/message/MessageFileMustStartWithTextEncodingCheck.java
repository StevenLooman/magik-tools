package nl.ramsolutions.sw.checks.message;

import nl.ramsolutions.sw.FileCharsetDeterminer;
import nl.ramsolutions.sw.checks.MessageCheck;
import org.sonar.check.Rule;

/** Check if a message (.msg) file starts with a {@code #% text_encoding = ...} comment. */
@Rule(key = MessageFileMustStartWithTextEncodingCheck.CHECK_KEY)
public class MessageFileMustStartWithTextEncodingCheck extends MessageCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "MessageFileMustStartWithTextEncoding";

  private static final String MESSAGE =
      "File must start with a text encoding comment (#% text_encoding = ...).";

  @Override
  protected void scanFile() {
    // The encoding comment is only honoured on the first line (see FileCharsetDeterminer).
    final String[] sourceLines = this.getMessageFile().getSourceLines();
    if (sourceLines.length == 0 || !FileCharsetDeterminer.hasEncodingDeclaration(sourceLines[0])) {
      this.addFileIssue(MESSAGE);
    }
  }
}
