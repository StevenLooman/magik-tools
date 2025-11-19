package nl.ramsolutions.sw.checks;

import java.util.List;
import nl.ramsolutions.sw.OpenedFile;
import nl.ramsolutions.sw.loadlist.LoadListFile;
import nl.ramsolutions.sw.magik.CodeAction;
import nl.ramsolutions.sw.magik.Range;

/** {@link CodeAction} supplier for {@link LoadListFile}s. */
public abstract class LoadListCodeActionSupplier extends CodeActionSupplier {

  /**
   * Provide {@link CodeAction}s for {@link LoadListFile}s.
   *
   * @param loadListFile {@link LoadListFile} to provide fixes for.
   * @param range {@link Range} to provide fixes for.
   * @return List of {@link CodeAction}s to be applied.
   */
  public abstract List<CodeAction> provideLoadListCodeActions(
      final LoadListFile loadListFile, final Range range);

  @Override
  public List<CodeAction> provideCodeActions(final OpenedFile file, final Range range) {
    if (!(file instanceof LoadListFile loadListFile)) {
      throw new IllegalArgumentException(
          "OpenedFile is not a LoadListFile: " + file.getClass().getName());
    }

    return this.provideLoadListCodeActions(loadListFile, range);
  }
}
