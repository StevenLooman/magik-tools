package nl.ramsolutions.sw.magik.debugadapter.breakpoints;

import nl.ramsolutions.sw.magik.debugadapter.breakpoints.BreakpointManager.StopHook;
import org.eclipse.lsp4j.debug.StoppedEventArguments;
import org.eclipse.lsp4j.debug.StoppedEventArgumentsReason;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;

/** Keeps the thread stopped and tells the client. Handles every hit, so it comes last. */
public class StopHitAction implements HitAction {

  @Override
  public boolean handle(final HitContext context) {
    final StopHook stopHook = context.stopHook();
    stopHook.beforeStop();

    final long threadId = context.threadId();
    final StoppedEventArguments args = new StoppedEventArguments();
    args.setThreadId((int) threadId);
    args.setReason(StoppedEventArgumentsReason.BREAKPOINT);
    final IDebugProtocolClient debugClient = context.debugClient();
    debugClient.stopped(args);
    return true;
  }
}
