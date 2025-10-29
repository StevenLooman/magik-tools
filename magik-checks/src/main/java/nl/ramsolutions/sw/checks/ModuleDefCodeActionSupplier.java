package nl.ramsolutions.sw.checks;

import java.util.List;
import nl.ramsolutions.sw.OpenedFile;
import nl.ramsolutions.sw.magik.CodeAction;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.moduledef.ModuleDefFile;

/** {@link CodeAction} supplier for {@link ModuleDefFile}s. */
public abstract class ModuleDefCodeActionSupplier extends CodeActionSupplier {

  /**
   * Provide {@link CodeAction}s for {@link ModuleDefFile}s.
   *
   * @param moduleDefFile {@link ModuleDefFile} to provide fixes for.
   * @param range {@link Range} to provide fixes for.
   * @return List of {@link CodeAction}s to be applied.
   */
  public abstract List<CodeAction> provideModuleDefCodeActions(
      final ModuleDefFile moduleDefFile, final Range range);

  @Override
  public List<CodeAction> provideCodeActions(final OpenedFile file, final Range range) {
    if (!(file instanceof ModuleDefFile moduleDefFile)) {
      throw new IllegalArgumentException(
          "OpenedFile is not a ModuleDefFile: " + file.getClass().getName());
    }

    return this.provideModuleDefCodeActions(moduleDefFile, range);
  }
}
