package nl.ramsolutions.sw.magik.debugadapter.breakpoints;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import nl.ramsolutions.sw.magik.debugadapter.RecordingDebugProtocolClient;
import nl.ramsolutions.sw.magik.debugadapter.TestSlapProtocol;
import nl.ramsolutions.sw.magik.debugadapter.breakpoints.BreakpointManager.MagikBreakpoint;
import nl.ramsolutions.sw.magik.debugadapter.slap.ErrorMessage;
import nl.ramsolutions.sw.magik.debugadapter.slap.ISlapProtocol;
import nl.ramsolutions.sw.magik.debugadapter.slap.ISlapResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.RequestType;
import nl.ramsolutions.sw.magik.debugadapter.slap.SlapErrorException;
import nl.ramsolutions.sw.magik.debugadapter.slap.events.BreakpointEvent;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.BreakpointModifyResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.BreakpointSetResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.ErrorResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.EvalResponse;
import org.eclipse.lsp4j.debug.FunctionBreakpoint;
import org.eclipse.lsp4j.debug.Source;
import org.eclipse.lsp4j.debug.SourceBreakpoint;
import org.eclipse.lsp4j.debug.StoppedEventArguments;
import org.eclipse.lsp4j.debug.StoppedEventArgumentsReason;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests for BreakpointManager. */
@SuppressWarnings("checkstyle:MagicNumber")
class BreakpointManagerTest {

  /**
   * VSCode runs from module directory, mvn runs from project directory.
   *
   * @return Proper {@link Path} to use.
   */
  private Path getPath(final String relativePath) {
    final Path path = Path.of(".").toAbsolutePath().getParent();
    if (path.endsWith("magik-debug-adapter")) {
      return Path.of("..").resolve(relativePath);
    }
    return Path.of(".").resolve(relativePath);
  }

  @BeforeEach
  void disableLogger() {
    final Logger logger = Logger.getLogger(BreakpointManager.class.getName());
    logger.setUseParentHandlers(false);
  }

  @Test
  void testAddBreakpoint() throws IOException, InterruptedException, ExecutionException {
    final TestSlapProtocol slapProtocol = new TestSlapProtocol();
    final BreakpointManager manager = new BreakpointManager(slapProtocol, null);

    final SourceBreakpoint sourceBreakpoint = new SourceBreakpoint();
    sourceBreakpoint.setLine(17);
    final Source source = new Source();
    source.setPath(this.getPath("magik-debug-adapter/src/test/resources/bpt.magik").toString());
    final BreakpointManager.MagikBreakpoint breakpoint =
        manager.addBreakpoint(source, sourceBreakpoint);
    assertThat(breakpoint.getMethodName()).isEqualTo("user:bpt.t()");
    assertThat(breakpoint.getMethodLine()).isZero();
    assertThat(breakpoint.getCondition()).isNull();
    assertThat(breakpoint.getBreakpointId()).isNotEqualTo(ISlapProtocol.INVALID_BREAKPOINT_ID);
  }

  @Test
  void testAddBreakpointInvalidLine() throws IOException, InterruptedException, ExecutionException {
    final TestSlapProtocol slapProtocol =
        new TestSlapProtocol() {
          @Override
          public CompletableFuture<ISlapResponse> setBreakpoint(String method, int line) {
            ErrorResponse errorResponse =
                new ErrorResponse(RequestType.BREAKPOINT_SET, ErrorMessage.INVALID_LINE_NUMBER);
            SlapErrorException exception = new SlapErrorException(errorResponse);

            CompletableFuture<ISlapResponse> future = new CompletableFuture<>();
            future.completeExceptionally(exception);
            return future;
          }
        };
    final BreakpointManager manager = new BreakpointManager(slapProtocol, null);

    final SourceBreakpoint sourceBreakpoint = new SourceBreakpoint();
    sourceBreakpoint.setLine(18);
    final Source source = new Source();
    source.setPath(this.getPath("magik-debug-adapter/src/test/resources/bpt.magik").toString());
    final BreakpointManager.MagikBreakpoint breakpoint =
        manager.addBreakpoint(source, sourceBreakpoint);
    assertThat(breakpoint.getMethodName()).isEqualTo("user:bpt.t()");
    assertThat(breakpoint.getMethodLine()).isEqualTo(18);
    assertThat(breakpoint.getCondition()).isNull();
    assertThat(breakpoint.getMessage()).isEqualTo("INVALID_LINE_NUMBER");
    assertThat(breakpoint.getBreakpointId()).isEqualTo(ISlapProtocol.INVALID_BREAKPOINT_ID);
  }

