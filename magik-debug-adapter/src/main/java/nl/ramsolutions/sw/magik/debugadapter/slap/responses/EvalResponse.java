package nl.ramsolutions.sw.magik.debugadapter.slap.responses;

import java.nio.ByteBuffer;
import nl.ramsolutions.sw.magik.debugadapter.slap.ByteBufferUtils;
import nl.ramsolutions.sw.magik.debugadapter.slap.ISlapResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.RequestType;

/**
 * Eval response.
 */
@SuppressWarnings("JavadocVariable")
public class EvalResponse implements ISlapResponse {

    // Named EvalResponse to prevent colission with Lsp4j.debug.EvaluateResponse

    // Response layout:
    //  0- 4: uint32, message length
    //  4- 8: uint32, response type
    //  8-12: uint32, request type
    // 12-16: ???  maybe type?
    // 16-20: uint32, result length
    // 20-..: string, result string
    public static final int OFFSET_UNKNOWN = 12;
    public static final int OFFSET_RESULT_LENGTH = 16;
    public static final int OFFSET_RESULT = 20;

    private final String result;

    public EvalResponse(String result) {
        this.result = result;
    }

    public String getResult() {
        return this.result;
    }

    @Override
    public RequestType getRequestType() {
        return RequestType.EVALUATE;
    }

    /**
     * Decode message from buffer.
     * @param buffer Buffer containing message.
     * @return Decoded message.
     */
    public static EvalResponse decode(final ByteBuffer buffer) {
        final String result = ByteBufferUtils.readString(buffer, OFFSET_RESULT_LENGTH);
        return new EvalResponse(result);
    }

}
