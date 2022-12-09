package nl.ramsolutions.sw.magik.debugadapter.slap.responses;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.magik.debugadapter.slap.ByteBufferHelper;
import nl.ramsolutions.sw.magik.debugadapter.slap.ISlapResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.RequestType;

/**
 * Thread list response.
 */
@SuppressWarnings("JavadocVariable")
public class ThreadListResponse implements ISlapResponse {

    // Response layout:
    //  0- 4: uint32, message length
    //  4- 8: uint32, response type
    //  8-12: uint32, request type
    // 12-16: uint32, num threads
    // 16-..: uint32, thread id, 4 bytes each
    public static final int OFFSET_NUM_THREADS = 12;
    public static final int OFFSET_THREAD_IDS = 16;

    private final List<Long> threadIds;

    public ThreadListResponse(final List<Long> threadIds) {
        this.threadIds = threadIds;
    }

    public List<Long> getThreadIds() {
        return Collections.unmodifiableList(this.threadIds);
    }

    @Override
    public String toString() {
        return String.format(
            "%s@%s(%s)",
            this.getClass().getName(), Integer.toHexString(this.hashCode()),
            this.threadIds);
    }

    @Override
    public RequestType getRequestType() {
        return RequestType.GET_THREAD_LIST;
    }

    /**
     * Decode message from buffer.
     * @param buffer Buffer containing message.
     * @return Decoded message.
     */
    public static ThreadListResponse decode(final ByteBuffer buffer) {
        final List<Long> threadIds = new ArrayList<>();
        final int numThreads = (int) ByteBufferHelper.readUInt32(buffer, OFFSET_NUM_THREADS);
        for (int i = 0; i < numThreads; ++i) {
            final long threadId = ByteBufferHelper.readUInt32(buffer, OFFSET_THREAD_IDS + i * INT_SIZE_BYTES);
            threadIds.add(threadId);
        }

        return new ThreadListResponse(threadIds);
    }

}
