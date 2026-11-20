package nl.ramsolutions.sw.magik.debugadapter.breakpoints;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.debugadapter.slap.ErrorMessage;
import nl.ramsolutions.sw.magik.debugadapter.slap.ISlapProtocol;
import nl.ramsolutions.sw.magik.debugadapter.slap.ISlapResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.SlapErrorException;
import nl.ramsolutions.sw.magik.debugadapter.slap.events.BreakpointEvent;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.BreakpointSetResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.ErrorResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.EvalResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.ThreadStackResponse;
import nl.ramsolutions.sw.magik.parser.MagikParser;
import org.eclipse.lsp4j.debug.FunctionBreakpoint;
import org.eclipse.lsp4j.debug.Source;
import org.eclipse.lsp4j.debug.SourceBreakpoint;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Breakpoint manager. */
public class BreakpointManager {

  private static final String CONDITION_BREAKPOINT_METHOD = "sw:condition.invoke()";
  private static final String SW_TRUE = "True";

  /** Lets a step take part in the handling of a breakpoint hit. */
  public interface StopHook {

    /** A hook for a thread which is not stepping. */
    StopHook NONE =
        new StopHook() {
          @Override
          public void beforeStop() {
            // Nothing to do.
          }

          @Override
          public boolean endStepInsteadOfResume(final MagikBreakpoint magikBreakpoint) {
            return false;
          }
        };

    /** Run when the thread stays stopped, before the client hears of it. */
    void beforeStop();

    /**
     * Run instead of resuming a thread whose hit does not stop it.
     *
     * @param magikBreakpoint Breakpoint which was hit.
     * @return True if the thread is left to a step, which keeps it suspended.
     */
    boolean endStepInsteadOfResume(MagikBreakpoint magikBreakpoint);
  }

  /** Releases a location held by a stepper, before a breakpoint is set there. */
  public interface LocationReleaser {

    /**
     * Release a location.
     *
     * @param methodName Package-qualified method name.
     * @param line Line, 0 for the method's entry.
     * @throws IOException -
     * @throws InterruptedException -
     */
    void releaseLocation(String methodName, int line) throws IOException, InterruptedException;
  }

  /** Magik breakpoint. */
  public static class MagikBreakpoint {

    private volatile long breakpointId;
    private final String methodName;
    private final int methodLine;
    private volatile int sourceLine;
    private volatile String condition;
    private volatile String logMessage;
    private String message;

    /**
     * Constructor.
     *
     * @param methodName Name of method.
     * @param methodLine Line in method.
     */
    MagikBreakpoint(final String methodName, final int methodLine) {
      this.methodName = methodName;
      this.methodLine = methodLine;
      this.sourceLine = methodLine;
      this.condition = null;

      this.setBreakpointId(ISlapProtocol.INVALID_BREAKPOINT_ID);
    }

    /**
     * Constructor.
     *
     * @param methodName Name of method.
     * @param methodLine Line in method.
     * @param condition Condition.
     */
    MagikBreakpoint(final String methodName, final int methodLine, final String condition) {
      this.methodName = methodName;
      this.methodLine = methodLine;
      this.sourceLine = methodLine;
      this.condition = condition;

      this.setBreakpointId(ISlapProtocol.INVALID_BREAKPOINT_ID);
    }

    public String getMethodName() {
      return this.methodName;
    }

    int getMethodLine() {
      return this.methodLine;
    }

    /**
     * Get the line the client asked for, which differs from the method line on a method header.
     *
     * @return Line in the source file.
     */
    int getSourceLine() {
      return this.sourceLine;
    }

    void setSourceLine(final int sourceLine) {
      this.sourceLine = sourceLine;
    }

    @CheckForNull
    String getCondition() {
      return this.condition;
    }

    void setCondition(final @Nullable String condition) {
      this.condition = condition;
    }

    @CheckForNull
    String getLogMessage() {
      return this.logMessage;
    }

    void setLogMessage(final @Nullable String logMessage) {
      this.logMessage = logMessage;
    }

    void setMessage(final String message) {
      this.message = message;
    }

    public String getMessage() {
      return this.message;
    }

    public long getBreakpointId() {
      return this.breakpointId;
    }

