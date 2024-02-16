package nl.ramsolutions.sw.magik.debugadapter;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.debugadapter.slap.ISlapProtocol;
import nl.ramsolutions.sw.magik.debugadapter.slap.ISlapResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.SlapErrorException;
import nl.ramsolutions.sw.magik.debugadapter.slap.events.BreakpointEvent;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.BreakpointSetResponse;
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

/**
 * Breakpoint manager.
 */
class BreakpointManager {

    static final ExceptionBreakpointsFilter[] EXCEPTION_BREAKPOINTS_FILTERS;
    private static final String CONDITION_BREAKPOINT_METHOD = "sw:condition.invoke()";
    private static final String SW_TRUE = "True";

    /**
     * Magik breakpoint.
     */
    static class MagikBreakpoint {

        private long breakpointId;
        private final String methodName;
        private final int methodLine;
        private String condition;
        private String message;

        /**
         * Contructor.
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
         * Contructor.
         *
         * @param methodName Name of method.
         * @param methodLine Line in method.
         * @param condition    Condition.
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
            return String.format(
                "%s@%s(%s, %s, %s)",
                this.getClass().getName(), Integer.toHexString(this.hashCode()),
                this.getMethodName(), this.getMethodLine(), this.getBreakpointId());
        }

    }

    static {
        final ExceptionBreakpointsFilter errorFilter = new ExceptionBreakpointsFilter();
        errorFilter.setLabel("Any error condition");
        errorFilter.setFilter("_self.taxonomy.includes?(:error)");

        EXCEPTION_BREAKPOINTS_FILTERS = new ExceptionBreakpointsFilter[]{
            errorFilter
        };
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(BreakpointManager.class);

    private final ISlapProtocol slapProtocol;
    private final IDebugProtocolClient debugClient;
    private final Map<Source, List<MagikBreakpoint>> sourceBreakpoints = new HashMap<>();
    private MagikBreakpoint conditionBreakpoint;
    private final Map<Long, MagikBreakpoint> breakpointIds = new HashMap<>();

    BreakpointManager(final ISlapProtocol slapProtocol, final IDebugProtocolClient debugClient) {
        this.slapProtocol = slapProtocol;
        this.debugClient = debugClient;
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
    public List<MagikBreakpoint> setBreakpoints(final Source source, final SourceBreakpoint[] newSourceBreakpoints)
            throws IOException, InterruptedException, ExecutionException {
        // Get all current breakpoints.
        final List<MagikBreakpoint> breakpoints =
            this.sourceBreakpoints.computeIfAbsent(source, key -> new ArrayList<>());

        // Add new breakpoints.
        final List<Integer> magikBreakpointLines = breakpoints.stream()
            .map(MagikBreakpoint::getMethodLine)
            .collect(Collectors.toList());
        final List<SourceBreakpoint> addedBreakpoints = Arrays.stream(newSourceBreakpoints)
            .filter(breakpoint -> !magikBreakpointLines.contains(breakpoint.getLine()))
            .collect(Collectors.toList());
        for (final SourceBreakpoint sourceBreakpoint : addedBreakpoints) {
            this.addBreakpoint(source, sourceBreakpoint);
        }

        // Remove old breakpoints.
        final List<Integer> sourceBreakpointLines = Arrays.stream(newSourceBreakpoints)
            .map(SourceBreakpoint::getLine)
            .collect(Collectors.toList());
        final List<MagikBreakpoint> removedBreakpoints = breakpoints.stream()
            .filter(magikBreakpoint -> !sourceBreakpointLines.contains(magikBreakpoint.getMethodLine()))
            .collect(Collectors.toList());
        for (final MagikBreakpoint magikBreakpoint : removedBreakpoints) {
            this.removeBreakpoint(source, magikBreakpoint);
        }

        // breakpoints gets updated through addBreakpoint/removeBreakpoint.
        return breakpoints;
    }

    /**
     * Add a new breakpoint to self and debugger.
     *
     * @param source                     Source.
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

    /**
     * Remove breakpoint.
     */
    void removeBreakpoint(final @Nullable Source source, final MagikBreakpoint magikBreakpoint)
            throws IOException, InterruptedException, ExecutionException {
        this.sendDeleteBreakpoint(magikBreakpoint);

        final long breakpointId = magikBreakpoint.getBreakpointId();
        this.breakpointIds.remove(breakpointId);

        final List<MagikBreakpoint> breakpoints =
            this.sourceBreakpoints.computeIfAbsent(source, key -> new ArrayList<>());
        breakpoints.remove(magikBreakpoint);
    }
    // endregion

