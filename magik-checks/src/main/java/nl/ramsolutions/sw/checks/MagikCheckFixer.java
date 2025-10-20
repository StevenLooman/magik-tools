package nl.ramsolutions.sw.checks;

import java.util.List;
import nl.ramsolutions.sw.magik.CodeAction;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.Range;

/** Base class to provide automatic fixes for {@link MagikCheck}s. */
public abstract class MagikCheckFixer {

  /**
   * Provide automatic fixes for violations detected by the sibling check.
   *
   * @param file MagikFile to provide fixes for.
   * @param range Range to provide fixes for.
   * @return List of {@link CodeAction}s to be applied.
   */
  public abstract List<CodeAction> provideCodeActions(final MagikFile file, final Range range);
}
