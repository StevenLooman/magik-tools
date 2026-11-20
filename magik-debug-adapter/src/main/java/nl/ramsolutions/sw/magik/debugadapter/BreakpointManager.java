package nl.ramsolutions.sw.magik.debugadapter;

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
import nl.ramsolutions.sw.magik.parser.MagikParser;
import org.eclipse.lsp4j.debug.ExceptionBreakpointsFilter;
import org.eclipse.lsp4j.debug.FunctionBreakpoint;
import org.eclipse.lsp4j.debug.Source;
import org.eclipse.lsp4j.debug.SourceBreakpoint;
import org.eclipse.lsp4j.debug.StoppedEventArguments;
import org.eclipse.lsp4j.debug.StoppedEventArgumentsReason;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Breakpoint manager. */
class BreakpointManager {

  static final ExceptionBreakpointsFilter[] EXCEPTION_BREAKPOINTS_FILTERS;
  private static final String CONDITION_BREAKPOINT_METHOD = "sw:condition.invoke()";
  private static final String SW_TRUE = "True";

  /** Lets a step take part in the handling of a user breakpoint hit. */
  interface StopHook {

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
     * Run instead of resuming a thread whose breakpoint condition is false.
     *
     * @param magikBreakpoint Breakpoint which was hit.
     * @return True if a step ended there, which keeps the thread stopped.
     */
    boolean endStepInsteadOfResume(MagikBreakpoint magikBreakpoint);
  }

  /** Releases a location held by a temporary breakpoint, before a user breakpoint is set there. */
  interface LocationReleaser {

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
  static class MagikBreakpoint {

    private volatile long breakpointId;
    private final String methodName;
    private final int methodLine;
    private volatile String condition;
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
      this.condition = condition;

      this.setBreakpointId(ISlapProtocol.INVALID_BREAKPOINT_ID);
    }

    String getMethodName() {
      return this.methodName;
    }

    int getMethodLine() {
      return this.methodLine;
    }

    @CheckForNull
    String getCondition() {
      return this.condition;
    }

    void setCondition(final @Nullable String condition) {
      this.condition = condition;
    }

    void setMessage(final String message) {
      this.message = message;
    }

    String getMessage() {
      return this.message;
    }

    long getBreakpointId() {
      return this.breakpointId;
    }

    void setBreakpointId(long breakpointId) {
      this.breakpointId = breakpointId;
    }