  @Test
  void testAddBreakpointMethodNotFound()
      throws IOException, InterruptedException, ExecutionException {
    final TestSlapProtocol slapProtocol =
        new TestSlapProtocol() {
          @Override
          public CompletableFuture<ISlapResponse> setBreakpoint(String method, int line) {
            final ErrorResponse errorResponse =
                new ErrorResponse(RequestType.BREAKPOINT_SET, ErrorMessage.METHOD_NOT_FOUND);
            final SlapErrorException exception = new SlapErrorException(errorResponse);

            final CompletableFuture<ISlapResponse> future = new CompletableFuture<>();
            future.completeExceptionally(exception);
            return future;
          }
        };
    final BreakpointManager manager = new BreakpointManager(slapProtocol, null);

    final SourceBreakpoint sourceBreakpoint = new SourceBreakpoint();
    sourceBreakpoint.setLine(18);
    final Source source = new Source();
    source.setPath(this.getPath("magik-debug-adapter/src/test/resources/bpt.magik").toString());
    final BreakpointManager.MagikBreakpoint breakpoint =
        manager.addBreakpoint(source, sourceBreakpoint);
    assertThat(breakpoint.getMethodName()).isEqualTo("user:bpt.t()");
    assertThat(breakpoint.getMethodLine()).isEqualTo(18);
    assertThat(breakpoint.getCondition()).isNull();
    assertThat(breakpoint.getMessage()).isEqualTo("METHOD_NOT_FOUND");
    assertThat(breakpoint.getBreakpointId()).isEqualTo(ISlapProtocol.INVALID_BREAKPOINT_ID);
  }

  private SourceBreakpoint createSourceBreakpoint(
      final int line, final String condition, final String logMessage) {
    final SourceBreakpoint sourceBreakpoint = new SourceBreakpoint();
    sourceBreakpoint.setLine(line);
    sourceBreakpoint.setCondition(condition);
    sourceBreakpoint.setLogMessage(logMessage);
    return sourceBreakpoint;
  }

  private Source createBptSource() {
    final Path path = this.getPath("magik-debug-adapter/src/test/resources/bpt.magik");
    final Source source = new Source();
    source.setPath(path.toString());
    return source;
  }

  private TestSlapProtocol createRecordingSlapProtocol(final List<String> calls) {
    return new TestSlapProtocol() {
      @Override
      public CompletableFuture<ISlapResponse> setBreakpoint(final String method, final int line) {
        calls.add("setBreakpoint");
        return super.setBreakpoint(method, line);
      }

      @Override
      public CompletableFuture<ISlapResponse> deleteBreakpoint(final long breakpointId) {
        calls.add("deleteBreakpoint");
        return super.deleteBreakpoint(breakpointId);
      }
    };
  }

