package nl.ramsolutions.sw.magik.debugadapter.slap.responses;

import java.nio.ByteBuffer;
import nl.ramsolutions.sw.magik.debugadapter.slap.ISlapResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.RequestType;

/**
 * Step response.
 */
@SuppressWarnings("JavadocVariable")
public class StepResponse implements ISlapResponse {

    // Response layout:
    //  0- 4: uint32, message length
    //  4- 8: uint32, response type
    //  8-12: uint32, request type
    // 12-16: uint32, always 0???
    public static final int OFFSET_UNKNOWN = 12;

    @Override
    public RequestType getRequestType() {
        return RequestType.STEP;
    }

    /**
     * Decode message from buffer.
     * @param buffer Buffer containing message.
     * @return Decoded message.
     */
    @SuppressWarnings("S1172")
    public static StepResponse decode(final ByteBuffer buffer) {
        return new StepResponse();
    }

}
