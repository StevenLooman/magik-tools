package nl.ramsolutions.sw.magik.debugadapter.slap;

import java.io.Serial;
import java.util.concurrent.ExecutionException;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.ErrorResponse;

/** Slap error exception. */
public class SlapErrorException extends SlapException {

  /** Serial version UID. */
  @Serial private static final long serialVersionUID = 4756599758479724128L;

  private final ErrorResponse error;

  /**
   * Constructor.
   *
   * @param errorResponse Error to encapsulate.
   */
  public SlapErrorException(final ErrorResponse errorResponse) {
    super(
        "Caught error: %s, on request: %s%n"
            .formatted(errorResponse.getErrorMessage(), errorResponse.getRequestType()));
    this.error = errorResponse;
  }

  /**
   * Rethrow a failed request, unless the agent answered that the method is not found.
   *
   * @param exception Failure of the request.
   * @throws ExecutionException The given exception, if it is not a METHOD_NOT_FOUND answer, such as
   *     another error answer or a closed connection.
   */
  public static void rethrowUnlessMethodNotFound(final ExecutionException exception)
      throws ExecutionException {
    final Throwable cause = exception.getCause();
    if (!(cause instanceof SlapErrorException slapErrorException)) {
      throw exception;
    }

    final ErrorResponse error = slapErrorException.getError();
    final ErrorMessage errorMessage = error.getErrorMessage();
    if (errorMessage != ErrorMessage.METHOD_NOT_FOUND) {
      throw exception;
    }
  }

  public ErrorResponse getError() {
    return this.error;
  }

  public RequestType getRequestType() {
    return this.error.getRequestType();
  }
}