  @Test
  void testEditedConditionIsUpdatedInPlace()
      throws IOException, InterruptedException, ExecutionException {
    final List<String> calls = new ArrayList<>();
    final TestSlapProtocol slapProtocol = this.createRecordingSlapProtocol(calls);
    final BreakpointManager manager = new BreakpointManager(slapProtocol, null);
    final Source source = this.createBptSource();

    final SourceBreakpoint firstSourceBreakpoint = this.createSourceBreakpoint(18, "a = 1", null);
    final SourceBreakpoint[] firstSourceBreakpoints = {firstSourceBreakpoint};
    final List<BreakpointManager.MagikBreakpoint> firstBreakpoints =
        manager.setBreakpoints(source, firstSourceBreakpoints);
    final BreakpointManager.MagikBreakpoint first = firstBreakpoints.get(0);
    final SourceBreakpoint secondSourceBreakpoint = this.createSourceBreakpoint(18, "a = 2", null);
    final SourceBreakpoint[] secondSourceBreakpoints = {secondSourceBreakpoint};
    final List<BreakpointManager.MagikBreakpoint> breakpoints =
        manager.setBreakpoints(source, secondSourceBreakpoints);

    assertThat(breakpoints).hasSize(1);
    final BreakpointManager.MagikBreakpoint breakpoint = breakpoints.get(0);
    assertThat(breakpoint).isSameAs(first);
    final String condition = breakpoint.getCondition();
    assertThat(condition).isEqualTo("a = 2");
    assertThat(calls).containsExactly("setBreakpoint");
  }

  @Test
  void testEditedLogMessageIsUpdatedInPlace()
      throws IOException, InterruptedException, ExecutionException {
    final List<String> calls = new ArrayList<>();
    final TestSlapProtocol slapProtocol = this.createRecordingSlapProtocol(calls);
    final BreakpointManager manager = new BreakpointManager(slapProtocol, null);
    final Source source = this.createBptSource();

    final SourceBreakpoint firstSourceBreakpoint = this.createSourceBreakpoint(18, null, "x={x}");
    final SourceBreakpoint[] firstSourceBreakpoints = {firstSourceBreakpoint};
    final List<BreakpointManager.MagikBreakpoint> firstBreakpoints =
        manager.setBreakpoints(source, firstSourceBreakpoints);
    final BreakpointManager.MagikBreakpoint first = firstBreakpoints.get(0);
    final SourceBreakpoint secondSourceBreakpoint = this.createSourceBreakpoint(18, null, "y={y}");
    final SourceBreakpoint[] secondSourceBreakpoints = {secondSourceBreakpoint};
    final List<BreakpointManager.MagikBreakpoint> breakpoints =
        manager.setBreakpoints(source, secondSourceBreakpoints);

    assertThat(breakpoints).hasSize(1);
    final BreakpointManager.MagikBreakpoint breakpoint = breakpoints.get(0);
    assertThat(breakpoint).isSameAs(first);
    final String logMessage = breakpoint.getLogMessage();
    assertThat(logMessage).isEqualTo("y={y}");
    assertThat(calls).containsExactly("setBreakpoint");
  }

  @Test
  void testRemovedConditionAndLogMessageAreClearedInPlace()
      throws IOException, InterruptedException, ExecutionException {
    final TestSlapProtocol slapProtocol = new TestSlapProtocol();
    final BreakpointManager manager = new BreakpointManager(slapProtocol, null);
    final Source source = this.createBptSource();

    final SourceBreakpoint firstSourceBreakpoint =
        this.createSourceBreakpoint(18, "a = 1", "x={x}");
    final SourceBreakpoint[] firstSourceBreakpoints = {firstSourceBreakpoint};
    manager.setBreakpoints(source, firstSourceBreakpoints);
    final SourceBreakpoint secondSourceBreakpoint = this.createSourceBreakpoint(18, null, null);
    final SourceBreakpoint[] secondSourceBreakpoints = {secondSourceBreakpoint};
    final List<BreakpointManager.MagikBreakpoint> breakpoints =
        manager.setBreakpoints(source, secondSourceBreakpoints);

    assertThat(breakpoints).hasSize(1);
    final BreakpointManager.MagikBreakpoint breakpoint = breakpoints.get(0);
    final String condition = breakpoint.getCondition();
    assertThat(condition).isNull();
    final String logMessage = breakpoint.getLogMessage();
    assertThat(logMessage).isNull();
  }

