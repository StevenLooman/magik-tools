package nl.ramsolutions.sw.magik.debugadapter.slap;

import java.io.Serial;

/** Slap exception. */
public class SlapException extends Exception {

  @Serial private static final long serialVersionUID = 8892401574886655251L;

  /**
   * Constructor.
   *
   * @param message Message.
   */
  public SlapException(final String message) {
    super(message);
  }

  /**
   * Constructor.
   *
   * @param cause Cause.
   */
  public SlapException(final Throwable cause) {
    super(cause);
  }
}
