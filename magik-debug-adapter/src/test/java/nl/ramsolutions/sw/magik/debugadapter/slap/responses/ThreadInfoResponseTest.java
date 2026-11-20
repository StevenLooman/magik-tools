package nl.ramsolutions.sw.magik.debugadapter.slap.responses;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import nl.ramsolutions.sw.magik.debugadapter.slap.RequestType;
import nl.ramsolutions.sw.magik.debugadapter.slap.ResponseType;
import org.junit.jupiter.api.Test;

/** Tests for {@link ThreadInfoResponse}. */
@SuppressWarnings("checkstyle:MagicNumber")
class ThreadInfoResponseTest {

  private static final String THREAD_NAME = "Thread-3";

  /** Create a GET_THREAD_INFO reply as the agent sends it. */
  private ByteBuffer createReply(final int flags) {
    final byte[] nameBytes = THREAD_NAME.getBytes(StandardCharsets.UTF_8);
    final ByteBuffer buffer =
        ByteBuffer.allocate(ThreadInfoResponse.OFFSET_NAME + nameBytes.length);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(buffer.capacity());
    final int responseVal = ResponseType.REPLY.getVal();
    buffer.putInt(responseVal);
    final int requestVal = RequestType.GET_THREAD_INFO.getVal();
    buffer.putInt(requestVal);
    buffer.putInt(0);
    buffer.putInt(5);
    buffer.putInt(0);
    buffer.putInt(flags);
    buffer.putInt(nameBytes.length);
    buffer.put(nameBytes);
    buffer.flip();
    return buffer;
  }

  @Test
  void testDecodeRunningThread() {
    // Probed live: a running thread.
    final ByteBuffer reply = this.createReply(0x2);

    final ThreadInfoResponse response = ThreadInfoResponse.decode(reply);

    final ThreadInfoResponse.ThreadState threadState = response.getThreadState();
    assertThat(threadState).isEqualTo(ThreadInfoResponse.ThreadState.RUNNABLE);
    final Set<ThreadInfoResponse.ThreadFlag> threadFlags = response.getThreadFlags();
    assertThat(threadFlags).doesNotContain(ThreadInfoResponse.ThreadFlag.SUSPENDED);
    final String name = response.getName();
    assertThat(name).isEqualTo(THREAD_NAME);
    final int priority = response.getPriority();
    assertThat(priority).isEqualTo(5);
  }

  @Test
  void testDecodeSuspendedThread() {
    // Probed live: the same thread, suspended.
    final ByteBuffer reply = this.createReply(0x102);

    final ThreadInfoResponse response = ThreadInfoResponse.decode(reply);

    final ThreadInfoResponse.ThreadState threadState = response.getThreadState();
    assertThat(threadState).isEqualTo(ThreadInfoResponse.ThreadState.RUNNABLE);
    final Set<ThreadInfoResponse.ThreadFlag> threadFlags = response.getThreadFlags();
    assertThat(threadFlags).containsExactly(ThreadInfoResponse.ThreadFlag.SUSPENDED);
  }
}