    boolean isVerified() {
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

  static {
    final ExceptionBreakpointsFilter errorFilter = new ExceptionBreakpointsFilter();
    errorFilter.setLabel("Any error condition");
    errorFilter.setFilter("_self.taxonomy.includes?(:error)");

    EXCEPTION_BREAKPOINTS_FILTERS = new ExceptionBreakpointsFilter[] {errorFilter};
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(BreakpointManager.class);

  private final ISlapProtocol slapProtocol;
  private final IDebugProtocolClient debugClient;
  // Request threads and event handler threads use this manager at the same time.
  // `sourceBreakpoints` is guarded by `lock`, which is never held during a SLAP round trip.
  private final Object lock = new Object();
  private final Map<Source, List<MagikBreakpoint>> sourceBreakpoints = new HashMap<>();
  private volatile MagikBreakpoint conditionBreakpoint;
  private final Map<Long, MagikBreakpoint> breakpointIds = new ConcurrentHashMap<>();
  private volatile LocationReleaser locationReleaser = (methodName, line) -> {};

  BreakpointManager(final ISlapProtocol slapProtocol, final IDebugProtocolClient debugClient) {
    this.slapProtocol = slapProtocol;
    this.debugClient = debugClient;
  }

  /**
   * Set the releaser of locations held by temporary breakpoints.
   *
   * @param newLocationReleaser Location releaser.
   */
  void setLocationReleaser(final LocationReleaser newLocationReleaser) {
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
          breakpoints.stream().map(MagikBreakpoint::getMethodLine).toList();
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
                      !sourceBreakpointLines.contains(magikBreakpoint.getMethodLine()))
              .toList();
    }

    for (final SourceBreakpoint sourceBreakpoint : addedBreakpoints) {
      this.addBreakpoint(source, sourceBreakpoint);
    }
    for (final MagikBreakpoint magikBreakpoint : removedBreakpoints) {
      this.removeBreakpoint(source, magikBreakpoint);
    }

    // The registered breakpoints are updated through addBreakpoint/removeBreakpoint.
    synchronized (this.lock) {
      final List<MagikBreakpoint> breakpoints = this.sourceBreakpoints.get(source);
      return new ArrayList<>(breakpoints);
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
    int line = sourceBreakpoint.getLine();
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
    return this.createBreakpoint(source, method, line, condition);
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
  void clearFunctionBreakpoints() throws IOException, InterruptedException, ExecutionException {
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
  List<MagikBreakpoint> addFunctionBreakpoints(final FunctionBreakpoint[] functionBreakpoints)
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
    return this.createBreakpoint(source, methodName, methodLine, condition);
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
    final MagikBreakpoint currentConditionBreakpoint = this.conditionBreakpoint;
    if (currentConditionBreakpoint != null) {
      final long breakpointId = currentConditionBreakpoint.getBreakpointId();
      this.slapProtocol.deleteBreakpoint(breakpointId).get();
    }

    this.conditionBreakpoint = null;
    return currentConditionBreakpoint;
  }

  /**
   * Set the condition breakpoint.
   *
   * @param filters Filters.
   * @return Created MagikBreakpoint.
   * @throws ExecutionException -
   * @throws InterruptedException -
   * @throws IOException -
   */
  MagikBreakpoint setConditionBreakpoint(final String[] filters)
      throws IOException, InterruptedException, ExecutionException {
    final MagikBreakpoint currentConditionBreakpoint = this.conditionBreakpoint;
    if (currentConditionBreakpoint != null && filters.length == 0) {
      return this.clearConditionBreakpoint();
    }

    final String condition;
    if (filters.length != 0) {
      condition = Arrays.stream(filters).collect(Collectors.joining(" _orif "));
    } else {
      condition = null;
    }

    // Ensure there is a breakpoint, with its condition set before it is published.
    if (currentConditionBreakpoint == null && filters.length != 0) {
      final MagikBreakpoint newConditionBreakpoint =
          this.sendSetBreakpoint(CONDITION_BREAKPOINT_METHOD, 0);
      newConditionBreakpoint.setCondition(condition);
      this.conditionBreakpoint = newConditionBreakpoint;
      return newConditionBreakpoint;
    }

    if (currentConditionBreakpoint != null) {
      currentConditionBreakpoint.setCondition(condition);
    }
    return currentConditionBreakpoint;
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
    final MagikBreakpoint currentConditionBreakpoint = this.conditionBreakpoint;
    if (currentConditionBreakpoint != null
        && currentConditionBreakpoint.getBreakpointId() == breakpointId) {
      return currentConditionBreakpoint;
    }

    return this.breakpointIds.get(breakpointId);
  }

  /**
   * Get the lines of a method which hold a verified user breakpoint.
   *
   * @param methodName Package-qualified method name.
   * @return Lines, 0 stands for the method's entry.
   */
  Set<Integer> getBreakpointLines(final String methodName) {
    final List<MagikBreakpoint> magikBreakpoints = new ArrayList<>();
    synchronized (this.lock) {
      final Collection<List<MagikBreakpoint>> breakpointLists = this.sourceBreakpoints.values();
      breakpointLists.forEach(magikBreakpoints::addAll);
    }
    final MagikBreakpoint currentConditionBreakpoint = this.conditionBreakpoint;
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
  void handleBreakpointEvent(final BreakpointEvent breakpointEvent) {
    this.handleBreakpointEvent(breakpointEvent, StopHook.NONE);
  }

  /**
   * Handle a {@link BreakpointEvent}.
   *
   * @param breakpointEvent event.
   * @param stopHook Hook for a step of the thread.
   */
  void handleBreakpointEvent(final BreakpointEvent breakpointEvent, final StopHook stopHook) {
    final long threadId = breakpointEvent.getThreadId();

    // If conditional breakpoint, then test condition and optionally continue.
    final long breakpointId = breakpointEvent.getBreakpointId();
    final MagikBreakpoint magikBreakpoint = this.getBreakpoint(breakpointId);
    if (magikBreakpoint != null
        && this.isConditionFalse(magikBreakpoint, threadId)
        && this.resumeUnlessStepEnds(magikBreakpoint, threadId, stopHook)) {
      return;
    }

    stopHook.beforeStop();
    final StoppedEventArguments args = new StoppedEventArguments();
    args.setThreadId((int) threadId);
    args.setReason(StoppedEventArgumentsReason.BREAKPOINT);
    this.debugClient.stopped(args);
  }

  // endregion

  // region: Internals
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
      final @Nullable String condition)
      throws IOException, InterruptedException, ExecutionException {
    // Send breakpoint to debugger.
    final MagikBreakpoint magikBreakpoint = this.sendSetBreakpoint(method, line);
    magikBreakpoint.setCondition(condition);

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
    // A location which holds a temporary breakpoint refuses another one.
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
