package nl.ramsolutions.sw.checks;

import java.util.List;
import nl.ramsolutions.sw.magik.CodeAction;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Range;

/** Base class to provide automatic fixes for {@link MagikTypedCheck} checks. */
public abstract class MagikTypedCheckFixer {

  /**
   * Provide automatic fixes for violations detected by the sibling check.
   *
   * @param file MagikTypedFile to provide fixes for.
   * @param range Range to provide fixes for.
   * @return List of {@link CodeAction}s to be applied.
   */
  public abstract List<CodeAction> provideCodeActions(final MagikTypedFile file, final Range range);
}
