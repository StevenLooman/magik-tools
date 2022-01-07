package nl.ramsolutions.sw.magik.debugadapter.slap.responses;

import java.nio.ByteBuffer;
import nl.ramsolutions.sw.magik.debugadapter.slap.ByteBufferUtils;
import nl.ramsolutions.sw.magik.debugadapter.slap.ErrorMessage;
import nl.ramsolutions.sw.magik.debugadapter.slap.ISlapResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.RequestType;

/**
 * Error response.
 */
@SuppressWarnings("JavadocVariable")
public class ErrorResponse implements ISlapResponse {

    // Response layout:
    //  0- 4: uint32, message length
    //  4- 8: uint32, response type
    //  8-12: uint32, request type
    // 12-16: uint32, error type
    public static final int OFFSET_ERROR_TYPE = 12;

    private final RequestType requestType;
    private final ErrorMessage errorMessage;

    public ErrorResponse(final RequestType requestType, final ErrorMessage errorMessage) {
        this.requestType = requestType;
        this.errorMessage = errorMessage;
    }

    public ErrorMessage getErrorMessage() {
        return this.errorMessage;
    }

    @Override
    public String toString() {
        return String.format(
            "%s@%s(%s)",
            this.getClass().getName(), Integer.toHexString(this.hashCode()),
            this.errorMessage);
    }

    @Override
    public RequestType getRequestType() {
        return this.requestType;
    }

    /**
     * Decode message from buffer.
     * @param buffer Buffer containing message.
     * @return Decoded message.
     */
    public static ErrorResponse decode(final ByteBuffer buffer) {
        final int requestTypeVal = (int) ByteBufferUtils.readUInt32(buffer, OFFSET_REQUEST_TYPE);
        final RequestType requestType = RequestType.valueOf(requestTypeVal);
        final int errorMessageVal = (int) ByteBufferUtils.readUInt32(buffer, OFFSET_ERROR_TYPE);
        final ErrorMessage errorMessage = ErrorMessage.valueOf(errorMessageVal);

        return new ErrorResponse(requestType, errorMessage);
    }

}
