package nl.ramsolutions.sw.magik.debugadapter.slap.events;

import java.nio.ByteBuffer;
import nl.ramsolutions.sw.magik.debugadapter.slap.ByteBufferHelper;
import nl.ramsolutions.sw.magik.debugadapter.slap.EventType;
import nl.ramsolutions.sw.magik.debugadapter.slap.ISlapEvent;

/**
 * Breakpoint event.
 */
@SuppressWarnings("JavadocVariable")
public class BreakpointEvent implements ISlapEvent {

    // Event layout:
    //  0- 4: uint32, message length
    //  4- 8: uint32, response type
    //  8-12: uint32, event type
    // 12-16: uint32, breakpoint ID
    // 16-20: uint32, thread ID
    public static final int OFFSET_BREAKPOINT_ID = 12;
    public static final int OFFSET_THREAD_ID = 16;

    private final long breakpointId;
    private final long threadId;

    public BreakpointEvent(final long breakpointId, final long threadId) {
        this.breakpointId = breakpointId;
        this.threadId = threadId;
    }

    public long getBreakpointId() {
        return this.breakpointId;
    }

    public long getThreadId() {
        return this.threadId;
    }

    @Override
    public EventType getEventType() {
        return EventType.BREAKPOINT;
    }

    @Override
    public String toString() {
        return String.format(
            "%s@%s(%s, %s)",
            this.getClass().getName(), Integer.toHexString(this.hashCode()),
            this.breakpointId, this.threadId);
    }

    /**
     * Decode event from buffer.
     * @param buffer Buffer containing event.
     * @return Decoded event.
     */
    public static BreakpointEvent decode(final ByteBuffer buffer) {
        final long breakpointId = ByteBufferHelper.readUInt32(buffer, OFFSET_BREAKPOINT_ID);
        final long threadId = ByteBufferHelper.readUInt32(buffer, OFFSET_THREAD_ID);
        return new BreakpointEvent(breakpointId, threadId);
    }

}
