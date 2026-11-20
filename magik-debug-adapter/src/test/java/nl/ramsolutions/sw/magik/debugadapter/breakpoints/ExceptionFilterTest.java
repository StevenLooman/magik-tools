package nl.ramsolutions.sw.magik.debugadapter.breakpoints;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import nl.ramsolutions.sw.magik.debugadapter.RecordingDebugProtocolClient;
import nl.ramsolutions.sw.magik.debugadapter.TestSlapProtocol;
import nl.ramsolutions.sw.magik.debugadapter.breakpoints.BreakpointManager.MagikBreakpoint;
import nl.ramsolutions.sw.magik.debugadapter.breakpoints.BreakpointManager.StopHook;
import nl.ramsolutions.sw.magik.debugadapter.slap.ISlapProtocol;
import nl.ramsolutions.sw.magik.debugadapter.slap.ISlapResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.events.BreakpointEvent;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.BreakpointModifyResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.EvalResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.ThreadStackResponse;
import org.eclipse.lsp4j.debug.FunctionBreakpoint;
import org.eclipse.lsp4j.debug.StoppedEventArguments;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for {@link ExceptionFilter} and its use by {@link BreakpointManager}. */
@SuppressWarnings("checkstyle:MagicNumber")
class ExceptionFilterTest {

  private static final long THREAD_ID = 3;
  private static final String ANY_ID = "_self.taxonomy.includes?(:error)";

  /** Filter which records that it was asked, and answers a fixed verdict. */
  private static class VerdictFilter implements ExceptionFilter {

    private final String id;
    private final boolean verdict;
    private final List<String> askedIds;

    VerdictFilter(final String id, final boolean verdict, final List<String> askedIds) {
      this.id = id;
      this.verdict = verdict;
      this.askedIds = askedIds;
    }

    @Override
    public String getId() {
      return this.id;
    }

    @Override
    public String getLabel() {
      return this.id;
    }

    @Override
    public String getCondition() {
      return "cond_" + this.id;
    }

    @Override
    public boolean shouldStop(final FilterContext context) throws IOException {
      this.askedIds.add(this.id);
      return this.verdict;
    }
  }

  /** Source lookup which knows no source. */
  private static final class NoSourceLookup implements SourceLookup {

    @Override
    public Path findSource(final long threadId, final ThreadStackResponse.StackElement frame) {
      return null;
    }

    @Override
    public Path mapPath(final Path agentPath) {
      return agentPath;
    }
  }

  private final List<Long> resumedThreadIds = new ArrayList<>();
  private final List<String> evaluatedExpressions = new ArrayList<>();
  private final List<String> askedIds = new ArrayList<>();
  private final RecordingDebugProtocolClient client = new RecordingDebugProtocolClient();
  private final SourceLookup sourceLookup = new NoSourceLookup();
  private TestSlapProtocol slapProtocol;

  @BeforeEach
  void createSlapProtocol() {
    this.slapProtocol =
        new TestSlapProtocol() {
          @Override
          public CompletableFuture<ISlapResponse> evaluate(
              final long threadId, final int level, final String expression) {
            ExceptionFilterTest.this.evaluatedExpressions.add(expression);
            final ISlapResponse response = new EvalResponse("True");
            return CompletableFuture.completedFuture(response);
          }

          @Override
          public CompletableFuture<ISlapResponse> resumeThread(final long threadId) {
            ExceptionFilterTest.this.resumedThreadIds.add(threadId);
            final ISlapResponse response = new BreakpointModifyResponse();
            return CompletableFuture.completedFuture(response);
          }
        };
  }

  private VerdictFilter createVerdictFilter(final String id, final boolean verdict) {
    return new VerdictFilter(id, verdict, this.askedIds);
  }

  private BreakpointManager createManager(final List<ExceptionFilter> filters) {
    final List<HitAction> actions = List.of(new StopHitAction());
    return new BreakpointManager(
        this.slapProtocol, this.client, actions, filters, this.sourceLookup);
  }

  private long selectFilters(final BreakpointManager manager, final String... filterIds)
      throws IOException, InterruptedException, ExecutionException {
    final MagikBreakpoint conditionBreakpoint = manager.setConditionBreakpoint(filterIds);
    return conditionBreakpoint.getBreakpointId();
  }

