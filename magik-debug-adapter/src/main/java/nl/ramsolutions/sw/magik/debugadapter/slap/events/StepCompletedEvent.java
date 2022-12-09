package nl.ramsolutions.sw.magik.debugadapter.slap.events;

import java.nio.ByteBuffer;
import nl.ramsolutions.sw.magik.debugadapter.slap.ByteBufferHelper;
import nl.ramsolutions.sw.magik.debugadapter.slap.EventType;
import nl.ramsolutions.sw.magik.debugadapter.slap.ISlapEvent;

/**
 * Step completed event.
 */
@SuppressWarnings("JavadocVariable")
public class StepCompletedEvent implements ISlapEvent {

    // Event layout:
    //  0- 4: uint32, message length
    //  4- 8: uint32, response type
    //  8-12: uint32, event type
    // 12-16: uint32, thread id
    public static final int OFFSET_THREAD_ID = 12;

    private final long threadId;

    public StepCompletedEvent(final long threadId) {
        this.threadId = threadId;
    }

    public long getThreadId() {
        return this.threadId;
    }

    @Override
    public EventType getEventType() {
        return EventType.STEP_COMPLETED;
    }

    /**
     * Decode event from buffer.
     * @param buffer Buffer containing event.
     * @return Decoded event.
     */
    public static StepCompletedEvent decode(final ByteBuffer buffer) {
        final long threadId = ByteBufferHelper.readUInt32(buffer, OFFSET_THREAD_ID);
        return new StepCompletedEvent(threadId);
    }

}
