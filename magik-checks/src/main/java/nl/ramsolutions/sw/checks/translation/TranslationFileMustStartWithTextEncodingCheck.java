package nl.ramsolutions.sw.checks.translation;

import nl.ramsolutions.sw.FileCharsetDeterminer;
import nl.ramsolutions.sw.checks.TranslationCheck;
import org.sonar.check.Rule;

/** Check if a translation (.trn) file starts with a {@code #% text_encoding = ...} comment. */
@Rule(key = TranslationFileMustStartWithTextEncodingCheck.CHECK_KEY)
public class TranslationFileMustStartWithTextEncodingCheck extends TranslationCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "TranslationFileMustStartWithTextEncoding";

  private static final String MESSAGE =
      "File must start with a text encoding comment (#% text_encoding = ...).";

  @Override
  protected void scanFile() {
    // The encoding comment is only honoured on the first line (see FileCharsetDeterminer).
    final String[] sourceLines = this.getTranslationFile().getSourceLines();
    if (sourceLines.length == 0 || !FileCharsetDeterminer.hasEncodingDeclaration(sourceLines[0])) {
      this.addFileIssue(MESSAGE);
    }
  }
}
