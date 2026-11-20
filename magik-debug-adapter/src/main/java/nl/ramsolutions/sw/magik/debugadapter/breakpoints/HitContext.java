package nl.ramsolutions.sw.magik.debugadapter.breakpoints;

import edu.umd.cs.findbugs.annotations.Nullable;
import nl.ramsolutions.sw.magik.debugadapter.breakpoints.BreakpointManager.MagikBreakpoint;
import nl.ramsolutions.sw.magik.debugadapter.breakpoints.BreakpointManager.StopHook;
import nl.ramsolutions.sw.magik.debugadapter.slap.ISlapProtocol;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;

/**
 * A breakpoint hit whose condition, if any, holds.
 *
 * @param breakpoint Breakpoint which was hit, or {@code null} if its id is not known.
 * @param threadId Thread which hit the breakpoint.
 * @param stopHook Hook for a step of the thread.
 * @param slapProtocol Protocol to the agent.
 * @param debugClient Client to send events to.
 */
public record HitContext(
    @Nullable MagikBreakpoint breakpoint,
    long threadId,
    StopHook stopHook,
    ISlapProtocol slapProtocol,
    IDebugProtocolClient debugClient) {}
