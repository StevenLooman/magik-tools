package nl.ramsolutions.sw.magik.debugadapter.evaluate;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/** Answers evaluate requests. */
public interface EvaluateModule {

  /**
   * Try to answer an evaluate request.
   *
   * @param context Request.
   * @return Result of the evaluation, or empty if this module does not handle the request.
   * @throws IOException -
   * @throws InterruptedException -
   * @throws ExecutionException -
   */
  Optional<String> tryEvaluate(EvaluateContext context)
      throws IOException, InterruptedException, ExecutionException;
}