  @Test
  void testLogMessageIsKeptOnANewBreakpoint()
      throws IOException, InterruptedException, ExecutionException {
    final TestSlapProtocol slapProtocol = new TestSlapProtocol();
    final BreakpointManager manager = new BreakpointManager(slapProtocol, null);
    final Source source = this.createBptSource();

    final SourceBreakpoint sourceBreakpoint = this.createSourceBreakpoint(18, null, "x={x}");
    final BreakpointManager.MagikBreakpoint breakpoint =
        manager.addBreakpoint(source, sourceBreakpoint);

    final String logMessage = breakpoint.getLogMessage();
    assertThat(logMessage).isEqualTo("x={x}");
  }

  @Test
  void testHeaderLineBreakpointIsKeptAcrossSetBreakpoints()
      throws IOException, InterruptedException, ExecutionException {
    final List<String> calls = new ArrayList<>();
    final TestSlapProtocol slapProtocol = this.createRecordingSlapProtocol(calls);
    final BreakpointManager manager = new BreakpointManager(slapProtocol, null);
    final Source source = this.createBptSource();

    final SourceBreakpoint firstSourceBreakpoint = this.createSourceBreakpoint(17, null, null);
    final SourceBreakpoint[] firstSourceBreakpoints = {firstSourceBreakpoint};
    final List<BreakpointManager.MagikBreakpoint> firstBreakpoints =
        manager.setBreakpoints(source, firstSourceBreakpoints);
    final BreakpointManager.MagikBreakpoint first = firstBreakpoints.get(0);
    final SourceBreakpoint secondSourceBreakpoint = this.createSourceBreakpoint(17, null, null);
    final SourceBreakpoint[] secondSourceBreakpoints = {secondSourceBreakpoint};
    final List<BreakpointManager.MagikBreakpoint> breakpoints =
        manager.setBreakpoints(source, secondSourceBreakpoints);

    assertThat(breakpoints).hasSize(1);
    final BreakpointManager.MagikBreakpoint breakpoint = breakpoints.get(0);
    assertThat(breakpoint).isSameAs(first);
    assertThat(calls).containsExactly("setBreakpoint");
  }

  @Test
  void testEditedConditionOnHeaderLineIsUpdatedInPlace()
      throws IOException, InterruptedException, ExecutionException {
    final List<String> calls = new ArrayList<>();
    final TestSlapProtocol slapProtocol = this.createRecordingSlapProtocol(calls);
    final BreakpointManager manager = new BreakpointManager(slapProtocol, null);
    final Source source = this.createBptSource();

    final SourceBreakpoint firstSourceBreakpoint = this.createSourceBreakpoint(17, "a = 1", null);
    final SourceBreakpoint[] firstSourceBreakpoints = {firstSourceBreakpoint};
    final List<BreakpointManager.MagikBreakpoint> firstBreakpoints =
        manager.setBreakpoints(source, firstSourceBreakpoints);
    final BreakpointManager.MagikBreakpoint first = firstBreakpoints.get(0);
    final SourceBreakpoint secondSourceBreakpoint = this.createSourceBreakpoint(17, "a = 2", null);
    final SourceBreakpoint[] secondSourceBreakpoints = {secondSourceBreakpoint};
    final List<BreakpointManager.MagikBreakpoint> breakpoints =
        manager.setBreakpoints(source, secondSourceBreakpoints);

    assertThat(breakpoints).hasSize(1);
    final BreakpointManager.MagikBreakpoint breakpoint = breakpoints.get(0);
    assertThat(breakpoint).isSameAs(first);
    final String condition = breakpoint.getCondition();
    assertThat(condition).isEqualTo("a = 2");
    assertThat(calls).containsExactly("setBreakpoint");
  }

