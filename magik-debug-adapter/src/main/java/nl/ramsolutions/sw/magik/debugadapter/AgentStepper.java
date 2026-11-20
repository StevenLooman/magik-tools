package nl.ramsolutions.sw.magik.debugadapter;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import nl.ramsolutions.sw.magik.debugadapter.slap.ISlapProtocol;
import nl.ramsolutions.sw.magik.debugadapter.slap.ISlapResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.StepType;
import nl.ramsolutions.sw.magik.debugadapter.slap.events.BreakpointEvent;
import nl.ramsolutions.sw.magik.debugadapter.slap.events.StepCompletedEvent;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.ThreadStackResponse;
import org.eclipse.lsp4j.debug.StoppedEventArguments;
import org.eclipse.lsp4j.debug.StoppedEventArgumentsReason;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Steps with the agent's own step, one at a time until the line or the frame changes. */
class AgentStepper implements Stepper {

  private static final Logger LOGGER = LoggerFactory.getLogger(AgentStepper.class);

  private final ISlapProtocol slapProtocol;
  private final IDebugProtocolClient debugClient;
  private final BreakpointManager breakpointManager;
  private boolean stepCompletedEventReceived;
  private BreakpointEvent breakpointEvent;

  /**
   * Constructor.
   *
   * @param slapProtocol SLAP protocol.
   * @param debugClient LSP4j DebugClient.
   * @param breakpointManager Breakpoint manager, which holds the user breakpoints.
   */
  AgentStepper(
      final ISlapProtocol slapProtocol,
      final IDebugProtocolClient debugClient,
      final BreakpointManager breakpointManager) {
    this.slapProtocol = slapProtocol;
    this.debugClient = debugClient;
    this.breakpointManager = breakpointManager;
  }

  @Override
  public void stepOver(final long threadId)
      throws IOException, InterruptedException, ExecutionException {
    this.step(threadId, StepType.OVER);
  }

  @Override
  public void stepIn(final long threadId)
      throws IOException, InterruptedException, ExecutionException {
    this.step(threadId, StepType.LINE);
  }

  @Override
  public void stepOut(final long threadId)
      throws IOException, InterruptedException, ExecutionException {
    this.step(threadId, StepType.OUT);
  }

  @Override
  public void handleStepCompletedEvent(final StepCompletedEvent event) {
    this.signalStepCompletedEvent();
  }

  @Override
  public void handleBreakpointEvent(final BreakpointEvent event, final Runnable clearVariables) {
    this.breakpointManager.handleBreakpointEvent(event);
    this.recordBreakpointEvent(event);
    clearVariables.run();
  }

  private void step(final long threadId, final StepType stepType)
      throws IOException, InterruptedException, ExecutionException {
    // Record what the stack looks like now.
    final ThreadStackResponse startStack = this.getThreadStack(threadId);
    LOGGER.trace("Start stepping: thread id: {}, step type: {}", threadId, stepType);

    // Do the stepping.
    boolean stepping = true;
    while (stepping) {
      this.recordBreakpointEvent(null);

      LOGGER.trace("Single step start: thread: {}, step type: {}", threadId, stepType);

      // Do a single step.
      final CompletableFuture<ISlapResponse> stepFuture =
          this.slapProtocol.step(threadId, stepType, 1);
      stepFuture.get();

      LOGGER.trace("Single step done: thread: {}, step type: {}", threadId, stepType);

      // Wait for the STEP_COMPLETED event.
      this.waitForStepCompletedEvent();

      // A BreakpointEvent disrupts the stepping.
      if (this.didReceiveBreakpointEvent()) {
        LOGGER.debug("Stop stepping, received breakpoint event");
        return;
      }

      final ThreadStackResponse currentStack = this.getThreadStack(threadId);
      stepping = this.isSteppingOn(stepType, startStack, currentStack);
    }

    // Signal client step is completed.
    final StoppedEventArguments args = new StoppedEventArguments();
    args.setThreadId((int) threadId);
    args.setReason(StoppedEventArgumentsReason.STEP);
    this.debugClient.stopped(args);
  }

  private ThreadStackResponse getThreadStack(final long threadId)
      throws IOException, InterruptedException, ExecutionException {
    final CompletableFuture<ISlapResponse> threadStackFuture =
        this.slapProtocol.getThreadStack(threadId);
    return (ThreadStackResponse) threadStackFuture.get();
  }

  private boolean isSteppingOn(
      final StepType stepType,
      final ThreadStackResponse startStack,
      final ThreadStackResponse currentStack) {
    final List<ThreadStackResponse.StackElement> startFrames = startStack.getStackFrames();
    final List<ThreadStackResponse.StackElement> currentFrames = currentStack.getStackFrames();
    final ThreadStackResponse.StackElement startElement = startFrames.get(0);
    final ThreadStackResponse.StackElement topElement = currentFrames.get(0);
    final String startMethod = startElement.getName();
    final int startLine = startElement.getOffset();
    final int startStackDepth = startFrames.size();
    final String currentMethod = topElement.getName();
    final int currentLine = topElement.getOffset();
    final int currentStackDepth = currentFrames.size();
    LOGGER.trace(
        "Current state: top element: {}, line: {}, stack depth: {}",
        currentMethod,
        currentLine,
        currentStackDepth);

    // Step on over frames which are not Magik.
    final String language = topElement.getLanguage();
    if (!language.equals(FrameSourceResolver.LANGUAGE_MAGIK)) {
      return true;
    }

    // Step out goes on while the stack is as deep, the others while on the same line.
    return switch (stepType) {
      case OUT -> currentStackDepth > startStackDepth;
      case LINE, OVER -> currentMethod.equals(startMethod) && currentLine == startLine;
      default -> true;
    };
  }

  private void waitForStepCompletedEvent() {
    synchronized (this) {
      LOGGER.trace("Waiting for step to complete, value: {}", this.stepCompletedEventReceived);
      while (!this.stepCompletedEventReceived) {
        try {
          this.wait();
        } catch (final InterruptedException exception) {
          // Keep waiting until the step completes.
          Thread.currentThread().interrupt();
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

  private void recordBreakpointEvent(final @Nullable BreakpointEvent newBreakpointEvent) {
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
