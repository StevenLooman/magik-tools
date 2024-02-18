package nl.ramsolutions.sw.magik.debugadapter.slap.responses;

import java.nio.ByteBuffer;
import nl.ramsolutions.sw.magik.debugadapter.slap.ByteBufferHelper;
import nl.ramsolutions.sw.magik.debugadapter.slap.ISlapResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.RequestType;

/** Breakpoint set response. */
@SuppressWarnings("JavadocVariable")
public class BreakpointSetResponse implements ISlapResponse {

  // Response layout:
  //  0- 4: uint32, message length
  //  4- 8: uint32, response type
  //  8-12: uint32, request type
  // 12-16: uint32, breakpoint id
  public static final int OFFSET_BREAKPOINT_ID = 12;

  private final long breakpointId;

  public BreakpointSetResponse(final long breakpointId) {
    this.breakpointId = breakpointId;
  }

  public long getBreakpointId() {
    return this.breakpointId;
  }

  @Override
  public RequestType getRequestType() {
    return RequestType.BREAKPOINT_SET;
  }

  @Override
  public String toString() {
    return String.format(
        "%s@%s(%s)",
        this.getClass().getName(), Integer.toHexString(this.hashCode()), this.breakpointId);
  }

  /**
   * Decode message from buffer.
   *
   * @param buffer Buffer containing message.
   * @return Decoded message.
   */
  public static BreakpointSetResponse decode(final ByteBuffer buffer) {
    final long breakpointId = ByteBufferHelper.readUInt32(buffer, OFFSET_BREAKPOINT_ID);

    return new BreakpointSetResponse(breakpointId);
  }
}