  /** Slap protocol which, as the agent does, refuses a second breakpoint at a location. */
  private TestSlapProtocol createLocationSlapProtocol() {
    return new TestSlapProtocol() {
      private final Map<Long, String> locations = new HashMap<>();

      @Override
      public CompletableFuture<ISlapResponse> setBreakpoint(final String method, final int line) {
        final String location = method + "@" + line;
        if (this.locations.containsValue(location)) {
          final ErrorResponse errorResponse =
              new ErrorResponse(
                  RequestType.BREAKPOINT_SET, ErrorMessage.BREAKPOINT_ALREADY_SET_AT_LOCATION);
          final SlapErrorException exception = new SlapErrorException(errorResponse);
          return CompletableFuture.failedFuture(exception);
        }
        final CompletableFuture<ISlapResponse> future = super.setBreakpoint(method, line);
        final BreakpointSetResponse response = (BreakpointSetResponse) future.join();
        final long breakpointId = response.getBreakpointId();
        this.locations.put(breakpointId, location);
        return future;
      }

      @Override
      public CompletableFuture<ISlapResponse> deleteBreakpoint(final long breakpointId) {
        this.locations.remove(breakpointId);
        return super.deleteBreakpoint(breakpointId);
      }
    };
  }

  @Test
  void testHeaderLineBreakpointMovedToTheShiftedHeaderStaysVerified(@TempDir final Path directory)
      throws IOException, InterruptedException, ExecutionException {
    final Path path = directory.resolve("moved.magik");
    final String method = "_method bpt.m()\n    _return 1\n_endmethod\n$\n";
    Files.writeString(path, "_package user\n" + method);
    final TestSlapProtocol slapProtocol = this.createLocationSlapProtocol();
    final BreakpointManager manager = new BreakpointManager(slapProtocol, null);
    final Source source = new Source();
    source.setPath(path.toString());
    final SourceBreakpoint headerBreakpoint = this.createSourceBreakpoint(2, null, null);
    final SourceBreakpoint[] headerBreakpoints = {headerBreakpoint};
    manager.setBreakpoints(source, headerBreakpoints);

    // An edit inserts a line above the method and the client moves the breakpoint along.
    Files.writeString(path, "_package user\n\n" + method);
    final SourceBreakpoint movedBreakpoint = this.createSourceBreakpoint(3, null, null);
    final SourceBreakpoint[] movedBreakpoints = {movedBreakpoint};
    final List<BreakpointManager.MagikBreakpoint> breakpoints =
        manager.setBreakpoints(source, movedBreakpoints);

    assertThat(breakpoints).hasSize(1);
    final BreakpointManager.MagikBreakpoint breakpoint = breakpoints.get(0);
    final boolean verified = breakpoint.isVerified();
    assertThat(verified).isTrue();
    final int sourceLine = breakpoint.getSourceLine();
    assertThat(sourceLine).isEqualTo(3);
  }

  @Test
  void testFailingFallbackActionDoesNotEscape() {
    final TestSlapProtocol slapProtocol = new TestSlapProtocol();
    final RecordingDebugProtocolClient client =
        new RecordingDebugProtocolClient() {
          @Override
          public void stopped(final StoppedEventArguments args) {
            throw new IllegalStateException("client gone");
          }
        };
    final BreakpointManager manager = new BreakpointManager(slapProtocol, client, List.of());
    final BreakpointEvent event = new BreakpointEvent(99, 3);

    final Throwable thrown = catchThrowable(() -> manager.handleBreakpointEvent(event));

    assertThat(thrown).isNull();
  }

  private FunctionBreakpoint createFunctionBreakpoint(final String name) {
    final FunctionBreakpoint functionBreakpoint = new FunctionBreakpoint();
    functionBreakpoint.setName(name);
    return functionBreakpoint;
  }

