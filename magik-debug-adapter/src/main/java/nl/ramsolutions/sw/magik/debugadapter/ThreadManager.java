package nl.ramsolutions.sw.magik.debugadapter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import nl.ramsolutions.sw.magik.debugadapter.breakpoints.BreakpointManager;
import nl.ramsolutions.sw.magik.debugadapter.slap.ErrorMessage;
import nl.ramsolutions.sw.magik.debugadapter.slap.ISlapProtocol;
import nl.ramsolutions.sw.magik.debugadapter.slap.ISlapResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.SlapErrorException;
import nl.ramsolutions.sw.magik.debugadapter.slap.events.BreakpointEvent;
import nl.ramsolutions.sw.magik.debugadapter.slap.events.StepCompletedEvent;
import nl.ramsolutions.sw.magik.debugadapter.slap.events.ThreadEndedEvent;
import nl.ramsolutions.sw.magik.debugadapter.slap.events.ThreadStartedEvent;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.ThreadInfoResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.ThreadListResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.ThreadStackResponse;
import org.eclipse.lsp4j.debug.StackFrame;
import org.eclipse.lsp4j.debug.Thread;
import org.eclipse.lsp4j.debug.ThreadEventArguments;
import org.eclipse.lsp4j.debug.ThreadEventArgumentsReason;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Thread manager. */
class ThreadManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(ThreadManager.class);

  private final ISlapProtocol slapProtocol;
  private final IDebugProtocolClient debugClient;
  private final FrameSourceResolver frameSourceResolver;
  private final Stepper stepper;

  /**
   * Constructor.
   *
   * @param slapProtocol SLapProtocol.
   * @param debugClient LSP4j DebugClient.
   * @param frameSourceResolver Resolves the source file of a stack frame.
   * @param breakpointManager Breakpoint manager.
   */
  ThreadManager(
      final ISlapProtocol slapProtocol,
      final IDebugProtocolClient debugClient,
      final FrameSourceResolver frameSourceResolver,
      final BreakpointManager breakpointManager) {
    this.slapProtocol = slapProtocol;
    this.debugClient = debugClient;
    this.frameSourceResolver = frameSourceResolver;
    this.stepper =
        this.createStepper(slapProtocol, debugClient, breakpointManager, this.frameSourceResolver);
  }

  /**
   * Create the stepper which steps the threads.
   *
   * @param slapProtocol SLAP protocol.
   * @param debugClient LSP4j DebugClient.
   * @param breakpointManager Breakpoint manager.
   * @param frameSourceResolver Resolver for the source of a frame.
   * @return Stepper.
   */
  protected Stepper createStepper(
      final ISlapProtocol slapProtocol,
      final IDebugProtocolClient debugClient,
      final BreakpointManager breakpointManager,
      final FrameSourceResolver frameSourceResolver) {
    return new AgentStepper(slapProtocol, debugClient, breakpointManager);
  }

  /**
   * Get the Thread currently active.
   *
   * @return {@link Thread}s
   * @throws IOException -
   * @throws InterruptedException -
   * @throws ExecutionException -
   */
  List<Thread> threads() throws IOException, InterruptedException, ExecutionException {
    // Get all thread IDs.
    final CompletableFuture<ISlapResponse> futureThreadList = this.slapProtocol.getThreadList();
    final ThreadListResponse threadList = (ThreadListResponse) futureThreadList.get();

    // Get all thread info for each received thread ID.
    final List<Thread> threads = new ArrayList<>();
    for (long threadId : threadList.getThreadIds()) {
      final CompletableFuture<ISlapResponse> threadInfoFuture =
          this.slapProtocol.getThreadInfo(threadId);
      try {
        final ThreadInfoResponse threadInfo = (ThreadInfoResponse) threadInfoFuture.get();
        LOGGER.trace("Got thread, id: {}, thread info: {}", threadId, threadInfo);

        final Thread thread = Lsp4jConversion.toLsp4j(threadId, threadInfo);
        threads.add(thread);
      } catch (final ExecutionException exception) {
        LOGGER.trace(
            "Exception while getting thread, id: {}, exception: {}",
            threadId,
            exception.getMessage());

        // If not ErrorMessage.UNKNOWN_ERROR, then re-throw.
        if (exception.getCause() instanceof SlapErrorException slapErrorException
            && slapErrorException.getError().getErrorMessage() != ErrorMessage.UNKNOWN_ERROR) {
          throw exception;
        }
      }
    }

    return threads;
  }

  /**
   * Get a stack trace for a given thread.
   *
   * @param threadId Thread ID.
   * @return StackFrames for thread.
   * @throws IOException -
   * @throws InterruptedException -
   * @throws ExecutionException -
   */
  List<StackFrame> stackTrace(final long threadId)
      throws IOException, InterruptedException, ExecutionException {
    final CompletableFuture<ISlapResponse> threadStackFuture =
        this.slapProtocol.getThreadStack(threadId);
    final ThreadStackResponse threadStack = (ThreadStackResponse) threadStackFuture.get();

    // Do conversion here due to filtering on language + getting source,
    // instead of Lsp4jConversion.
    final List<StackFrame> stackFrames = new ArrayList<>();
    for (final ThreadStackResponse.StackElement stackElement : threadStack.getStackFrames()) {
      LOGGER.trace(
          "Stack element, level: {}, language: {}, name: '{}', offset: {}",
          stackElement.getLevel(),
          stackElement.getLanguage(),
          stackElement.getName(),
          stackElement.getOffset());

      // Don't mess with non-Magik stack frames.
      final String language = stackElement.getLanguage();
      if (!language.equals(FrameSourceResolver.LANGUAGE_MAGIK)) {
        continue;
      }

      // This sets the frameId to the given stack frames.
      final Path path = this.frameSourceResolver.determinePath(threadId, stackElement);
      final StackFrame stackFrame = Lsp4jConversion.toLsp4j(threadId, stackElement, path);
      stackFrames.add(stackFrame);
    }

    return stackFrames;
  }

  /**
   * Pause a thread. A thread which is already suspended counts as paused.
   *
   * @param threadId Thread ID.
   * @throws IOException -
   * @throws InterruptedException -
   * @throws ExecutionException -
   */
  void pause(final long threadId) throws IOException, InterruptedException, ExecutionException {
    this.stepper.endStepForPause(threadId);
    try {
      this.slapProtocol.suspendThread(threadId).get();
    } catch (final ExecutionException exception) {
      // The agent answers an already suspended thread with an error which holds the thread id.
      if (!this.isSuspended(threadId)) {
        throw exception;
      }
    }
  }

  private boolean isSuspended(final long threadId) throws IOException, InterruptedException {
    try {
      final CompletableFuture<ISlapResponse> threadInfoFuture =
          this.slapProtocol.getThreadInfo(threadId);
      final ThreadInfoResponse threadInfo = (ThreadInfoResponse) threadInfoFuture.get();
      final Set<ThreadInfoResponse.ThreadFlag> threadFlags = threadInfo.getThreadFlags();
      return threadFlags.contains(ThreadInfoResponse.ThreadFlag.SUSPENDED);
    } catch (final ExecutionException exception) {
      LOGGER.trace("Could not get thread info, id: {}", threadId, exception);
      return false;
    }
  }

  /**
   * Continue a thread.
   *
   * @param threadId Thread ID.
   * @throws IOException -
   * @throws InterruptedException -
   * @throws ExecutionException -
   */
  @SuppressWarnings("java:S100")
  void continue_(final long threadId)
      throws IOException, InterruptedException, ExecutionException { // NOSONAR
    this.stepper.endStepForContinue(threadId);
    final CompletableFuture<ISlapResponse> resumeFuture = this.slapProtocol.resumeThread(threadId);
    resumeFuture.get();
  }

  /**
   * Do a Step Next for a thread.
   *
   * @param threadId Thread ID.
   * @throws IOException -
   * @throws InterruptedException -
   * @throws ExecutionException -
   */
  void stepNext(final long threadId) throws IOException, InterruptedException, ExecutionException {
    this.stepper.stepOver(threadId);
  }

  /**
   * Do a Step In for a thread.
   *
   * @param threadId Thread ID.
   * @throws IOException -
   * @throws InterruptedException -
   * @throws ExecutionException -
   */
  void stepIn(final long threadId) throws IOException, InterruptedException, ExecutionException {
    this.stepper.stepIn(threadId);
  }

  /**
   * Do a Step Out for a thread.
   *
   * @param threadId Thread ID.
   * @throws IOException -
   * @throws InterruptedException -
   * @throws ExecutionException -
   */
  void stepOut(final long threadId) throws IOException, InterruptedException, ExecutionException {
    this.stepper.stepOut(threadId);
  }

  /**
   * Handle a {@link ThreadStartedEvent}.
   *
   * @param event Event.
   */
  void handleThreadStartedEvent(final ThreadStartedEvent event) {
    final ThreadEventArguments args = new ThreadEventArguments();
    args.setThreadId((int) event.getThreadId());
    args.setReason(ThreadEventArgumentsReason.STARTED);
    this.debugClient.thread(args);
  }

  /**
   * Handle a {@link ThreadEndedEvent}.
   *
   * @param event Event.
   */
  void handleThreadEndedEvent(final ThreadEndedEvent event) {
    final ThreadEventArguments args = new ThreadEventArguments();
    args.setThreadId((int) event.getThreadId());
    args.setReason(ThreadEventArgumentsReason.EXITED);
    this.debugClient.thread(args);
  }

  /**
   * Handle a {@link StepCompletedEvent}.
   *
   * @param event Event.
   */
  void handleStepCompletedEvent(final StepCompletedEvent event) {
    this.stepper.handleStepCompletedEvent(event);
  }

  /**
   * Handle a {@link BreakpointEvent}. The stepper hands the event to the breakpoint manager.
   *
   * @param event Event.
   * @param clearVariables Clears the variables of the debug client.
   */
  void handleBreakpointEvent(final BreakpointEvent event, final Runnable clearVariables) {
    this.stepper.handleBreakpointEvent(event, clearVariables);
  }

  /** Cancel all steps, as the connection is closing. */
  void cancelSteps() {
    this.stepper.cancelSteps();
  }
}
