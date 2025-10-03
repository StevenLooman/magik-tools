package nl.ramsolutions.sw.checks;

import java.util.List;
import nl.ramsolutions.sw.OpenedFile;
import nl.ramsolutions.sw.magik.CodeAction;
import nl.ramsolutions.sw.magik.Range;

/** Base class to provide automatic fixes for {@link Check}s. */
public abstract class CheckFixer {

  /**
   * Provide automatic fixes for violations detected by the sibling check.
   *
   * @param file OpenedFile to provide fixes for.
   * @param range Range to provide fixes for.
   * @return List of {@link CodeAction}s to be applied.
   */
  public abstract List<CodeAction> provideCodeActions(final OpenedFile file, final Range range);
}
