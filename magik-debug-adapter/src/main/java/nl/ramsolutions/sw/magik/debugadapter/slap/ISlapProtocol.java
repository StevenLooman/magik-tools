package nl.ramsolutions.sw.magik.debugadapter.slap;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Slap protocol.
 */
public interface ISlapProtocol {

    /**
     * Invalid breakpoint ID.
     */
    int INVALID_BREAKPOINT_ID = -1;

    /**
     * Get the version of the remote debugger.
     * @return Version.
     */
    long getVersion();

    /**
     * Connect to the running session.
     * @throws IOException -
     * @throws SlapErrorException -
     */
    void connect() throws IOException, SlapException;

    /**
     * Close the connection to the debuggee.
     *
     * @throws IOException -
     */
    void close() throws IOException;

    /**
     * Is connected with debuggee.
     * @return True if connected, false if not.
     */
    boolean isConnected();

    // region: Breakpoints
    /**
     * Set a breakpoint.
     * @param method Method to set breakpoint in.
     * @param line Line in method, offset from start of method (`_method` token).
     * @return Response of request.
     * @throws IOException -
     */
    CompletableFuture<ISlapResponse> setBreakpoint(String method, int line) throws IOException;

    /**
     * Delete a breakpoint.
     * @param breakpointId Breakpoint to delete.
     * @return Response of request.
     * @throws IOException -
     */
    CompletableFuture<ISlapResponse> deleteBreakpoint(long breakpointId) throws IOException;

    /**
     * Enable a breakpoint.
     * @param breakpointId Breakpoint to enable.
     * @return Response of request.
     * @throws IOException -
     */
    CompletableFuture<ISlapResponse> enableBreakpoint(long breakpointId) throws IOException;

    /**
     * Disable a breakpoint.
     * @param breakpointId Breakpoint to disable.
     * @return Response of request.
     * @throws IOException -
     */
    CompletableFuture<ISlapResponse> disableBreakpoint(long breakpointId) throws IOException;
    // endregion

    // region: Thread info
    /**
     * Get a list of threads.
     * @return Response of request.
     * @throws IOException -
     */
    CompletableFuture<ISlapResponse> getThreadList() throws IOException;

    /**
     * Get information about a thread.
     * @param threadId Thread to get information from.
     * @return Response of request.
     * @throws IOException -
     */
    CompletableFuture<ISlapResponse> getThreadInfo(long threadId) throws IOException;

    /**
     * Get the stack of a thread.
     * @param threadId Thread to get stack from.
     * @return Response of request.
     * @throws IOException -
     */
    CompletableFuture<ISlapResponse> getThreadStack(long threadId) throws IOException;

    /**
     * Get the locals from a stack frame.
     * @param threadId Thread to get locals from.
     * @param level Stack level.
     * @return Response of request.
     * @throws IOException -
     */
    CompletableFuture<ISlapResponse> getStackFrameLocals(long threadId, int level) throws IOException;
    // endregion

    // region: Thread control
    /**
     * Suspend a thread.
     * @param threadId Thread to suspend.
     * @return Response of request.
     * @throws IOException -
     */
    CompletableFuture<ISlapResponse> suspendThread(long threadId) throws IOException;

    /**
     * Resume a thread.
     * @param threadId Thread to resume.
     * @return Response of request.
     * @throws IOException -
     */
    CompletableFuture<ISlapResponse> resumeThread(long threadId) throws IOException;

    /**
     * Perform a step.
     * @param threadId Thread to step.
     * @param stepType Type of step.
     * @param count Number of steps.
     * @return Response of request.
     * @throws IOException -
     */
    CompletableFuture<ISlapResponse> step(long threadId, StepType stepType, int count) throws IOException;

    /**
     * Evaluate {@code expression}.
     * @param threadId Thread to evaluate in.
     * @param level Stack level to evaluate in.
     * @param expression Code to evaluate.
     * @return Response of request.
     * @throws IOException -
     */
    CompletableFuture<ISlapResponse> evaluate(long threadId, int level, String expression) throws IOException;
    // endregion

    /**
     * Get the source file for method.
     * @param method Method to get source file for.
     * @return Response of request.
     * @throws IOException -
     */
    CompletableFuture<ISlapResponse> getSourceFile(String method) throws IOException;

}
