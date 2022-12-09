package nl.ramsolutions.sw.magik.debugadapter.slap;

/**
 * Request type.
 */
@SuppressWarnings("checkstyle:JavadocVariable")
public enum RequestType {

    GET_THREAD_LIST(0),
    GET_THREAD_INFO(1),
    SUSPEND_THREAD(2),
    RESUME_THREAD(3),
    GET_THREAD_STACK(4),
    GET_FRAME_LOCALS(5),
    BREAKPOINT_SET(6),
    BREAKPOINT_MODIFY(7),
    EVALUATE(8),
    SOURCE_FILE(9),
    STEP(10),

    UNKOWN(255);

    private final int val;

    RequestType(final int val) {
        this.val = val;
    }

    public int getVal() {
        return this.val;
    }

    /**
     * Get the {@link RequestType} from an interger value.
     * @param value Integer value.
     * @return RequestType
     */
    public static RequestType valueOf(final int value) {
        for (final RequestType requestType : RequestType.values()) {
            if (requestType.getVal() == value) {
                return requestType;
            }
        }

        return null;
    }

}
