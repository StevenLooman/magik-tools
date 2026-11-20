package nl.ramsolutions.sw.magik.debugadapter.slap;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Futures of the requests which wait for their reply, replies come in order of request. */
class PendingRequests {

  /** Request future. */
  private static final class RequestFuture {
    private final RequestType requestType;
    private final CompletableFuture<ISlapResponse> future;

    RequestFuture(final RequestType requestType, final CompletableFuture<ISlapResponse> future) {
      this.requestType = requestType;
      this.future = future;
    }

    @Override
    public String toString() {
      return "%s@%s(%s)"
          .formatted(
              this.getClass().getName(), Integer.toHexString(this.hashCode()), this.requestType);
    }
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(PendingRequests.class);

  private final List<RequestFuture> requestFutures = new ArrayList<>();

  /**
   * Add a request which waits for its reply.
   *
   * @param requestType Request type.
   * @return Future of the reply.
   */
  CompletableFuture<ISlapResponse> add(final RequestType requestType) {
    final CompletableFuture<ISlapResponse> future = new CompletableFuture<>();
    final RequestFuture requestFuture = new RequestFuture(requestType, future);

    synchronized (this.requestFutures) {
      LOGGER.debug(
          "Thread: {}, added request future: {}", Thread.currentThread().getName(), requestFuture);
      this.requestFutures.add(requestFuture);
    }

    return future;
  }

  /**
   * Complete the oldest request of a request type.
   *
   * @param requestType Request type.
   * @param response Reply.
   */
  void complete(final RequestType requestType, final ISlapResponse response) {
    LOGGER.debug(
        "Thread: {}, Request type: {}, response: {}",
        Thread.currentThread().getName(),
        requestType,
        response);

    final Optional<RequestFuture> pending = this.removeFirst(requestType);
    pending.ifPresent(requestFuture -> requestFuture.future.complete(response));
  }

  /**
   * Fail the oldest request of a request type with an error of the agent.
   *
   * @param requestType Request type.
   * @param errorResponse Error.
   */
  void completeError(final RequestType requestType, final ErrorResponse errorResponse) {
    LOGGER.debug(
        "Thread: {}, Request type: {}, error response: {}",
        Thread.currentThread().getName(),
        requestType,
        errorResponse);

    final SlapErrorException exception = new SlapErrorException(errorResponse);
    this.fail(requestType, exception);
  }

  /**
   * Fail the oldest request of a request type.
   *
   * @param requestType Request type.
   * @param exception Why the request failed.
   */
  void fail(final RequestType requestType, final SlapException exception) {
    final Optional<RequestFuture> pending = this.removeFirst(requestType);
    pending.ifPresent(requestFuture -> requestFuture.future.completeExceptionally(exception));
  }

  /**
   * Fail all requests.
   *
   * @param exception Why the requests failed.
   */
  void failAll(final SlapException exception) {
    final List<RequestFuture> pendingFutures;
    synchronized (this.requestFutures) {
      pendingFutures = new ArrayList<>(this.requestFutures);
      this.requestFutures.clear();
    }

    for (final RequestFuture requestFuture : pendingFutures) {
      requestFuture.future.completeExceptionally(exception);
    }
  }

  private Optional<RequestFuture> removeFirst(final RequestType requestType) {
    synchronized (this.requestFutures) {
      final Optional<RequestFuture> first =
          this.requestFutures.stream()
              .filter(requestFuture -> requestFuture.requestType == requestType)
              .findFirst();
      if (first.isEmpty()) {
        LOGGER.warn(
            "Thread: {}, Trying to handle request, but not available, request type: {}",
            Thread.currentThread().getName(),
            requestType);
        return first;
      }

      this.requestFutures.remove(first.get());
      return first;
    }
  }
}
