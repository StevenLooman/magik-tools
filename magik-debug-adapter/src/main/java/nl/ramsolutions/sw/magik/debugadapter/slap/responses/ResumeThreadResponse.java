package nl.ramsolutions.sw.magik.debugadapter.slap.responses;

import java.nio.ByteBuffer;
import nl.ramsolutions.sw.magik.debugadapter.slap.ISlapResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.RequestType;

/**
 * Resume thread response.
 */
@SuppressWarnings("JavadocVariable")
public class ResumeThreadResponse implements ISlapResponse {

    // Response layout:
    //  0- 4: uint32, message length
    //  4- 8: uint32, response type
    //  8-12: uint32, request type
    // 12-16: ???
    public static final int OFFSET_UNKNOWN = 12;

    @Override
    public RequestType getRequestType() {
        return RequestType.RESUME_THREAD;
    }

    /**
     * Decode message from buffer.
     * @param buffer Buffer containing message.
     * @return Decoded message.
     */
    @SuppressWarnings("S1172")
    public static ResumeThreadResponse decode(final ByteBuffer buffer) {
        return new ResumeThreadResponse();
    }

}
