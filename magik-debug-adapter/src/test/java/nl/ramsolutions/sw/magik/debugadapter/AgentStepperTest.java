package nl.ramsolutions.sw.magik.debugadapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import nl.ramsolutions.sw.magik.debugadapter.slap.ISlapResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.StepType;
import nl.ramsolutions.sw.magik.debugadapter.slap.events.BreakpointEvent;
import nl.ramsolutions.sw.magik.debugadapter.slap.events.StepCompletedEvent;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.StepResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.ThreadStackResponse;
import org.eclipse.lsp4j.debug.StoppedEventArguments;
import org.eclipse.lsp4j.debug.StoppedEventArgumentsReason;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for {@link AgentStepper}, the stepping with the agent's own step. */
@SuppressWarnings("checkstyle:MagicNumber")
class AgentStepperTest {

  private static final long THREAD_ID = 3;

  /** A STEP request. */
  private record StepRequest(long threadId, StepType stepType, int count) {}

  /** Scripted agent: answers each stack request with the next stack, completes each step. */
  private static final class ScriptedSlapProtocol extends TestSlapProtocol {

    private final Deque<ThreadStackResponse> stacks = new ArrayDeque<>();
    private final List<StepRequest> stepRequests = new CopyOnWriteArrayList<>();
    private Runnable stepHook = () -> {};

    void addStack(final ThreadStackResponse.StackElement... frames) {
      final List<ISlapResponse> frameList = List.of(frames);
      final ThreadStackResponse stack = new ThreadStackResponse(frameList);
      this.stacks.add(stack);
    }

    void setStepHook(final Runnable hook) {
      this.stepHook = hook;
    }

    List<StepRequest> getStepRequests() {
      return this.stepRequests;
    }

    @Override
    public CompletableFuture<ISlapResponse> getThreadStack(final long threadId) {
      final ThreadStackResponse stack =
          this.stacks.size() > 1 ? this.stacks.poll() : this.stacks.peek();
      return CompletableFuture.completedFuture(stack);
    }

    @Override
    public CompletableFuture<ISlapResponse> step(
        final long threadId, final StepType stepType, final int count) {
      final StepRequest request = new StepRequest(threadId, stepType, count);
      this.stepRequests.add(request);
      this.stepHook.run();
      final StepResponse response = new StepResponse();
      return CompletableFuture.completedFuture(response);
    }
  }

  private ScriptedSlapProtocol slapProtocol;
  private RecordingDebugProtocolClient client;
  private AgentStepper stepper;

  @BeforeEach
  void createStepper() {
    this.slapProtocol = new ScriptedSlapProtocol();
    this.client = new RecordingDebugProtocolClient();
    final BreakpointManager breakpointManager =
        new BreakpointManager(this.slapProtocol, this.client);
    this.stepper = new AgentStepper(this.slapProtocol, this.client, breakpointManager);
    final StepCompletedEvent completed = new StepCompletedEvent(THREAD_ID);
    this.slapProtocol.setStepHook(() -> this.stepper.handleStepCompletedEvent(completed));
  }

  private ThreadStackResponse.StackElement createMagikFrame(
      final int level, final int line, final String name) {
    return new ThreadStackResponse.StackElement(level, line, name, "Magik");
  }

  private ThreadStackResponse.StackElement createJavaFrame(final int level) {
    return new ThreadStackResponse.StackElement(level, 100, "java/lang/Object;m", "Java");
  }

  private void assertSingleStepRequests(final StepType stepType, final int requestCount) {
    final StepRequest request = new StepRequest(THREAD_ID, stepType, 1);
    final List<StepRequest> expected = Collections.nCopies(requestCount, request);
    final List<StepRequest> stepRequests = this.slapProtocol.getStepRequests();
    assertThat(stepRequests).isEqualTo(expected);
  }

  private void assertStoppedForStep() {
    final List<StoppedEventArguments> stoppedEvents = this.client.getStoppedEvents();
    assertThat(stoppedEvents).hasSize(1);
    final StoppedEventArguments stopped = stoppedEvents.get(0);
    final Integer threadId = stopped.getThreadId();
    assertThat(threadId).isEqualTo((int) THREAD_ID);
    final String reason = stopped.getReason();
    assertThat(reason).isEqualTo(StoppedEventArgumentsReason.STEP);
  }

  @Test
  void testStepOverStepsOneAtATimeUntilLineChanges()
      throws IOException, InterruptedException, ExecutionException {
    final ThreadStackResponse.StackElement startFrame = this.createMagikFrame(0, 10, "bpt.t()");
    final ThreadStackResponse.StackElement nextFrame = this.createMagikFrame(0, 11, "bpt.t()");
    this.slapProtocol.addStack(startFrame);
    this.slapProtocol.addStack(startFrame);
    this.slapProtocol.addStack(nextFrame);

    this.stepper.stepOver(THREAD_ID);

    this.assertSingleStepRequests(StepType.OVER, 2);
    this.assertStoppedForStep();
  }