    // region: Function breakpoints
    /**
     * Clear all function breakpoints.
     */
    void clearFunctionBreakpoints() throws IOException, InterruptedException, ExecutionException {
        final Source source = null;    // A function breakpoint has no source.

        final List<MagikBreakpoint> magikBreakpoints =
            this.sourceBreakpoints.computeIfAbsent(source, key -> new ArrayList<>());
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
    MagikBreakpoint clearConditionBreakpoint() throws IOException, InterruptedException, ExecutionException {
        if (this.conditionBreakpoint != null) {
            final long breakpointId = this.conditionBreakpoint.getBreakpointId();
            this.slapProtocol.deleteBreakpoint(breakpointId).get();
        }

        final MagikBreakpoint currentConditionBreakpoint = this.conditionBreakpoint;
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
        if (this.conditionBreakpoint != null
            && filters.length == 0) {
            return this.clearConditionBreakpoint();
        }

        // Ensure there is a breakpoint.
        if (this.conditionBreakpoint == null
            && filters.length != 0) {
            this.conditionBreakpoint = this.sendSetBreakpoint(CONDITION_BREAKPOINT_METHOD, 0);
        }

        // Set condition for breakpoint.
        if (this.conditionBreakpoint != null) {
            if (filters.length != 0) {
                final String condition = Arrays.stream(filters)
                    .collect(Collectors.joining(" _orif "));
                this.conditionBreakpoint.setCondition(condition);
            } else {
                this.conditionBreakpoint.setCondition(null);
            }
        }

        return this.conditionBreakpoint;
    }
    // endregion

    /**
     * Get a breakpoint by its ID.
     * @param breakpointId Breakpoint ID.
     * @return Breakpoint for ID.
     */
    MagikBreakpoint getBreakpoint(final long breakpointId) {
        if (this.conditionBreakpoint != null
            && this.conditionBreakpoint.getBreakpointId() == breakpointId) {
            return this.conditionBreakpoint;
        }

        return this.breakpointIds.get(breakpointId);
    }

    // region: Event handling
    /**
     * Handle a {@link BreakpointEvent}.
     * @param breakpointEvent event.
     */
    void handleBreakpointEvent(final BreakpointEvent breakpointEvent) {
        final long threadId = breakpointEvent.getThreadId();

        // If conditional breakpoint, then test condition and optionally continue.
        final long breakpointId = breakpointEvent.getBreakpointId();
        final MagikBreakpoint magikBreakpoint = this.getBreakpoint(breakpointId);
        final String condition = magikBreakpoint.getCondition();
        if (condition != null) {
            try {
                final EvalResponse eval = (EvalResponse) this.slapProtocol.evaluate(threadId, 0, condition).get();
                final String result = eval.getResult();
                if (!result.equals(SW_TRUE)) {
                    this.slapProtocol.resumeThread(threadId).get();
                    return;
                }
            } catch (InterruptedException exception) {
                LOGGER.warn("Interrupted!", exception);
                Thread.currentThread().interrupt();
            } catch (ExecutionException | IOException exception) {
                // Could not execute, pretend nothing happened.
            }
        }

        final StoppedEventArguments args = new StoppedEventArguments();
        args.setThreadId((int) threadId);
        args.setReason(StoppedEventArgumentsReason.BREAKPOINT);
        this.debugClient.stopped(args);
    }
    // endergion

    // region: Internals
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
        final List<MagikBreakpoint> breakpoints =
            this.sourceBreakpoints.computeIfAbsent(source, key -> new ArrayList<>());
        breakpoints.add(magikBreakpoint);

        return magikBreakpoint;
    }

    private MagikBreakpoint sendSetBreakpoint(final String method, final int line)
            throws IOException, InterruptedException, ExecutionException {
        LOGGER.trace("Send set breakpoint: method: {}, line: {}", method, line);

        final MagikBreakpoint magikBreakpoint = new MagikBreakpoint(method, line);

        try {
            final CompletableFuture<ISlapResponse> breakpointSetFuture = this.slapProtocol.setBreakpoint(method, line);
            final BreakpointSetResponse breakpointSet = (BreakpointSetResponse) breakpointSetFuture.get();
            final long breakpointId = breakpointSet.getBreakpointId();
            magikBreakpoint.setBreakpointId(breakpointId);

            LOGGER.trace("Created breakpoint: {}", magikBreakpoint);
        } catch (ExecutionException exception) {
            final Throwable cause = exception.getCause();
            if (cause instanceof SlapErrorException) {
                // Do nothing, verified will become false, error will be shown to user.
                final SlapErrorException slapErrorException = (SlapErrorException) cause;
                final String message = slapErrorException.getError().getErrorMessage().name();
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
                magikBreakpoint.getMethodName(), magikBreakpoint.getMethodLine());

        final long breakpointId = magikBreakpoint.getBreakpointId();
        this.slapProtocol.deleteBreakpoint(breakpointId).get();

        LOGGER.trace("Deleted breakpoint: {}", magikBreakpoint);
    }
    // endregion

}
