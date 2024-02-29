package nl.ramsolutions.sw.magik.debugadapter.slap.responses;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.magik.debugadapter.slap.ByteBufferHelper;
import nl.ramsolutions.sw.magik.debugadapter.slap.ISlapResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.RequestType;

/** Thread stack response. */
@SuppressWarnings("JavadocVariable")
public class ThreadStackResponse implements ISlapResponse {

  // The response consists of a multi-reponse.
  // Response layout:
  //  0- 4: uint32, message length
  //  4- 8: uint32, response type
  //  8-12: uint32, ??? -- 4, 1, 0, 0?  flags? local count?
  // 12-16: uint32, level
  // 16-20: uint32, offset
  // 20-24: uint32, name length
  // 24-28: uint32, language length
  // 28-..: string, name + language
  public static final int OFFSET_LEVEL = 12;
  public static final int OFFSET_OFFSET = 16;
  public static final int OFFSET_NAME_LENGTH = 20;
  public static final int OFFSET_LANGUAGE_LENGTH = 24;
  public static final int OFFSET_NAME_LANGUAGE = 28;

  /** Stack element. */
  public static class StackElement implements ISlapResponse {

    private final int level;
    private final int offset;
    private String name;
    private final String language;

    /**
     * Constructor.
     *
     * @param level Level.
     * @param offset Offset.
     * @param name Name.
     * @param language Language.
     */
    public StackElement(
        final int level, final int offset, final String name, final String language) {
      this.level = level;
      this.offset = offset;
      this.name = name;
      this.language = language;
    }

    public int getLevel() {
      return this.level;
    }

    public int getOffset() {
      return this.offset;
    }

    public String getName() {
      return this.name;
    }

    public void setName(final String name) {
      this.name = name;
    }

    public String getLanguage() {
      return this.language;
    }

    @Override
    public String toString() {
      return String.format(
          "%s@%s(%s, %s, %s, %s)",
          this.getClass().getName(),
          Integer.toHexString(this.hashCode()),
          this.level,
          this.offset,
          this.name,
          this.language);
    }

    @Override
    public RequestType getRequestType() {
      return RequestType.GET_THREAD_STACK;
    }

    /**
     * Decode message from buffer.
     *
     * @param buffer Buffer containing message.
     * @return Decoded message.
     */
    public static StackElement decode(final ByteBuffer buffer) {
      final int level = (int) ByteBufferHelper.readUInt32(buffer, OFFSET_LEVEL);
      final int offset = (int) ByteBufferHelper.readUInt32(buffer, OFFSET_OFFSET);

      final int nameLength = (int) ByteBufferHelper.readUInt32(buffer, OFFSET_NAME_LENGTH);
      final byte[] nameArr = new byte[nameLength];
      buffer.position(OFFSET_NAME_LANGUAGE);
      buffer.get(nameArr);
      final String name = new String(nameArr, StandardCharsets.UTF_8);

      final int languageLength = (int) ByteBufferHelper.readUInt32(buffer, OFFSET_LANGUAGE_LENGTH);
      final byte[] langArr = new byte[languageLength];
      buffer.position(OFFSET_NAME_LANGUAGE + nameLength);
      buffer.get(langArr);
      final String language = new String(langArr, StandardCharsets.UTF_8);

      return new StackElement(level, offset, name, language);
    }
  }

  private List<StackElement> stackFrames;

  /**
   * Constructor.
   *
   * @param subResponses Stack elements.
   */
  public ThreadStackResponse(final List<ISlapResponse> subResponses) {
    this.stackFrames = subResponses.stream().map(StackElement.class::cast).toList();
  }

  public List<StackElement> getStackFrames() {
    return Collections.unmodifiableList(this.stackFrames);
  }

  @Override
  public RequestType getRequestType() {
    return RequestType.GET_THREAD_STACK;
  }
}
