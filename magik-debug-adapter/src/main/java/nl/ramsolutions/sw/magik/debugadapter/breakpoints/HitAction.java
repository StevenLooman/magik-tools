package nl.ramsolutions.sw.magik.debugadapter.breakpoints;

import java.io.IOException;

/**
 * Handles a breakpoint hit. A stop of an exception filter is a hit of the condition breakpoint, so
 * the actions handle it too; the {@link HitContext} does not tell the two apart.
 */
public interface HitAction {

  /**
   * Handle a hit.
   *
   * @param context The hit.
   * @return True if the hit is handled, which ends the asking of the actions; false to leave it to
   *     the next action.
   * @throws IOException -
   * @throws InterruptedException -
   */
  boolean handle(HitContext context) throws IOException, InterruptedException;

  /**
   * Test if this action makes the adapter support logpoints.
   *
   * @return True if the adapter should advertise log point support.
   */
  default boolean supportsLogpoints() {
    return false;
  }
}