  @Test
  void testAnyErrorFilterValues() {
    final AnyErrorFilter filter = new AnyErrorFilter();

    final String id = filter.getId();
    assertThat(id).isEqualTo(ANY_ID);
    final String label = filter.getLabel();
    assertThat(label).isEqualTo("Any error condition");
    final String condition = filter.getCondition();
    assertThat(condition).isEqualTo(ANY_ID);
  }

  @Test
  void testFirstTrueVerdictStops() throws IOException, InterruptedException, ExecutionException {
    final List<ExceptionFilter> filters =
        List.of(
            this.createVerdictFilter("a", false),
            this.createVerdictFilter("b", true),
            this.createVerdictFilter("c", true));
    final BreakpointManager manager = this.createManager(filters);
    final long breakpointId = this.selectFilters(manager, "a", "b", "c");

    final BreakpointEvent event = new BreakpointEvent(breakpointId, THREAD_ID);
    manager.handleBreakpointEvent(event);

    assertThat(this.askedIds).containsExactly("a", "b");
    final List<StoppedEventArguments> stoppedEvents = this.client.getStoppedEvents();
    assertThat(stoppedEvents).hasSize(1);
    assertThat(this.resumedThreadIds).isEmpty();
  }

  @Test
  void testAllFalseVerdictsResume() throws IOException, InterruptedException, ExecutionException {
    final List<ExceptionFilter> filters =
        List.of(this.createVerdictFilter("a", false), this.createVerdictFilter("b", false));
    final BreakpointManager manager = this.createManager(filters);
    final long breakpointId = this.selectFilters(manager, "a", "b");

    final BreakpointEvent event = new BreakpointEvent(breakpointId, THREAD_ID);
    manager.handleBreakpointEvent(event);

    assertThat(this.askedIds).containsExactly("a", "b");
    final List<StoppedEventArguments> stoppedEvents = this.client.getStoppedEvents();
    assertThat(stoppedEvents).isEmpty();
    assertThat(this.resumedThreadIds).containsExactly(THREAD_ID);
  }

  @Test
  void testAnyErrorFilterNeverWalksTheStack()
      throws IOException, InterruptedException, ExecutionException {
    final List<ExceptionFilter> filters =
        List.of(new AnyErrorFilter(), this.createVerdictFilter("walker", false));
    final BreakpointManager manager = this.createManager(filters);
    final long breakpointId = this.selectFilters(manager, "walker", ANY_ID);

    final BreakpointEvent event = new BreakpointEvent(breakpointId, THREAD_ID);
    manager.handleBreakpointEvent(event);

    assertThat(this.askedIds).isEmpty();
    final List<StoppedEventArguments> stoppedEvents = this.client.getStoppedEvents();
    assertThat(stoppedEvents).hasSize(1);
  }

  @Test
  void testVerdictsAreAskedInFactoryOrder()
      throws IOException, InterruptedException, ExecutionException {
    final List<ExceptionFilter> filters =
        List.of(this.createVerdictFilter("a", false), this.createVerdictFilter("b", false));
    final BreakpointManager manager = this.createManager(filters);
    final long breakpointId = this.selectFilters(manager, "b", "a");

    final BreakpointEvent event = new BreakpointEvent(breakpointId, THREAD_ID);
    manager.handleBreakpointEvent(event);

    assertThat(this.askedIds).containsExactly("a", "b");
  }

  @Test
  void testUnselectedFilterIsNotAsked()
      throws IOException, InterruptedException, ExecutionException {
    final List<ExceptionFilter> filters =
        List.of(this.createVerdictFilter("a", false), this.createVerdictFilter("b", false));
    final BreakpointManager manager = this.createManager(filters);
    final long breakpointId = this.selectFilters(manager, "b");

    final BreakpointEvent event = new BreakpointEvent(breakpointId, THREAD_ID);
    manager.handleBreakpointEvent(event);

    assertThat(this.askedIds).containsExactly("b");
  }

