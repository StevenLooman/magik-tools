package nl.ramsolutions.sw.magik.debugadapter.slap;

/**
 * Slap response.
 */
@SuppressWarnings("JavadocVariable")
public interface ISlapResponse {

    // Response layout:
    //  0- 4: uint32, message length
    //  4- 8: uint32, response type
    //  8-12: uint32, request type
    //  ....: ....
    int OFFSET_MESSAGE_LENGTH = 0;
    int OFFSET_RESPONSE_TYPE = 4;
    int OFFSET_REQUEST_TYPE = 8;

    int BYTE_2_MASK = 0xff00;
    int BYTE_2_SHIFT = 8;
    int BYTE_1_MASK = 0xff;
    int INT_SIZE_BYTES = 4;

    /**
     * Get the {{RequestType}} from this response.
     * @return RequestType.
     */
    RequestType getRequestType();

}
