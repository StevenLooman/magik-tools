package nl.ramsolutions.sw.magik.debugadapter.breakpoints;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

/** An exception breakpoint filter the client can select. */
public interface ExceptionFilter {

  /**
   * Get the ID the client sends when it selects this filter.
   *
   * @return ID.
   */
  String getId();

  /**
   * Get the label the client shows.
   *
   * @return Label.
   */
  String getLabel();

  /**
   * Get the Magik condition the agent evaluates when a condition is signalled. The breakpoint hits
   * when the condition of any selected filter holds.
   *
   * @return Magik expression.
   */
  String getCondition();

  /**
   * Decide whether the thread stops, after the condition held. The selected filters are asked in
   * order, the first {@code true} stops the thread, and if all answer {@code false} it resumes.
   *
   * @param context The hit.
   * @return True to stop the thread.
   * @throws IOException -
   * @throws InterruptedException -
   * @throws ExecutionException -
   */
  default boolean shouldStop(final FilterContext context)
      throws IOException, InterruptedException, ExecutionException {
    return true;
  }
}