  @Test
  void testClearFunctionBreakpoints() throws IOException, InterruptedException, ExecutionException {
    final List<Long> deletedIds = new ArrayList<>();
    final TestSlapProtocol slapProtocol =
        new TestSlapProtocol() {
          @Override
          public CompletableFuture<ISlapResponse> deleteBreakpoint(final long breakpointId) {
            deletedIds.add(breakpointId);
            return super.deleteBreakpoint(breakpointId);
          }
        };
    final BreakpointManager manager = new BreakpointManager(slapProtocol, null);
    final FunctionBreakpoint[] functionBreakpoints = {
      this.createFunctionBreakpoint("user:a.m1()"), this.createFunctionBreakpoint("user:a.m2()")
    };
    final List<BreakpointManager.MagikBreakpoint> breakpoints =
        manager.addFunctionBreakpoints(functionBreakpoints);
    final long firstId = breakpoints.get(0).getBreakpointId();
    final long secondId = breakpoints.get(1).getBreakpointId();

    manager.clearFunctionBreakpoints();

    assertThat(deletedIds).containsExactlyInAnyOrder(firstId, secondId);
    final BreakpointManager.MagikBreakpoint first = manager.getBreakpoint(firstId);
    assertThat(first).isNull();
    final BreakpointManager.MagikBreakpoint second = manager.getBreakpoint(secondId);
    assertThat(second).isNull();
  }

  @Test
  void testHandleBreakpointEventUnknownBreakpointId() {
    final TestSlapProtocol slapProtocol = new TestSlapProtocol();
    final RecordingDebugProtocolClient client = new RecordingDebugProtocolClient();
    final BreakpointManager manager = new BreakpointManager(slapProtocol, client);

    final BreakpointEvent event = new BreakpointEvent(99, 3);
    manager.handleBreakpointEvent(event);

    final List<StoppedEventArguments> stoppedEvents = client.getStoppedEvents();
    assertThat(stoppedEvents).hasSize(1);
    final StoppedEventArguments stopped = stoppedEvents.get(0);
    final Integer stoppedThreadId = stopped.getThreadId();
    assertThat(stoppedThreadId).isEqualTo(3);
    final String reason = stopped.getReason();
    assertThat(reason).isEqualTo(StoppedEventArgumentsReason.BREAKPOINT);
  }

  private HitAction createRecordingAction(
      final List<HitContext> seenContexts, final boolean handled) {
    return context -> {
      seenContexts.add(context);
      return handled;
    };
  }

  private TestSlapProtocol createConditionSlapProtocol(
      final String conditionResult, final List<Long> resumedThreadIds) {
    return new TestSlapProtocol() {
      @Override
      public CompletableFuture<ISlapResponse> evaluate(
          final long threadId, final int level, final String expression) {
        final ISlapResponse response = new EvalResponse(conditionResult);
        return CompletableFuture.completedFuture(response);
      }

      @Override
      public CompletableFuture<ISlapResponse> resumeThread(final long threadId) {
        resumedThreadIds.add(threadId);
        final ISlapResponse response = new BreakpointModifyResponse();
        return CompletableFuture.completedFuture(response);
      }
    };
  }

  @Test
  void testEarlierActionCanHandleTheHit() {
    final TestSlapProtocol slapProtocol = new TestSlapProtocol();
    final RecordingDebugProtocolClient client = new RecordingDebugProtocolClient();
    final List<HitContext> seenContexts = new ArrayList<>();
    final HitAction handlingAction = this.createRecordingAction(seenContexts, true);
    final List<HitAction> actions = List.of(handlingAction, new StopHitAction());
    final BreakpointManager manager = new BreakpointManager(slapProtocol, client, actions);

    final BreakpointEvent event = new BreakpointEvent(99, 3);
    manager.handleBreakpointEvent(event);

    assertThat(seenContexts).hasSize(1);
    final List<StoppedEventArguments> stoppedEvents = client.getStoppedEvents();
    assertThat(stoppedEvents).isEmpty();
  }

