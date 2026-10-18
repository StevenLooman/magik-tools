package nl.ramsolutions.sw.checks;

import java.util.List;
import nl.ramsolutions.sw.OpenedFile;
import nl.ramsolutions.sw.magik.CodeAction;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.translation.TranslationFile;

/** {@link CodeAction} supplier for translation (.trn) files. */
public abstract class TranslationCodeActionSupplier extends CodeActionSupplier {

  /**
   * Provide {@link CodeAction}s for {@link TranslationFile}s.
   *
   * @param translationFile {@link TranslationFile} to provide fixes for.
   * @param range {@link Range} to provide fixes for.
   * @return List of {@link CodeAction}s to be applied.
   */
  public abstract List<CodeAction> provideTranslationCodeActions(
      final TranslationFile translationFile, final Range range);

  @Override
  public List<CodeAction> provideCodeActions(final OpenedFile file, final Range range) {
    if (!(file instanceof TranslationFile translationFile)) {
      throw new IllegalArgumentException(
          "OpenedFile is not a TranslationFile: " + file.getClass().getName());
    }

    return this.provideTranslationCodeActions(translationFile, range);
  }
}
