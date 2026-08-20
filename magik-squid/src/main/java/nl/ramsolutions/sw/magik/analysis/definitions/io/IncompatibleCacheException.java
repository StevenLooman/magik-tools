package nl.ramsolutions.sw.magik.analysis.definitions.io;

import java.io.IOException;
import java.io.Serial;

/**
 * Signals a types cache written by a different schema version. A cache owner (the language server's
 * workspace folder) should delete it and re-index; a one-shot tool should let it propagate.
 */
public class IncompatibleCacheException extends IOException {

  @Serial private static final long serialVersionUID = 1L;

  /**
   * Constructor.
   *
   * @param message What was found versus what was expected.
   */
  public IncompatibleCacheException(final String message) {
    super(message);
  }
}