  @Test
  void testConditionIsTheOrifJoinOfTheSelectedConditions()
      throws IOException, InterruptedException, ExecutionException {
    final List<ExceptionFilter> filters =
        List.of(
            this.createVerdictFilter("a", true),
            this.createVerdictFilter("b", true),
            this.createVerdictFilter("c", true));
    final BreakpointManager manager = this.createManager(filters);
    final long breakpointId = this.selectFilters(manager, "c", "a");

    final BreakpointEvent event = new BreakpointEvent(breakpointId, THREAD_ID);
    manager.handleBreakpointEvent(event);

    assertThat(this.evaluatedExpressions).containsExactly("cond_a _orif cond_c");
  }

  @Test
  void testReselectionReplacesTheConditionAndTheFilters()
      throws IOException, InterruptedException, ExecutionException {
    final List<ExceptionFilter> filters =
        List.of(this.createVerdictFilter("a", true), this.createVerdictFilter("b", false));
    final BreakpointManager manager = this.createManager(filters);
    final long firstId = this.selectFilters(manager, "a");
    final long secondId = this.selectFilters(manager, "b");

    final BreakpointEvent event = new BreakpointEvent(secondId, THREAD_ID);
    manager.handleBreakpointEvent(event);

    assertThat(secondId).isEqualTo(firstId);
    assertThat(this.evaluatedExpressions).containsExactly("cond_b");
    assertThat(this.askedIds).containsExactly("b");
    assertThat(this.resumedThreadIds).containsExactly(THREAD_ID);
  }

  @Test
  void testDuplicateIdSelectsTheFilterOnce()
      throws IOException, InterruptedException, ExecutionException {
    final List<ExceptionFilter> filters = List.of(this.createVerdictFilter("a", false));
    final BreakpointManager manager = this.createManager(filters);
    final long breakpointId = this.selectFilters(manager, "a", "a");

    final BreakpointEvent event = new BreakpointEvent(breakpointId, THREAD_ID);
    manager.handleBreakpointEvent(event);

    assertThat(this.evaluatedExpressions).containsExactly("cond_a");
    assertThat(this.askedIds).containsExactly("a");
  }

  @Test
  void testVerdictThrowingRuntimeExceptionStops()
      throws IOException, InterruptedException, ExecutionException {
    final ExceptionFilter throwingFilter =
        new VerdictFilter("a", false, this.askedIds) {
          @Override
          public boolean shouldStop(final FilterContext context) {
            throw new IllegalStateException("failed");
          }
        };
    final BreakpointManager manager = this.createManager(List.of(throwingFilter));
    final long breakpointId = this.selectFilters(manager, "a");

    final BreakpointEvent event = new BreakpointEvent(breakpointId, THREAD_ID);
    manager.handleBreakpointEvent(event);

    final List<StoppedEventArguments> stoppedEvents = this.client.getStoppedEvents();
    assertThat(stoppedEvents).hasSize(1);
    assertThat(this.resumedThreadIds).isEmpty();
  }

  @Test
  void testUnknownFilterIsNotSelected()
      throws IOException, InterruptedException, ExecutionException {
    final List<ExceptionFilter> filters = List.of(this.createVerdictFilter("a", true));
    final BreakpointManager manager = this.createManager(filters);

    final String[] unknownIds = {"unknown"};
    final MagikBreakpoint conditionBreakpoint = manager.setConditionBreakpoint(unknownIds);

    assertThat(conditionBreakpoint).isNull();
  }

  @Test
  void testFalseVerdictLetsAnEndingStepKeepTheThreadStopped()
      throws IOException, InterruptedException, ExecutionException {
    final List<ExceptionFilter> filters = List.of(this.createVerdictFilter("a", false));
    final BreakpointManager manager = this.createManager(filters);
    final long breakpointId = this.selectFilters(manager, "a");
    final List<MagikBreakpoint> hookBreakpoints = new ArrayList<>();
    final StopHook endingHook =
        new StopHook() {
          @Override
          public void beforeStop() {
            // Nothing to do.
          }

          @Override
          public boolean endStepInsteadOfResume(final MagikBreakpoint magikBreakpoint) {
            hookBreakpoints.add(magikBreakpoint);
            return true;
          }
        };

    final BreakpointEvent event = new BreakpointEvent(breakpointId, THREAD_ID);
    manager.handleBreakpointEvent(event, endingHook);

    assertThat(hookBreakpoints).hasSize(1);
    assertThat(this.resumedThreadIds).isEmpty();
    final List<StoppedEventArguments> stoppedEvents = this.client.getStoppedEvents();
    assertThat(stoppedEvents).isEmpty();
  }

