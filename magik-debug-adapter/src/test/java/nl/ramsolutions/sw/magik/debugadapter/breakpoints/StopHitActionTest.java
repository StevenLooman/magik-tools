package nl.ramsolutions.sw.magik.debugadapter.breakpoints;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import nl.ramsolutions.sw.magik.debugadapter.breakpoints.BreakpointManager.MagikBreakpoint;
import nl.ramsolutions.sw.magik.debugadapter.breakpoints.BreakpointManager.StopHook;
import org.eclipse.lsp4j.debug.StoppedEventArguments;
import org.eclipse.lsp4j.debug.StoppedEventArgumentsReason;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import org.junit.jupiter.api.Test;

/** Tests for StopHitAction. */
@SuppressWarnings("checkstyle:MagicNumber")
class StopHitActionTest {

  private static final long THREAD_ID = 3;

  private StopHook createRecordingHook(final List<String> events) {
    return new StopHook() {
      @Override
      public void beforeStop() {
        events.add("beforeStop");
      }

      @Override
      public boolean endStepInsteadOfResume(final MagikBreakpoint magikBreakpoint) {
        return false;
      }
    };
  }

  private IDebugProtocolClient createRecordingClient(
      final List<String> events, final List<StoppedEventArguments> stoppedEvents) {
    return new IDebugProtocolClient() {
      @Override
      public void stopped(final StoppedEventArguments args) {
        events.add("stopped");
        stoppedEvents.add(args);
      }
    };
  }

  @Test
  void testStopActionStopsWithReasonBreakpoint() throws IOException, InterruptedException {
    final List<String> events = new ArrayList<>();
    final List<StoppedEventArguments> stoppedEvents = new ArrayList<>();
    final StopHook stopHook = this.createRecordingHook(events);
    final IDebugProtocolClient client = this.createRecordingClient(events, stoppedEvents);
    final MagikBreakpoint magikBreakpoint = new MagikBreakpoint("user:bpt.t()", 0);
    final HitContext context = new HitContext(magikBreakpoint, THREAD_ID, stopHook, null, client);
    final StopHitAction action = new StopHitAction();

    final boolean handled = action.handle(context);

    assertThat(handled).isTrue();
    assertThat(events).containsExactly("beforeStop", "stopped");
    assertThat(stoppedEvents).hasSize(1);
    final StoppedEventArguments stopped = stoppedEvents.get(0);
    final Integer stoppedThreadId = stopped.getThreadId();
    assertThat(stoppedThreadId).isEqualTo(3);
    final String reason = stopped.getReason();
    assertThat(reason).isEqualTo(StoppedEventArgumentsReason.BREAKPOINT);
  }

  @Test
  void testStopActionStopsWithoutBreakpoint() throws IOException, InterruptedException {
    final List<String> events = new ArrayList<>();
    final List<StoppedEventArguments> stoppedEvents = new ArrayList<>();
    final StopHook stopHook = this.createRecordingHook(events);
    final IDebugProtocolClient client = this.createRecordingClient(events, stoppedEvents);
    final HitContext context = new HitContext(null, THREAD_ID, stopHook, null, client);
    final StopHitAction action = new StopHitAction();

    final boolean handled = action.handle(context);

    assertThat(handled).isTrue();
    assertThat(stoppedEvents).hasSize(1);
  }

  @Test
  void testStopActionIsNoLogpointSupport() {
    final StopHitAction action = new StopHitAction();

    final boolean logpointSupport = action.supportsLogpoints();

    assertThat(logpointSupport).isFalse();
  }
}