    void setBreakpointId(long breakpointId) {
      this.breakpointId = breakpointId;
    }

    public boolean isVerified() {
      return this.breakpointId != ISlapProtocol.INVALID_BREAKPOINT_ID;
    }

    @Override
    public String toString() {
      return "%s@%s(%s, %s, %s)"
          .formatted(
              this.getClass().getName(),
              Integer.toHexString(this.hashCode()),
              this.getMethodName(),
              this.getMethodLine(),
              this.getBreakpointId());
    }
  }

  /**
   * The condition breakpoint and the exception filters selected with it, published together so a
   * hit sees the condition and the filters of one selection.
   *
   * @param breakpoint Condition breakpoint, or {@code null} if no filter is selected.
   * @param filters Selected exception filters.
   */
  private record ConditionStop(
      @Nullable MagikBreakpoint breakpoint, List<ExceptionFilter> filters) {

    static final ConditionStop NONE = new ConditionStop(null, List.of());
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(BreakpointManager.class);
  private static final StopHitAction FALLBACK_ACTION = new StopHitAction();
  private static final SourceLookup NO_SOURCE_LOOKUP =
      new SourceLookup() {
        @Override
        public Path findSource(final long threadId, final ThreadStackResponse.StackElement frame) {
          return null;
        }

        @Override
        public Path mapPath(final Path agentPath) {
          return agentPath;
        }
      };

  private final ISlapProtocol slapProtocol;
  private final IDebugProtocolClient debugClient;
  // Request threads and event handler threads use this manager at the same time.
  // `sourceBreakpoints` is guarded by `lock`, which is never held during a SLAP round trip.
  private final Object lock = new Object();
  private final Map<Source, List<MagikBreakpoint>> sourceBreakpoints = new HashMap<>();
  private volatile ConditionStop conditionStop = ConditionStop.NONE;
  private final Map<Long, MagikBreakpoint> breakpointIds = new ConcurrentHashMap<>();
  private volatile LocationReleaser locationReleaser = (methodName, line) -> {};
  private final List<HitAction> hitActions;
  private final List<ExceptionFilter> exceptionFilters;
  private final SourceLookup sourceLookup;

  /**
   * Constructor, for a manager which stops on every hit.
   *
   * @param slapProtocol SLAP protocol.
   * @param debugClient Client to send events to.
   */
  public BreakpointManager(
      final ISlapProtocol slapProtocol, final IDebugProtocolClient debugClient) {
    this(slapProtocol, debugClient, List.of(new StopHitAction()));
  }

  /**
   * Constructor.
   *
   * @param slapProtocol SLAP protocol.
   * @param debugClient Client to send events to.
   * @param hitActions Ordered actions which handle a hit whose condition holds.
   */
  public BreakpointManager(
      final ISlapProtocol slapProtocol,
      final IDebugProtocolClient debugClient,
      final List<HitAction> hitActions) {
    this(slapProtocol, debugClient, hitActions, List.of(new AnyErrorFilter()), NO_SOURCE_LOOKUP);
  }

  /**
   * Constructor.
   *
   * @param slapProtocol SLAP protocol.
   * @param debugClient Client to send events to.
   * @param hitActions Ordered actions which handle a hit whose condition holds.
   * @param exceptionFilters Ordered exception filters the client can select.
   * @param sourceLookup Finds the source file of a stack frame, for the exception filters.
   */
  public BreakpointManager(
      final ISlapProtocol slapProtocol,
      final IDebugProtocolClient debugClient,
      final List<HitAction> hitActions,
      final List<ExceptionFilter> exceptionFilters,
      final SourceLookup sourceLookup) {
    this.slapProtocol = slapProtocol;
    this.debugClient = debugClient;
    this.hitActions = List.copyOf(hitActions);
    this.exceptionFilters = List.copyOf(exceptionFilters);
    this.sourceLookup = sourceLookup;
  }

  /**
   * Set the releaser of locations held by a stepper.
   *
   * @param newLocationReleaser Location releaser.
   */
  public void setLocationReleaser(final LocationReleaser newLocationReleaser) {
    this.locationReleaser = newLocationReleaser;
  }

  /**
   * Set breakpoints in source.
   *
   * @param source Source for breakpoints.
   * @param newSourceBreakpoints Breakpoints.
   * @return Current breakpoints.
   * @throws IOException -
   * @throws ExecutionException -
   * @throws InterruptedException -
   */
  public List<MagikBreakpoint> setBreakpoints(
      final Source source, final SourceBreakpoint[] newSourceBreakpoints)
      throws IOException, InterruptedException, ExecutionException {
    final List<SourceBreakpoint> addedBreakpoints;
    final List<MagikBreakpoint> removedBreakpoints;
    synchronized (this.lock) {
      // Get all current breakpoints.
      final List<MagikBreakpoint> breakpoints =
          this.sourceBreakpoints.computeIfAbsent(source, key -> new ArrayList<>());

      // Find new breakpoints.
      final List<Integer> magikBreakpointLines =
          breakpoints.stream().map(MagikBreakpoint::getSourceLine).toList();
      addedBreakpoints =
          Arrays.stream(newSourceBreakpoints)
              .filter(breakpoint -> !magikBreakpointLines.contains(breakpoint.getLine()))
              .toList();

      // Find old breakpoints.
      final List<Integer> sourceBreakpointLines =
          Arrays.stream(newSourceBreakpoints).map(SourceBreakpoint::getLine).toList();
      removedBreakpoints =
          breakpoints.stream()
              .filter(
                  magikBreakpoint ->
                      !sourceBreakpointLines.contains(magikBreakpoint.getSourceLine()))
              .toList();

      this.updateKeptBreakpoints(breakpoints, newSourceBreakpoints);
    }

    // A moved breakpoint can map to the location it leaves, so that location is freed first.
    for (final MagikBreakpoint magikBreakpoint : removedBreakpoints) {
      this.removeBreakpoint(source, magikBreakpoint);
    }
    for (final SourceBreakpoint sourceBreakpoint : addedBreakpoints) {
      this.addBreakpoint(source, sourceBreakpoint);
    }

    // The registered breakpoints are updated through addBreakpoint/removeBreakpoint.
    synchronized (this.lock) {
      final List<MagikBreakpoint> breakpoints = this.sourceBreakpoints.get(source);
      return new ArrayList<>(breakpoints);
    }
  }

  /**
   * Update the condition and log message of the breakpoints which stay on their line.
   *
   * <p>Both live in the adapter, the agent never sees them, so nothing is sent to it.
   */
  private void updateKeptBreakpoints(
      final List<MagikBreakpoint> breakpoints, final SourceBreakpoint[] newSourceBreakpoints) {
    for (final SourceBreakpoint sourceBreakpoint : newSourceBreakpoints) {
      for (final MagikBreakpoint magikBreakpoint : breakpoints) {
        if (magikBreakpoint.getSourceLine() == sourceBreakpoint.getLine()) {
          magikBreakpoint.setCondition(sourceBreakpoint.getCondition());
          magikBreakpoint.setLogMessage(sourceBreakpoint.getLogMessage());
        }
      }
    }
  }

  /**
   * Add a new breakpoint to self and debugger.
   *
   * @param source Source.
   * @param sourceBreakpoint Source breakpoint.
   * @return New magik breakpoint.
   * @throws IOException -
   * @throws ExecutionException -
   * @throws InterruptedException -
   */
  MagikBreakpoint addBreakpoint(final Source source, final SourceBreakpoint sourceBreakpoint)
      throws IOException, InterruptedException, ExecutionException {
    final int sourceLine = sourceBreakpoint.getLine();
    int line = sourceLine;
    final AstNode methodNode = this.getNode(source, line);
    final String method;
    if (methodNode == null) {
      method = "<not_in_method>";
    } else {
      final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(methodNode);
      method = helper.getFullExemplarMethodName();
      final int methodLine = methodNode.getTokenLine();
      if (methodLine == line) {
        line = 0;
      }
    }
    final String condition = sourceBreakpoint.getCondition();
    final String logMessage = sourceBreakpoint.getLogMessage();
    return this.createBreakpoint(source, method, line, sourceLine, condition, logMessage);
  }

  /** Remove breakpoint. */
  void removeBreakpoint(final @Nullable Source source, final MagikBreakpoint magikBreakpoint)
      throws IOException, InterruptedException, ExecutionException {
    this.sendDeleteBreakpoint(magikBreakpoint);

    final long breakpointId = magikBreakpoint.getBreakpointId();
    this.breakpointIds.remove(breakpointId);

    synchronized (this.lock) {
      final List<MagikBreakpoint> breakpoints = this.sourceBreakpoints.get(source);
      if (breakpoints != null) {
        breakpoints.remove(magikBreakpoint);
      }
    }
  }

  // endregion

  // region: Function breakpoints
  /** Clear all function breakpoints. */
  public void clearFunctionBreakpoints()
      throws IOException, InterruptedException, ExecutionException {
    final Source source = null; // A function breakpoint has no source.

    // Iterate over a copy, removing a breakpoint changes the registered list.
    final List<MagikBreakpoint> magikBreakpoints;
    synchronized (this.lock) {
      final List<MagikBreakpoint> registered =
          this.sourceBreakpoints.computeIfAbsent(source, key -> new ArrayList<>());
      magikBreakpoints = new ArrayList<>(registered);
    }
    for (final MagikBreakpoint magikBreakpoint : magikBreakpoints) {
      this.removeBreakpoint(source, magikBreakpoint);
    }
  }

  /**
   * Add multiple function breakpoints.
   *
   * @param functionBreakpoints Breakpoints to be set.
   * @return List of {@link MagikBreakpoint}s.
   * @throws ExecutionException -
   * @throws InterruptedException -
   * @throws IOException -
   */
  public List<MagikBreakpoint> addFunctionBreakpoints(
      final FunctionBreakpoint[] functionBreakpoints)
      throws IOException, InterruptedException, ExecutionException {
    final List<MagikBreakpoint> breakpoints = new ArrayList<>();
    for (final FunctionBreakpoint functionBreakpoint : functionBreakpoints) {
      final MagikBreakpoint magikBreakpoint = this.addFunctionBreakpoint(functionBreakpoint);
      breakpoints.add(magikBreakpoint);
    }
    return breakpoints;
  }

  /**
   * Add a function breakpoint.
   *
   * @param functionBreakpoint {@link FunctionBreakpoint} to add.
   * @return Created {@link MagikBreakpoint}.
   * @throws IOException -
   * @throws ExecutionException -
   * @throws InterruptedException -
   */
  MagikBreakpoint addFunctionBreakpoint(final FunctionBreakpoint functionBreakpoint)
      throws IOException, InterruptedException, ExecutionException {
    final Source source = null;
    final String methodName = functionBreakpoint.getName();
    final int methodLine = 0;
    final String condition = functionBreakpoint.getCondition();
    return this.createBreakpoint(source, methodName, methodLine, methodLine, condition, null);
  }

  // endregion

  // region: Exception breakpoints
  /**
   * Clear the condition breakpoint.
   *
   * @return Old MagikBreapoint.
   * @throws IOException -
   * @throws ExecutionException -
   * @throws InterruptedException -
   */
  MagikBreakpoint clearConditionBreakpoint()
      throws IOException, InterruptedException, ExecutionException {
    final ConditionStop currentStop = this.conditionStop;
    final MagikBreakpoint currentConditionBreakpoint = currentStop.breakpoint();
    if (currentConditionBreakpoint != null) {
      final long breakpointId = currentConditionBreakpoint.getBreakpointId();
      final CompletableFuture<ISlapResponse> deleteFuture =
          this.slapProtocol.deleteBreakpoint(breakpointId);
      deleteFuture.get();
    }

    this.conditionStop = ConditionStop.NONE;
    return currentConditionBreakpoint;
  }

  /**
   * Set the condition breakpoint.
   *
   * @param filterIds IDs of the selected exception filters, unknown IDs are ignored.
   * @return Created MagikBreakpoint, or the old one when no filter is selected any more.
   * @throws ExecutionException -
   * @throws InterruptedException -
   * @throws IOException -
   */
  public MagikBreakpoint setConditionBreakpoint(final String[] filterIds)
      throws IOException, InterruptedException, ExecutionException {
    final List<String> selectedIds = Arrays.asList(filterIds);
    final List<ExceptionFilter> newSelectedFilters =
        this.exceptionFilters.stream()
            .filter(filter -> selectedIds.contains(filter.getId()))
            .toList();
    final ConditionStop currentStop = this.conditionStop;
    final MagikBreakpoint currentConditionBreakpoint = currentStop.breakpoint();
    if (newSelectedFilters.isEmpty()) {
      return currentConditionBreakpoint != null ? this.clearConditionBreakpoint() : null;
    }

    final String condition =
        newSelectedFilters.stream()
            .map(ExceptionFilter::getCondition)
            .collect(Collectors.joining(" _orif "));
    final MagikBreakpoint newConditionBreakpoint;
    if (currentConditionBreakpoint == null) {
      newConditionBreakpoint = this.sendSetBreakpoint(CONDITION_BREAKPOINT_METHOD, 0);
    } else {
      // A new instance for the same agent breakpoint keeps a hit from seeing a half-made selection.
      newConditionBreakpoint = new MagikBreakpoint(CONDITION_BREAKPOINT_METHOD, 0);
      final long breakpointId = currentConditionBreakpoint.getBreakpointId();
      newConditionBreakpoint.setBreakpointId(breakpointId);
      final String message = currentConditionBreakpoint.getMessage();
      newConditionBreakpoint.setMessage(message);
    }
    newConditionBreakpoint.setCondition(condition);
    this.conditionStop = new ConditionStop(newConditionBreakpoint, newSelectedFilters);
    return newConditionBreakpoint;
  }

  // endregion

  /**
   * Get a breakpoint by its ID.
   *
   * @param breakpointId Breakpoint ID.
   * @return Breakpoint for ID, or null if the ID is not known.
   */
  @CheckForNull
  MagikBreakpoint getBreakpoint(final long breakpointId) {
    final ConditionStop currentStop = this.conditionStop;
    return this.getBreakpoint(breakpointId, currentStop);
  }

  @CheckForNull
  private MagikBreakpoint getBreakpoint(final long breakpointId, final ConditionStop stop) {
    final MagikBreakpoint currentConditionBreakpoint = stop.breakpoint();
    if (currentConditionBreakpoint != null
        && currentConditionBreakpoint.getBreakpointId() == breakpointId) {
      return currentConditionBreakpoint;
    }

    return this.breakpointIds.get(breakpointId);
  }

  /**
   * Get the lines of a method which hold a verified breakpoint.
   *
   * @param methodName Package-qualified method name.
   * @return Lines, 0 stands for the method's entry.
   */
  public Set<Integer> getBreakpointLines(final String methodName) {
    final List<MagikBreakpoint> magikBreakpoints = new ArrayList<>();
    synchronized (this.lock) {
      final Collection<List<MagikBreakpoint>> breakpointLists = this.sourceBreakpoints.values();
      breakpointLists.forEach(magikBreakpoints::addAll);
    }
    final ConditionStop currentStop = this.conditionStop;
    final MagikBreakpoint currentConditionBreakpoint = currentStop.breakpoint();
    if (currentConditionBreakpoint != null) {
      magikBreakpoints.add(currentConditionBreakpoint);
    }

    return magikBreakpoints.stream()
        .filter(MagikBreakpoint::isVerified)
        .filter(magikBreakpoint -> this.isBreakpointInMethod(magikBreakpoint, methodName))
        .map(MagikBreakpoint::getMethodLine)
        .collect(Collectors.toSet());
  }

  private boolean isBreakpointInMethod(
      final MagikBreakpoint magikBreakpoint, final String methodName) {
    final String breakpointMethodName = magikBreakpoint.getMethodName();
    return breakpointMethodName.equals(methodName);
  }

  // region: Event handling
  /**
   * Handle a {@link BreakpointEvent}.
   *
   * @param breakpointEvent event.
   */
  public void handleBreakpointEvent(final BreakpointEvent breakpointEvent) {
    this.handleBreakpointEvent(breakpointEvent, StopHook.NONE);
  }

  /**
   * Handle a {@link BreakpointEvent}.
   *
   * @param breakpointEvent event.
   * @param stopHook Hook for a step of the thread.
   */
  public void handleBreakpointEvent(
      final BreakpointEvent breakpointEvent, final StopHook stopHook) {
    final long threadId = breakpointEvent.getThreadId();

    // If conditional breakpoint, then test condition and optionally continue.
    final long breakpointId = breakpointEvent.getBreakpointId();
    final ConditionStop stop = this.conditionStop;
    final MagikBreakpoint magikBreakpoint = this.getBreakpoint(breakpointId, stop);
    if (magikBreakpoint != null
        && this.shouldResumeHit(magikBreakpoint, threadId, stop)
        && this.resumeUnlessStepEnds(magikBreakpoint, threadId, stopHook)) {
      return;
    }

    final HitContext context =
        new HitContext(magikBreakpoint, threadId, stopHook, this.slapProtocol, this.debugClient);
    this.runHitActions(context);
  }

  // endregion

  // region: Internals
  private void runHitActions(final HitContext context) {
    for (final HitAction hitAction : this.hitActions) {
      if (this.isHandledBy(hitAction, context)) {
        return;
      }
    }

    // The thread must not stay suspended without the client hearing of it.
    final long threadId = context.threadId();
    LOGGER.warn("No hit action handled the hit, stopping the thread: {}", threadId);
    try {
      FALLBACK_ACTION.handle(context);
    } catch (final RuntimeException exception) {
      LOGGER.error("Could not report the stop of thread: {}", threadId, exception);
    }
  }

  private boolean isHandledBy(final HitAction hitAction, final HitContext context) {
    try {
      return hitAction.handle(context);
    } catch (final InterruptedException exception) {
      LOGGER.warn("Interrupted!", exception);
      Thread.currentThread().interrupt();
    } catch (final IOException | RuntimeException exception) {
      LOGGER.warn("Hit action failed: {}", hitAction, exception);
    }
    return false;
  }

  /** Test if the hit resumes: its condition is false, or no selected filter wants the stop. */
  private boolean shouldResumeHit(
      final MagikBreakpoint magikBreakpoint, final long threadId, final ConditionStop stop) {
    if (this.isConditionFalse(magikBreakpoint, threadId)) {
      return true;
    }

    final boolean isConditionBreakpoint = magikBreakpoint == stop.breakpoint();
    return isConditionBreakpoint && !this.isStopWanted(threadId, stop.filters());
  }

  private boolean isStopWanted(final long threadId, final List<ExceptionFilter> filters) {
    final FilterContext context = new FilterContext(threadId, this.slapProtocol, this.sourceLookup);
    for (final ExceptionFilter filter : filters) {
      if (this.isFilterStopping(filter, context)) {
        return true;
      }
    }

    return false;
  }

  private boolean isFilterStopping(final ExceptionFilter filter, final FilterContext context) {
    try {
      return filter.shouldStop(context);
    } catch (final InterruptedException exception) {
      LOGGER.warn("Interrupted!", exception);
      Thread.currentThread().interrupt();
    } catch (final ExecutionException | IOException | RuntimeException exception) {
      LOGGER.warn("Exception filter failed: {}", filter.getId(), exception);
    }
    // Stop if the verdict is unknown.
    return true;
  }

  private boolean isConditionFalse(final MagikBreakpoint magikBreakpoint, final long threadId) {
    final String condition = magikBreakpoint.getCondition();
    if (condition == null) {
      return false;
    }

    try {
      final CompletableFuture<ISlapResponse> evalFuture =
          this.slapProtocol.evaluate(threadId, 0, condition);
      final EvalResponse eval = (EvalResponse) evalFuture.get();
      final String result = eval.getResult();
      return !result.equals(SW_TRUE);
    } catch (final InterruptedException exception) {
      LOGGER.warn("Interrupted!", exception);
      Thread.currentThread().interrupt();
    } catch (final ExecutionException | IOException exception) {
      // Could not execute, pretend nothing happened.
    }
    return false;
  }

  /**
   * Resume a thread whose breakpoint condition is false, unless its step ends there.
   *
   * @return True if the thread was resumed or its step ended, false if it could not be resumed.
   */
  private boolean resumeUnlessStepEnds(
      final MagikBreakpoint magikBreakpoint, final long threadId, final StopHook stopHook) {
    if (stopHook.endStepInsteadOfResume(magikBreakpoint)) {
      return true;
    }

    try {
      final CompletableFuture<ISlapResponse> resumeFuture =
          this.slapProtocol.resumeThread(threadId);
      resumeFuture.get();
      return true;
    } catch (final InterruptedException exception) {
      LOGGER.warn("Interrupted!", exception);
      Thread.currentThread().interrupt();
    } catch (final ExecutionException | IOException exception) {
      LOGGER.debug("Could not resume thread: {}", threadId, exception);
    }
    return false;
  }

  private AstNode getNode(final Source source, final int line) {
    final Path path = Path.of(source.getPath());
    final MagikParser parser = new MagikParser();
    final AstNode node = parser.parseSafe(path);
    return AstQuery.nodeSurrounding(node, new Position(line, 0), MagikGrammar.METHOD_DEFINITION);
  }

  private MagikBreakpoint createBreakpoint(
      final @Nullable Source source,
      final String method,
      final int line,
      final int sourceLine,
      final @Nullable String condition,
      final @Nullable String logMessage)
      throws IOException, InterruptedException, ExecutionException {
    // Send breakpoint to debugger.
    final MagikBreakpoint magikBreakpoint = this.sendSetBreakpoint(method, line);
    magikBreakpoint.setSourceLine(sourceLine);
    magikBreakpoint.setCondition(condition);
    magikBreakpoint.setLogMessage(logMessage);

    // Register breakpoint id, if successful.
    final long breakpointId = magikBreakpoint.getBreakpointId();
    if (breakpointId != ISlapProtocol.INVALID_BREAKPOINT_ID) {
      this.breakpointIds.put(breakpointId, magikBreakpoint);
    }

    // Register breakpoint.
    synchronized (this.lock) {
      final List<MagikBreakpoint> breakpoints =
          this.sourceBreakpoints.computeIfAbsent(source, key -> new ArrayList<>());
      breakpoints.add(magikBreakpoint);
    }

    return magikBreakpoint;
  }

  private MagikBreakpoint sendSetBreakpoint(final String method, final int line)
      throws IOException, InterruptedException, ExecutionException {
    LOGGER.trace("Send set breakpoint: method: {}, line: {}", method, line);

    final MagikBreakpoint magikBreakpoint = new MagikBreakpoint(method, line);
    // A location which holds a breakpoint of a stepper refuses another one.
    this.locationReleaser.releaseLocation(method, line);

    try {
      final CompletableFuture<ISlapResponse> breakpointSetFuture =
          this.slapProtocol.setBreakpoint(method, line);
      final BreakpointSetResponse breakpointSet = (BreakpointSetResponse) breakpointSetFuture.get();
      final long breakpointId = breakpointSet.getBreakpointId();
      magikBreakpoint.setBreakpointId(breakpointId);

      LOGGER.trace("Created breakpoint: {}", magikBreakpoint);
    } catch (final ExecutionException exception) {
      final Throwable cause = exception.getCause();
      if (cause instanceof SlapErrorException slapErrorException) {
        // Do nothing, verified will become false, error will be shown to user.
        final ErrorResponse error = slapErrorException.getError();
        final ErrorMessage errorMessage = error.getErrorMessage();
        final String message = errorMessage.name();
        magikBreakpoint.setMessage(message);

        LOGGER.trace("Unable to create breakpoint: {}, reason: {}", magikBreakpoint, message);
      } else {
        throw exception;
      }
    }

    return magikBreakpoint;
  }

  private void sendDeleteBreakpoint(final MagikBreakpoint magikBreakpoint)
      throws IOException, InterruptedException, ExecutionException {
    LOGGER.trace(
        "Send delete breakpoint: method: {}, line: {}",
        magikBreakpoint.getMethodName(),
        magikBreakpoint.getMethodLine());

    final long breakpointId = magikBreakpoint.getBreakpointId();
    this.slapProtocol.deleteBreakpoint(breakpointId).get();

    LOGGER.trace("Deleted breakpoint: {}", magikBreakpoint);
  }
  // endregion

}
