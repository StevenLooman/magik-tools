package nl.ramsolutions.sw.magik.debugadapter.slap;

/**
 * Slap event.
 */
@SuppressWarnings("JavadocVariable")
public interface ISlapEvent {

    // Event layout:
    //  0- 4: uint32, message length
    //  4- 8: uint32, response type
    //  8-12: uint32, event type
    //  ....: ....
    int OFFSET_MESSAGE_LENGTH = 0;
    int OFFSET_RESPONSE_TYPE = 4;
    int OFFSET_EVENT_TYPE = 8;

    /**
     * Get the {@link EventType} from this event.
     * @return EventType.
     */
    EventType getEventType();

}
