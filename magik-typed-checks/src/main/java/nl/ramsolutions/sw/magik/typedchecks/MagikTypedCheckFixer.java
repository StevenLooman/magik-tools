package nl.ramsolutions.sw.magik.typedchecks;

import java.util.List;
import nl.ramsolutions.sw.magik.CodeAction;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Range;

/** Base class to provide automatic fixes for Magik checks. */
public abstract class MagikTypedCheckFixer {

  /**
   * Provide automatic fixes for violations detected by the sibling check.
   *
   * @return
   */
  public abstract List<CodeAction> provideCodeActions(MagikTypedFile magikFile, Range range);
}
