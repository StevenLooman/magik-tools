package nl.ramsolutions.sw.magik.debugadapter;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import nl.ramsolutions.sw.magik.debugadapter.slap.events.BreakpointEvent;
import nl.ramsolutions.sw.magik.debugadapter.slap.events.StepCompletedEvent;

/** Steps threads, and takes part in the events and requests which end a step. */
interface Stepper {

  /**
   * Step over the current line of a suspended thread.
   *
   * @param threadId Thread ID.
   * @throws IOException -
   * @throws InterruptedException -
   * @throws ExecutionException -
   */
  void stepOver(long threadId) throws IOException, InterruptedException, ExecutionException;

  /**
   * Step into the current line of a suspended thread.
   *
   * @param threadId Thread ID.
   * @throws IOException -
   * @throws InterruptedException -
   * @throws ExecutionException -
   */
  void stepIn(long threadId) throws IOException, InterruptedException, ExecutionException;

  /**
   * Step out of the current method of a suspended thread.
   *
   * @param threadId Thread ID.
   * @throws IOException -
   * @throws InterruptedException -
   * @throws ExecutionException -
   */
  void stepOut(long threadId) throws IOException, InterruptedException, ExecutionException;

  /**
   * Handle a {@link StepCompletedEvent}.
   *
   * @param event Event.
   */
  void handleStepCompletedEvent(StepCompletedEvent event);

  /**
   * Handle a {@link BreakpointEvent}, and hand it to the breakpoint manager.
   *
   * @param event Event.
   * @param clearVariables Clears the variables of the debug client.
   */
  void handleBreakpointEvent(BreakpointEvent event, Runnable clearVariables);

  /**
   * End the step of a thread which is about to be paused.
   *
   * @param threadId Thread ID.
   * @throws IOException -
   * @throws InterruptedException -
   */
  default void endStepForPause(final long threadId) throws IOException, InterruptedException {
    // Nothing to end.
  }

  /**
   * End the step of a thread which is about to be continued.
   *
   * @param threadId Thread ID.
   * @throws IOException -
   * @throws InterruptedException -
   */
  default void endStepForContinue(final long threadId) throws IOException, InterruptedException {
    // Nothing to end.
  }

  /** Cancel all steps, as the connection is closing. */
  default void cancelSteps() {
    // Nothing to cancel.
  }
}
