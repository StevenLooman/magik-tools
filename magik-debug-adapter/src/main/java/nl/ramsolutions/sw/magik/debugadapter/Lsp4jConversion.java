package nl.ramsolutions.sw.magik.debugadapter;

import java.nio.file.Path;
import java.util.List;
import javax.annotation.Nullable;
import nl.ramsolutions.sw.magik.debugadapter.BreakpointManager.MagikBreakpoint;
import nl.ramsolutions.sw.magik.debugadapter.VariableManager.MagikVariable;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.BreakpointSetResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.ThreadInfoResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.ThreadStackResponse;
import org.eclipse.lsp4j.debug.Breakpoint;
import org.eclipse.lsp4j.debug.Source;
import org.eclipse.lsp4j.debug.StackFrame;
import org.eclipse.lsp4j.debug.Thread;
import org.eclipse.lsp4j.debug.Variable;

/**
 * LSP4J <-> Magik debug adapter conversion.
 */
@SuppressWarnings("checkstyle:MagicNumber")
public final class Lsp4jConversion {

    private Lsp4jConversion() {
    }

    /**
     * Convert a slap {{ThreadInfoResponse}} to a lsp4j {{Thread}}.
     * @param threadId ID of the thread.
     * @param threadInfo Slap thread info.
     * @return lsp4j {{Thread}}.
     */
    public static Thread toLsp4j(final long threadId, final ThreadInfoResponse threadInfo) {
        final Thread thread = new Thread();
        thread.setId((int) threadId);
        thread.setName(threadInfo.getName());
        return thread;
    }

    /**
     * Convert a slap {{BreakpointSetResponse}} to a lsp4j {{Breakpoint}}.
     * @param breakpointSet Slap breakpoint set.
     * @return lsp4j {{Breakpoint}}.
     */
    public static Breakpoint toLsp4j(final BreakpointSetResponse breakpointSet, final Source source) {
        final Breakpoint breakpoint = new Breakpoint();
        breakpoint.setId((int) breakpointSet.getBreakpointId());
        breakpoint.setVerified(true);
        breakpoint.setSource(source);
        return breakpoint;
    }

    /**
     * Convert a slap {{ThreadStackResponse.StackElement}} to lsp4j {{StackFrame}}.
     * @param threadId Thread ID.
     * @param stackElement Stack element.
     * @param path Path.
     * @return lsp4j {{StackFrame}}.
     */
    public static StackFrame toLsp4j(
            final long threadId, final ThreadStackResponse.StackElement stackElement, final @Nullable Path path) {
        final int level = stackElement.getLevel();
        final int frameId = Lsp4jConversion.threadIdLevelToFrameId(threadId, level);
        final StackFrame frame = new StackFrame();
        frame.setId(frameId);
        frame.setName(stackElement.getName());
        frame.setLine(stackElement.getOffset());
        if (path != null) {
            final Source source = new Source();
            source.setPath(path.toString());
            frame.setSource(source);
        }
        return frame;
    }

    /**
     * Convert {{MagikBreakpoint}}s to LSP4j {{Breakpoint}}s.
     * @param source Source of file.
     * @param magikBreakpoints MagikBreakpoints.
     * @return Array of converted LSP4j {{Breakpoint}}s.
     */
    public static Breakpoint[] toLsp4j(final Source source, final List<MagikBreakpoint> magikBreakpoints) {
        return magikBreakpoints.stream()
            .map(magikBreakpoint -> {
                final Breakpoint breakpoint = new Breakpoint();
                breakpoint.setSource(source);
                breakpoint.setId((int) magikBreakpoint.getBreakpointId());
                breakpoint.setVerified(magikBreakpoint.isVerified());
                breakpoint.setMessage(magikBreakpoint.getMessage());
                return breakpoint;
            })
            .toArray(size -> new Breakpoint[size]);
    }

    /**
     * Convert {{MagikVariable}}s to LSP4j {{Variable}}s.
     * @param magikVariables MagikVariables.
     * @return List of converted LSP4j {{Variable}}s.
     */
    public static Variable[] toLsp4j(final List<MagikVariable> magikVariables) {
        return magikVariables.stream()
            .map(magikVariable -> {
                final Variable variable = new Variable();
                variable.setVariablesReference(magikVariable.getId());
                variable.setName(magikVariable.getName());
                variable.setValue(magikVariable.getValue());
                variable.setEvaluateName(magikVariable.getExpression());
                return variable;
            })
            .toArray(size -> new Variable[size]);
    }

    /**
     * Convert thread ID and stack level to frame ID.
     * @param threadId Thread ID.
     * @param level Stack level.
     * @return Frame ID.
     */
    public static int threadIdLevelToFrameId(final long threadId, final int level) {
        return (int) ((threadId << 16) | level);
    }

    /**
     * Extract thread ID from frame ID.
     * @param frameId Frame ID.
     * @return Thread ID.
     */
    public static long frameIdToThreadId(final long frameId) {
        return frameId >> 16;
    }

    /**
     * Extract stack level from frame ID.
     * @param frameId Frame ID.
     * @return Stack level.
     */
    public static int frameIdToLevel(final long frameId) {
        return (int) (frameId & 0xFFFF);
    }

}