  @Test
  void testVerdictRunsBeforeTheHitActions()
      throws IOException, InterruptedException, ExecutionException {
    final List<HitContext> seenContexts = new ArrayList<>();
    final HitAction recordingAction =
        context -> {
          seenContexts.add(context);
          return false;
        };
    final List<HitAction> actions = List.of(recordingAction, new StopHitAction());
    final List<ExceptionFilter> filters = List.of(this.createVerdictFilter("a", false));
    final BreakpointManager manager =
        new BreakpointManager(this.slapProtocol, this.client, actions, filters, this.sourceLookup);
    final long breakpointId = this.selectFilters(manager, "a");

    final BreakpointEvent event = new BreakpointEvent(breakpointId, THREAD_ID);
    manager.handleBreakpointEvent(event);

    assertThat(seenContexts).isEmpty();
  }

  @Test
  void testFailingVerdictStops() throws IOException, InterruptedException, ExecutionException {
    final ExceptionFilter failingFilter =
        new VerdictFilter("a", false, this.askedIds) {
          @Override
          public boolean shouldStop(final FilterContext context) throws IOException {
            throw new IOException("failed");
          }
        };
    final BreakpointManager manager = this.createManager(List.of(failingFilter));
    final long breakpointId = this.selectFilters(manager, "a");

    final BreakpointEvent event = new BreakpointEvent(breakpointId, THREAD_ID);
    manager.handleBreakpointEvent(event);

    final List<StoppedEventArguments> stoppedEvents = this.client.getStoppedEvents();
    assertThat(stoppedEvents).hasSize(1);
    assertThat(this.resumedThreadIds).isEmpty();
  }

  @Test
  void testVerdictGetsTheThreadAndTheSourceLookup()
      throws IOException, InterruptedException, ExecutionException {
    final List<FilterContext> seenContexts = new ArrayList<>();
    final ExceptionFilter recordingFilter =
        new VerdictFilter("a", true, this.askedIds) {
          @Override
          public boolean shouldStop(final FilterContext context) throws IOException {
            seenContexts.add(context);
            return true;
          }
        };
    final BreakpointManager manager = this.createManager(List.of(recordingFilter));
    final long breakpointId = this.selectFilters(manager, "a");

    final BreakpointEvent event = new BreakpointEvent(breakpointId, THREAD_ID);
    manager.handleBreakpointEvent(event);

    assertThat(seenContexts).hasSize(1);
    final FilterContext context = seenContexts.get(0);
    final long contextThreadId = context.threadId();
    assertThat(contextThreadId).isEqualTo(THREAD_ID);
    final SourceLookup contextLookup = context.sourceLookup();
    assertThat(contextLookup).isSameAs(this.sourceLookup);
    final ISlapProtocol contextProtocol = context.slapProtocol();
    assertThat(contextProtocol).isSameAs(this.slapProtocol);
  }

  @Test
  void testUserBreakpointHitIgnoresTheFilters()
      throws IOException, InterruptedException, ExecutionException {
    final List<ExceptionFilter> filters = List.of(this.createVerdictFilter("a", false));
    final BreakpointManager manager = this.createManager(filters);
    this.selectFilters(manager, "a");
    final FunctionBreakpoint functionBreakpoint = new FunctionBreakpoint();
    functionBreakpoint.setName("user:a.m1()");
    final MagikBreakpoint userBreakpoint = manager.addFunctionBreakpoint(functionBreakpoint);

    final long userBreakpointId = userBreakpoint.getBreakpointId();
    final BreakpointEvent event = new BreakpointEvent(userBreakpointId, THREAD_ID);
    manager.handleBreakpointEvent(event);

    assertThat(this.askedIds).isEmpty();
    final List<StoppedEventArguments> stoppedEvents = this.client.getStoppedEvents();
    assertThat(stoppedEvents).hasSize(1);
  }
}
