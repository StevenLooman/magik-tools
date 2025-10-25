package nl.ramsolutions.sw.checks;

import java.util.List;
import nl.ramsolutions.sw.OpenedFile;
import nl.ramsolutions.sw.magik.CodeAction;
import nl.ramsolutions.sw.magik.Range;

/** Base class to provide automatic fixes for {@link OpenedFile}s, based on {@link Check}. */
public abstract class CodeActionSupplier {

  /**
   * Provide code {@link CodeAction} for {@link OpenedFile}.
   *
   * @param file {@link OpenedFile} to provide {@link CodeAction}s for.
   * @param range {@link Range} to provide {@link CodeAction}s for.
   * @return List of {@link CodeAction}s to be applied.
   */
  public abstract List<CodeAction> provideCodeActions(final OpenedFile file, final Range range);
}
