package nl.ramsolutions.sw.checks;

import java.util.List;
import nl.ramsolutions.sw.OpenedFile;
import nl.ramsolutions.sw.magik.CodeAction;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.Range;

/** {@link CodeAction} supplier for {@link MagikFile}s. */
public abstract class MagikCodeActionSupplier extends CodeActionSupplier {

  /**
   * Provide {@link CodeAction}s for {@link MagikFile}s.
   *
   * @param magikFile {@link MagikFile} to provide fixes for.
   * @param range {@link Range} to provide fixes for.
   * @return List of {@link CodeAction}s to be applied.
   */
  public abstract List<CodeAction> provideMagikCodeActions(
      final MagikFile magikFile, final Range range);

  @Override
  public List<CodeAction> provideCodeActions(final OpenedFile file, final Range range) {
    if (!(file instanceof MagikFile magikFile)) {
      throw new IllegalArgumentException(
          "OpenedFile is not a MagikFile: " + file.getClass().getName());
    }

    return this.provideMagikCodeActions(magikFile, range);
  }
}
