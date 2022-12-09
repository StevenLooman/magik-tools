package nl.ramsolutions.sw.magik.debugadapter.slap;

/**
 * Error message.
 */
@SuppressWarnings("checkstyle:JavadocVariable")
public enum ErrorMessage {

    UNKNOWN_ERROR(1),
    INVALID_LINE_NUMBER(2),
    METHOD_NOT_FOUND(3),
    ASSIST_CLASS_NOT_AVAILABLE(4),
    THREAD_NOT_SUSPENDED(5),
    REQUEST_TOO_SHORT(6),
    UNKNOWN_REQUEST(7),
    NATIVE_METHOD(8),
    NO_LINE_NUMBER_INFO(9),
    EVALUATION_FAILED(10),
    THREAD_ALREADY_SUSPENDED(20),
    BREAKPOINT_ALREADY_SET_AT_LOCATION(10040);

    private final int val;

    ErrorMessage(final int val) {
        this.val = val;
    }

    public int getVal() {
        return this.val;
    }

    /**
     * Get the {@link ErrorMessage} from an interger value.
     * @param value Integer value.
     * @return ErrorMessage
     */
    public static ErrorMessage valueOf(final int value) {
        for (final ErrorMessage errorMessage : ErrorMessage.values()) {
            if (errorMessage.getVal() == value) {
                return errorMessage;
            }
        }

        return null;
    }

}
