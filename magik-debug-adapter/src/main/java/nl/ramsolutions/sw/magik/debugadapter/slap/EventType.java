package nl.ramsolutions.sw.magik.debugadapter.slap;

/**
 * Event type.
 */
@SuppressWarnings("checkstyle:JavadocVariable")
public enum EventType {

    BREAKPOINT(1),
    THREAD_STARTED(2),
    THREAD_ENDED(3),
    STEP_COMPLETED(4);

    private final int val;

    EventType(final int val) {
        this.val = val;
    }

    public int getVal() {
        return this.val;
    }

    /**
     * Get the {@link EventType} from an interger value.
     * @param value Integer value.
     * @return EventType
     */
    public static EventType valueOf(final int value) {
        for (final EventType eventType : EventType.values()) {
            if (eventType.getVal() == value) {
                return eventType;
            }
        }

        return null;
    }

}