  @Test
  void testStepInStepsOneAtATimeUntilMethodChanges()
      throws IOException, InterruptedException, ExecutionException {
    final ThreadStackResponse.StackElement startFrame = this.createMagikFrame(0, 10, "bpt.t()");
    final ThreadStackResponse.StackElement calledFrame = this.createMagikFrame(0, 10, "bpt.t2()");
    final ThreadStackResponse.StackElement callerFrame = this.createMagikFrame(1, 10, "bpt.t()");
    this.slapProtocol.addStack(startFrame);
    this.slapProtocol.addStack(startFrame);
    this.slapProtocol.addStack(calledFrame, callerFrame);

    this.stepper.stepIn(THREAD_ID);

    this.assertSingleStepRequests(StepType.LINE, 2);
    this.assertStoppedForStep();
  }

  @Test
  void testStepOutStepsOneAtATimeWhileStackIsDeeper()
      throws IOException, InterruptedException, ExecutionException {
    final ThreadStackResponse.StackElement startFrame = this.createMagikFrame(0, 20, "bpt.t2()");
    final ThreadStackResponse.StackElement callerFrame = this.createMagikFrame(1, 10, "bpt.t()");
    this.slapProtocol.addStack(startFrame, callerFrame);
    // The first step stops in a method which t2() calls.
    final ThreadStackResponse.StackElement calledFrame = this.createMagikFrame(0, 30, "bpt.t3()");
    final ThreadStackResponse.StackElement middleFrame = this.createMagikFrame(1, 20, "bpt.t2()");
    final ThreadStackResponse.StackElement bottomFrame = this.createMagikFrame(2, 10, "bpt.t()");
    this.slapProtocol.addStack(calledFrame, middleFrame, bottomFrame);
    final ThreadStackResponse.StackElement returnedFrame = this.createMagikFrame(0, 10, "bpt.t()");
    this.slapProtocol.addStack(returnedFrame);

    this.stepper.stepOut(THREAD_ID);

    this.assertSingleStepRequests(StepType.OUT, 2);
    this.assertStoppedForStep();
  }

  @Test
  void testStepOverStepsOnOverFrameWhichIsNotMagik()
      throws IOException, InterruptedException, ExecutionException {
    final ThreadStackResponse.StackElement startFrame = this.createMagikFrame(0, 10, "bpt.t()");
    final ThreadStackResponse.StackElement javaFrame = this.createJavaFrame(0);
    final ThreadStackResponse.StackElement callerFrame = this.createMagikFrame(1, 10, "bpt.t()");
    final ThreadStackResponse.StackElement nextFrame = this.createMagikFrame(0, 11, "bpt.t()");
    this.slapProtocol.addStack(startFrame);
    this.slapProtocol.addStack(javaFrame, callerFrame);
    this.slapProtocol.addStack(nextFrame);

    this.stepper.stepOver(THREAD_ID);

    this.assertSingleStepRequests(StepType.OVER, 2);
    this.assertStoppedForStep();
  }

  @Test
  void testBreakpointEventEndsStepWithoutStepStop()
      throws IOException, InterruptedException, ExecutionException {
    final ThreadStackResponse.StackElement startFrame = this.createMagikFrame(0, 10, "bpt.t()");
    this.slapProtocol.addStack(startFrame);
    // The breakpoint arrives during the step, before its STEP_COMPLETED.
    final BreakpointEvent breakpointHit = new BreakpointEvent(99, THREAD_ID);
    final StepCompletedEvent completed = new StepCompletedEvent(THREAD_ID);
    this.slapProtocol.setStepHook(
        () -> {
          this.stepper.handleBreakpointEvent(breakpointHit, () -> {});
          this.stepper.handleStepCompletedEvent(completed);
        });

    this.stepper.stepOver(THREAD_ID);

    // The breakpoint manager reported the stop, the step sent no stop of its own.
    this.assertSingleStepRequests(StepType.OVER, 1);
    final List<StoppedEventArguments> stoppedEvents = this.client.getStoppedEvents();
    assertThat(stoppedEvents).hasSize(1);
    final StoppedEventArguments stopped = stoppedEvents.get(0);
    final String reason = stopped.getReason();
    assertThat(reason).isEqualTo(StoppedEventArgumentsReason.BREAKPOINT);
  }

  @Test
  void testBreakpointEventClearsVariablesAfterStop() {
    final BreakpointEvent breakpointHit = new BreakpointEvent(99, THREAD_ID);
    final List<Integer> stopsAtClear = new ArrayList<>();
    final Runnable clearVariables =
        () -> {
          final List<StoppedEventArguments> stoppedEvents = this.client.getStoppedEvents();
          stopsAtClear.add(stoppedEvents.size());
        };

    this.stepper.handleBreakpointEvent(breakpointHit, clearVariables);

    // The breakpoint manager reports the stop, then the variables are cleared.
    assertThat(stopsAtClear).containsExactly(1);
  }
}
