package nl.ramsolutions.sw.checks;

import java.util.List;
import nl.ramsolutions.sw.OpenedFile;
import nl.ramsolutions.sw.magik.CodeAction;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Range;

/** {@link CodeAction} supplier for {@link MagikTypedFile}s. */
public abstract class MagikTypedCodeActionSupplier extends CodeActionSupplier {

  /**
   * Provide {@link CodeAction}s for violations detected by the sibling check.
   *
   * @return List of {@link CodeAction}s to be applied.
   */
  public abstract List<CodeAction> provideMagikTypedCodeActions(
      final MagikTypedFile magikFile, final Range range);

  @Override
  public List<CodeAction> provideCodeActions(final OpenedFile file, final Range range) {
    if (!(file instanceof MagikTypedFile magikFile)) {
      throw new IllegalArgumentException(
          "OpenedFile is not a MagikTypedFile: " + file.getClass().getName());
    }

    return this.provideMagikTypedCodeActions(magikFile, range);
  }
}