  @Test
  void testUnknownIdStillReachesTheActionsAndStops() {
    final TestSlapProtocol slapProtocol = new TestSlapProtocol();
    final RecordingDebugProtocolClient client = new RecordingDebugProtocolClient();
    final List<HitContext> seenContexts = new ArrayList<>();
    final HitAction passingAction = this.createRecordingAction(seenContexts, false);
    final List<HitAction> actions = List.of(passingAction, new StopHitAction());
    final BreakpointManager manager = new BreakpointManager(slapProtocol, client, actions);

    final BreakpointEvent event = new BreakpointEvent(99, 3);
    manager.handleBreakpointEvent(event);

    assertThat(seenContexts).hasSize(1);
    final HitContext context = seenContexts.get(0);
    final MagikBreakpoint contextBreakpoint = context.breakpoint();
    assertThat(contextBreakpoint).isNull();
    final long contextThreadId = context.threadId();
    assertThat(contextThreadId).isEqualTo(3);
    final List<StoppedEventArguments> stoppedEvents = client.getStoppedEvents();
    assertThat(stoppedEvents).hasSize(1);
  }

  @Test
  void testActionsGetTheKnownBreakpoint()
      throws IOException, InterruptedException, ExecutionException {
    final TestSlapProtocol slapProtocol = new TestSlapProtocol();
    final RecordingDebugProtocolClient client = new RecordingDebugProtocolClient();
    final List<HitContext> seenContexts = new ArrayList<>();
    final HitAction handlingAction = this.createRecordingAction(seenContexts, true);
    final List<HitAction> actions = List.of(handlingAction, new StopHitAction());
    final BreakpointManager manager = new BreakpointManager(slapProtocol, client, actions);
    final FunctionBreakpoint functionBreakpoint = this.createFunctionBreakpoint("user:a.m1()");
    final MagikBreakpoint magikBreakpoint = manager.addFunctionBreakpoint(functionBreakpoint);

    final long breakpointId = magikBreakpoint.getBreakpointId();
    final BreakpointEvent event = new BreakpointEvent(breakpointId, 3);
    manager.handleBreakpointEvent(event);

    assertThat(seenContexts).hasSize(1);
    final HitContext context = seenContexts.get(0);
    final MagikBreakpoint contextBreakpoint = context.breakpoint();
    assertThat(contextBreakpoint).isSameAs(magikBreakpoint);
  }

  @Test
  void testFalseConditionNeverReachesTheActions()
      throws IOException, InterruptedException, ExecutionException {
    final List<Long> resumedThreadIds = new ArrayList<>();
    final TestSlapProtocol slapProtocol =
        this.createConditionSlapProtocol("False", resumedThreadIds);
    final RecordingDebugProtocolClient client = new RecordingDebugProtocolClient();
    final List<HitContext> seenContexts = new ArrayList<>();
    final HitAction recordingAction = this.createRecordingAction(seenContexts, false);
    final List<HitAction> actions = List.of(recordingAction, new StopHitAction());
    final BreakpointManager manager = new BreakpointManager(slapProtocol, client, actions);
    final FunctionBreakpoint functionBreakpoint = this.createFunctionBreakpoint("user:a.m1()");
    functionBreakpoint.setCondition("a = 1");
    final MagikBreakpoint magikBreakpoint = manager.addFunctionBreakpoint(functionBreakpoint);

    final long breakpointId = magikBreakpoint.getBreakpointId();
    final BreakpointEvent event = new BreakpointEvent(breakpointId, 3);
    manager.handleBreakpointEvent(event);

    assertThat(seenContexts).isEmpty();
    final List<StoppedEventArguments> stoppedEvents = client.getStoppedEvents();
    assertThat(stoppedEvents).isEmpty();
    assertThat(resumedThreadIds).containsExactly(3L);
  }

