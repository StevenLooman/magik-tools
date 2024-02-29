package nl.ramsolutions.sw.magik.debugadapter.slap;

import nl.ramsolutions.sw.magik.debugadapter.slap.responses.ErrorResponse;

/** Slap error exception. */
public class SlapErrorException extends SlapException {

  /** Serial version UID. */
  private static final long serialVersionUID = 4756599758479724128L;

  private final ErrorResponse error;

  /**
   * Constructor.
   *
   * @param errorResponse Error to encapsulate.
   */
  public SlapErrorException(final ErrorResponse errorResponse) {
    super(
        String.format(
            "Caught error: %s, on request: %s%n",
            errorResponse.getErrorMessage(), errorResponse.getRequestType()));
    this.error = errorResponse;
  }

  public ErrorResponse getError() {
    return this.error;
  }

  public RequestType getRequestType() {
    return this.error.getRequestType();
  }
}
