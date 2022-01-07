package nl.ramsolutions.sw.magik.debugadapter.slap.events;

import java.nio.ByteBuffer;
import nl.ramsolutions.sw.magik.debugadapter.slap.ByteBufferUtils;
import nl.ramsolutions.sw.magik.debugadapter.slap.EventType;
import nl.ramsolutions.sw.magik.debugadapter.slap.ISlapEvent;

/**
 * Thread ended event.
 */
@SuppressWarnings("JavadocVariable")
public class ThreadEndedEvent implements ISlapEvent {

    // Event layout:
    //  0- 4: uint32, message length
    //  4- 8: uint32, response type
    //  8-12: uint32, event type
    // 12-16: uint32, thread ID
    public static final int OFFSET_THREAD_ID = 12;

    private final long threadId;

    public ThreadEndedEvent(final long threadId) {
        this.threadId = threadId;
    }

    public long getThreadId() {
        return this.threadId;
    }

    @Override
    public EventType getEventType() {
        return EventType.THREAD_ENDED;
    }

    @Override
    public String toString() {
        return String.format(
            "%s@%s(%s)",
            this.getClass().getName(), Integer.toHexString(this.hashCode()),
            this.threadId);
    }

    /**
     * Decode event from buffer.
     * @param buffer Buffer containing event.
     * @return Decoded event.
     */
    public static ThreadEndedEvent decode(final ByteBuffer buffer) {
        final long threadId = ByteBufferUtils.readUInt32(buffer, OFFSET_THREAD_ID);
        return new ThreadEndedEvent(threadId);
    }

}