  @Test
  void testTrueConditionReachesTheActions()
      throws IOException, InterruptedException, ExecutionException {
    final List<Long> resumedThreadIds = new ArrayList<>();
    final TestSlapProtocol slapProtocol =
        this.createConditionSlapProtocol("True", resumedThreadIds);
    final RecordingDebugProtocolClient client = new RecordingDebugProtocolClient();
    final List<HitContext> seenContexts = new ArrayList<>();
    final HitAction recordingAction = this.createRecordingAction(seenContexts, true);
    final List<HitAction> actions = List.of(recordingAction, new StopHitAction());
    final BreakpointManager manager = new BreakpointManager(slapProtocol, client, actions);
    final FunctionBreakpoint functionBreakpoint = this.createFunctionBreakpoint("user:a.m1()");
    functionBreakpoint.setCondition("a = 1");
    final MagikBreakpoint magikBreakpoint = manager.addFunctionBreakpoint(functionBreakpoint);

    final long breakpointId = magikBreakpoint.getBreakpointId();
    final BreakpointEvent event = new BreakpointEvent(breakpointId, 3);
    manager.handleBreakpointEvent(event);

    assertThat(seenContexts).hasSize(1);
    assertThat(resumedThreadIds).isEmpty();
  }

  @Test
  void testThrowingActionFallsThroughToTheNextAction() {
    final TestSlapProtocol slapProtocol = new TestSlapProtocol();
    final RecordingDebugProtocolClient client = new RecordingDebugProtocolClient();
    final HitAction throwingAction =
        context -> {
          throw new IllegalStateException("failed");
        };
    final List<HitAction> actions = List.of(throwingAction, new StopHitAction());
    final BreakpointManager manager = new BreakpointManager(slapProtocol, client, actions);

    final BreakpointEvent event = new BreakpointEvent(99, 3);
    manager.handleBreakpointEvent(event);

    final List<StoppedEventArguments> stoppedEvents = client.getStoppedEvents();
    assertThat(stoppedEvents).hasSize(1);
  }

  @Test
  void testHitNoActionHandlesStillStops() {
    final TestSlapProtocol slapProtocol = new TestSlapProtocol();
    final RecordingDebugProtocolClient client = new RecordingDebugProtocolClient();
    final List<HitContext> seenContexts = new ArrayList<>();
    final HitAction passingAction = this.createRecordingAction(seenContexts, false);
    final List<HitAction> actions = List.of(passingAction);
    final BreakpointManager manager = new BreakpointManager(slapProtocol, client, actions);

    final BreakpointEvent event = new BreakpointEvent(99, 3);
    manager.handleBreakpointEvent(event);

    assertThat(seenContexts).hasSize(1);
    final List<StoppedEventArguments> stoppedEvents = client.getStoppedEvents();
    assertThat(stoppedEvents).hasSize(1);
    final StoppedEventArguments stopped = stoppedEvents.get(0);
    final String reason = stopped.getReason();
    assertThat(reason).isEqualTo(StoppedEventArgumentsReason.BREAKPOINT);
  }

  @Test
  void testFailingActionFallsThroughToTheNextAction() {
    final TestSlapProtocol slapProtocol = new TestSlapProtocol();
    final RecordingDebugProtocolClient client = new RecordingDebugProtocolClient();
    final HitAction failingAction =
        context -> {
          throw new IOException("failed");
        };
    final List<HitAction> actions = List.of(failingAction, new StopHitAction());
    final BreakpointManager manager = new BreakpointManager(slapProtocol, client, actions);

    final BreakpointEvent event = new BreakpointEvent(99, 3);
    manager.handleBreakpointEvent(event);

    final List<StoppedEventArguments> stoppedEvents = client.getStoppedEvents();
    assertThat(stoppedEvents).hasSize(1);
  }

  @Test
  void testConditionBreakpointLineIsBreakpointLine()
      throws IOException, InterruptedException, ExecutionException {
    final TestSlapProtocol slapProtocol = new TestSlapProtocol();
    final RecordingDebugProtocolClient client = new RecordingDebugProtocolClient();
    final BreakpointManager manager = new BreakpointManager(slapProtocol, client);
    final String[] filters = {"_self.taxonomy.includes?(:error)"};
    manager.setConditionBreakpoint(filters);

    final Set<Integer> lines = manager.getBreakpointLines("sw:condition.invoke()");

    assertThat(lines).containsExactly(0);
  }
}
