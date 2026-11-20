package nl.ramsolutions.sw.magik.debugadapter.slap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.concurrent.ExecutionException;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.ErrorResponse;
import org.junit.jupiter.api.Test;

/** Tests for {@link SlapErrorException}. */
class SlapErrorExceptionTest {

  private ExecutionException createErrorFailure(final ErrorMessage errorMessage) {
    final ErrorResponse errorResponse = new ErrorResponse(RequestType.EVALUATE, errorMessage);
    final SlapErrorException cause = new SlapErrorException(errorResponse);
    return new ExecutionException(cause);
  }

  @Test
  void testMethodNotFoundIsNotRethrown() {
    final ExecutionException failure = this.createErrorFailure(ErrorMessage.METHOD_NOT_FOUND);

    final Throwable thrown =
        catchThrowable(() -> SlapErrorException.rethrowUnlessMethodNotFound(failure));

    assertThat(thrown).isNull();
  }

  @Test
  void testOtherErrorIsRethrown() {
    final ExecutionException failure = this.createErrorFailure(ErrorMessage.EVALUATION_FAILED);

    final Throwable thrown =
        catchThrowable(() -> SlapErrorException.rethrowUnlessMethodNotFound(failure));

    assertThat(thrown).isSameAs(failure);
  }

  @Test
  void testFailureWhichIsNoErrorAnswerIsRethrown() {
    final SlapException cause = new SlapException("Connection to debug agent closed");
    final ExecutionException failure = new ExecutionException(cause);

    final Throwable thrown =
        catchThrowable(() -> SlapErrorException.rethrowUnlessMethodNotFound(failure));

    assertThat(thrown).isSameAs(failure);
  }
}
