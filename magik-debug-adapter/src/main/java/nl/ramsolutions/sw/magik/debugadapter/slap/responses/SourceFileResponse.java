package nl.ramsolutions.sw.magik.debugadapter.slap.responses;

import java.nio.ByteBuffer;
import nl.ramsolutions.sw.magik.debugadapter.slap.ByteBufferHelper;
import nl.ramsolutions.sw.magik.debugadapter.slap.ISlapResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.RequestType;

/** Source file response. */
@SuppressWarnings("JavadocVariable")
public class SourceFileResponse implements ISlapResponse {

  // Response layout:
  //  0- 4: uint32, message length
  //  4- 8: uint32, response type
  //  8-12: uint32, request type
  // 12-16: ???
  // 16-20: uint32, filename length
  // 20-..: string, filename string
  public static final int OFFSET_UNKNOWN = 12;
  public static final int OFFSET_FILENAME_LENGTH = 16;
  public static final int OFFSET_FILENAME = 20;

  private String filename;

  public SourceFileResponse(final String filename) {
    this.filename = filename;
  }

  public String getFilename() {
    return this.filename;
  }

  @Override
  public RequestType getRequestType() {
    return RequestType.SOURCE_FILE;
  }

  /**
   * Decode message from buffer.
   *
   * @param buffer Buffer containing message.
   * @return Decoded message.
   */
  public static SourceFileResponse decode(final ByteBuffer buffer) {
    final String filename = ByteBufferHelper.readString(buffer, OFFSET_FILENAME_LENGTH);

    return new SourceFileResponse(filename);
  }
}
