package nl.ramsolutions.sw.magik.debugadapter;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import nl.ramsolutions.sw.magik.debugadapter.slap.ErrorMessage;
import nl.ramsolutions.sw.magik.debugadapter.slap.ISlapProtocol;
import nl.ramsolutions.sw.magik.debugadapter.slap.ISlapResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.SlapErrorException;
import nl.ramsolutions.sw.magik.debugadapter.slap.StepType;
import nl.ramsolutions.sw.magik.debugadapter.slap.events.BreakpointEvent;
import nl.ramsolutions.sw.magik.debugadapter.slap.events.StepCompletedEvent;
import nl.ramsolutions.sw.magik.debugadapter.slap.events.ThreadEndedEvent;
import nl.ramsolutions.sw.magik.debugadapter.slap.events.ThreadStartedEvent;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.ErrorResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.EvalResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.SourceFileResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.ThreadInfoResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.ThreadListResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.ThreadStackResponse;
import org.eclipse.lsp4j.debug.StackFrame;
import org.eclipse.lsp4j.debug.StoppedEventArguments;
import org.eclipse.lsp4j.debug.StoppedEventArgumentsReason;
import org.eclipse.lsp4j.debug.Thread;
import org.eclipse.lsp4j.debug.ThreadEventArguments;
import org.eclipse.lsp4j.debug.ThreadEventArgumentsReason;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Thread manager. */
class ThreadManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(ThreadManager.class);

  private static final String LANGUAGE_MAGIK = "Magik";
  private static final String LOOPBODY = "<loopbody>";
  private static final String UNNAMED_PROC = "<unnamed proc>()";
  private static final String EVAL_EXEMPLAR_PACKAGE =
      "_self.define_method_target.meta_at(:exemplar_global).package.association_at(%s).package.name.write_string";

  private final ISlapProtocol slapProtocol;
  private final IDebugProtocolClient debugClient;
  private final PathMapper pathMapper;
  private boolean stepCompletedEventReceived;
  private BreakpointEvent breakpointEvent;

  /**
   * Constructor.
   *
   * @param slapProtocol SLapProtocol.
   * @param debugClient LSP4j DebugClient.
   * @param pathMapper Path mapping.
   */
  ThreadManager(
      final ISlapProtocol slapProtocol,
      final IDebugProtocolClient debugClient,
      final PathMapper pathMapper) {
    this.slapProtocol = slapProtocol;
    this.debugClient = debugClient;
    this.pathMapper = pathMapper;
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
      } catch (ExecutionException exception) {
        LOGGER.trace(
            "Exception while getting thread, id: {}, exception: {}",
            threadId,
            exception.getMessage());

        final Throwable cause = exception.getCause();
        if (!(cause instanceof SlapErrorException
            && ((SlapErrorException) cause).getError().getErrorMessage()
                == ErrorMessage.UNKNOWN_ERROR)) {
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
      if (!stackElement.getLanguage().equals(LANGUAGE_MAGIK)) {
        continue;
      }

      // This sets the frameId to the given stack frames.
      final Path path = this.determinePath(threadId, stackElement);
      final StackFrame stackFrame = Lsp4jConversion.toLsp4j(threadId, stackElement, path);
      stackFrames.add(stackFrame);
    }

    return stackFrames;
  }

  private Path determinePath(
      final long threadId, final ThreadStackResponse.StackElement stackElement)
      throws InterruptedException, ExecutionException, IOException {
    try {
      String method = stackElement.getName();
      if (!method.equals(UNNAMED_PROC) && !method.equals(LOOPBODY)) {
        final int indexDot = method.indexOf('.');
        final int indexBracket = method.indexOf('[');
        final int index = indexDot != -1 ? indexDot + 1 : indexBracket;
        if (!method.contains(":") && index != -1) {
          // Do some extra work to determine package.
          final int level = stackElement.getLevel();

          final String exemplarName = ":|" + method.substring(0, index - 1) + "|";
          final String expr = String.format(EVAL_EXEMPLAR_PACKAGE, exemplarName);
          LOGGER.debug("Eval expression: '{}'", expr);
          final EvalResponse eval =
              (EvalResponse) this.slapProtocol.evaluate(threadId, level, expr).get();
          method = eval.getResult() + ":" + method;

          // Bonus: update exemplar name with package.
          stackElement.setName(method);
        }

        // Clear any spaces (before `<<`/`^<<`). Lazy approach...
        method = method.replace(" ", "");

        // Get source file for method.
        final CompletableFuture<ISlapResponse> sourceFileFuture =
            this.slapProtocol.getSourceFile(method);
        final SourceFileResponse sourceFile = (SourceFileResponse) sourceFileFuture.get();
        final String filename = sourceFile.getFilename();
        final Path daPath = Path.of(filename);
        return this.pathMapper.applyMapping(daPath);
      }
    } catch (final ExecutionException exception) {
      final Throwable cause = exception.getCause();
      if (cause instanceof SlapErrorException
          && ((SlapErrorException) cause).getError().getErrorMessage()
              != ErrorMessage.METHOD_NOT_FOUND) {
        throw exception;
      }
    }

    return null;
  }

  /**
   * Pause a thread.
   *
   * @param threadId Thread ID.
   * @throws IOException -
   * @throws InterruptedException -
   * @throws ExecutionException -
   */
  void pause(final long threadId) throws IOException, InterruptedException, ExecutionException {
    try {
      this.slapProtocol.suspendThread(threadId).get();
    } catch (ExecutionException exception) {
      if (exception.getCause() instanceof SlapErrorException) {
        final SlapErrorException exception2 = (SlapErrorException) exception.getCause();
        final ErrorResponse error = exception2.getError();
        final ErrorMessage errorMessage = error.getErrorMessage();
        if (errorMessage != ErrorMessage.THREAD_ALREADY_SUSPENDED) {
          // Ignore ErrorMessage.THREAD_ALREADY_SUSPENDED.
          throw exception;
        }
      }
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
    this.slapProtocol.resumeThread(threadId).get();
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
    this.step(threadId, StepType.OVER);
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
    this.step(threadId, StepType.LINE);
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
    this.step(threadId, StepType.OUT);
  }

  /**
   * Evaluate an expression in a stack frame.
   *
   * @param frameId Stack frame.
   * @param expression Expression to evaluate.
   * @return Result of evalutate.
   * @throws IOException -
   * @throws InterruptedException -
   * @throws ExecutionException -
   */
  String evaluate(final @Nullable Integer frameId, final String expression)
      throws IOException, InterruptedException, ExecutionException {
    if (frameId == null) {
      throw new IllegalArgumentException("Missing frame ID");
    }

    final long threadId = Lsp4jConversion.frameIdToThreadId(frameId);
    final int level = Lsp4jConversion.frameIdToLevel(frameId);
    final CompletableFuture<ISlapResponse> evaluateFuture =
        this.slapProtocol.evaluate(threadId, level, expression);
    final EvalResponse response = (EvalResponse) evaluateFuture.get();
    return response.getResult();
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
    this.signalStepCompletedEvent();
  }

  private void step(final long threadId, final StepType stepType)
      throws IOException, InterruptedException, ExecutionException {
    // Record what the stack looks like now.
    CompletableFuture<ISlapResponse> threadStackFuture = this.slapProtocol.getThreadStack(threadId);
    ThreadStackResponse threadStack = (ThreadStackResponse) threadStackFuture.get();
    ThreadStackResponse.StackElement topElement = threadStack.getStackFrames().get(0);
    final String startMethod = topElement.getName();
    final int startLine = topElement.getOffset();
    final int startStackDepth = threadStack.getStackFrames().size();
    LOGGER.trace(
        "Current state: thread id: {}, top element: {}, line: {}, stack depth: {}",
        threadId,
        topElement.getName(),
        startLine,
        startStackDepth);

    // Do the stepping.
    boolean stepping = true;
    while (stepping) {
      this.handleBreakpointEvent(null);

      LOGGER.trace("Single step start: thread: {}, step type: {}", threadId, stepType);

      // Do a single step.
      this.slapProtocol.step(threadId, stepType, 1).get();

      LOGGER.trace("Single step done: thread: {}, step type: {}", threadId, stepType);

      // Wait for the STEP_COMPLETED event.
      this.waitForStepCompletedEvent();

      // A BreakpointEvent disrupts the stepping.
      if (this.didReceiveBreakpointEvent()) {
        LOGGER.debug("Stop stepping, received breakpoint event");
        return;
      }

      threadStackFuture = this.slapProtocol.getThreadStack(threadId);
      threadStack = (ThreadStackResponse) threadStackFuture.get();
      topElement = threadStack.getStackFrames().get(0);
      final String currentMethod = topElement.getName();
      final int currentLine = topElement.getOffset();
      final int currentStackDepth = threadStack.getStackFrames().size();
      LOGGER.trace(
          "Current state: thread id: {}, top element: {}, line: {}, stack depth: {}",
          threadId,
          topElement.getName(),
          currentLine,
          currentStackDepth);

      // Check if stack looks like something we want:
      // - Magik
      // - A real method
      // If not, otherwise continue stepping anyway.
      if (!topElement.getLanguage().equals(LANGUAGE_MAGIK)) {
        continue;
      }

      switch (stepType) {
        case LINE:
          // Continue stepping if:
          // - method is the same, and
          // - current line not changed
          stepping = currentMethod.equals(startMethod) && currentLine == startLine;
          break;

        case OUT:
          // Continue stepping if:
          // - stack depth has not decreased
          stepping = currentStackDepth > startStackDepth;
          break;

        case OVER:
          // Continue stepping if:
          // - method is the same, and
          // - current line not changed
          stepping = currentMethod.equals(startMethod) && currentLine == startLine;
          break;

        default:
          break;
      }
    }

    // Signal client step is completed.
    final StoppedEventArguments args = new StoppedEventArguments();
    args.setThreadId((int) threadId);
    args.setReason(StoppedEventArgumentsReason.STEP);
    this.debugClient.stopped(args);
  }

  private void waitForStepCompletedEvent() {
    synchronized (this) {
      LOGGER.trace("Waiting for step to complete, value: {}", this.stepCompletedEventReceived);
      while (!this.stepCompletedEventReceived) {
        try {
          this.wait();
        } catch (InterruptedException exception) {
          // pass; account for spurious wakeups
          java.lang.Thread.currentThread().interrupt();
        }
      }
      this.stepCompletedEventReceived = false;
    }
    LOGGER.trace(
        "Completed waiting for step to complete, value: {}", this.stepCompletedEventReceived);
  }

  private void signalStepCompletedEvent() {
    synchronized (this) {
      this.stepCompletedEventReceived = true;
      this.notifyAll();
    }
  }

  /**
   * Handle breakpoint event.
   *
   * @param newBreakpointEvent Breakpoint event.
   */
  public void handleBreakpointEvent(final @Nullable BreakpointEvent newBreakpointEvent) {
    synchronized (this) {
      this.breakpointEvent = newBreakpointEvent;
    }
  }

  private boolean didReceiveBreakpointEvent() {
    synchronized (this) {
      return this.breakpointEvent != null;
    }
  }
}
