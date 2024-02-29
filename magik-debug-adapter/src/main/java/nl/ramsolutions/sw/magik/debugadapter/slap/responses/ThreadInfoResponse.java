package nl.ramsolutions.sw.magik.debugadapter.slap.responses;

import java.nio.ByteBuffer;
import java.util.EnumSet;
import java.util.Set;
import nl.ramsolutions.sw.magik.debugadapter.slap.ByteBufferHelper;
import nl.ramsolutions.sw.magik.debugadapter.slap.ISlapResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.RequestType;

/** Thread info response. */
@SuppressWarnings("JavadocVariable")
public class ThreadInfoResponse implements ISlapResponse {

  // Response layout:
  //  0- 4: uint32, message length
  //  4- 8: uint32, response type
  //  8-12: uint32, request type
  // 12-16: uint32, ???, always 0
  // 16-20: uint32, priority
  // 20-24: bool, daemon
  // 24-28: uint32, flags
  // 28-32: uint32, name length
  // 32-..: string, name string
  public static final int OFFSET_UNKNOWN = 12;
  public static final int OFFSET_PRIORITY = 16;
  public static final int OFFSET_DAEMON = 20;
  public static final int OFFSET_FLAGS = 24;
  public static final int OFFSET_NAME_LENGTH = 28;
  public static final int OFFSET_NAME = 32;

  /** Thread state. */
  @SuppressWarnings("checkstyle:JavadocVariable")
  public enum ThreadState {
    NEW(0),
    TERMINATED(1),
    RUNNABLE(2),
    BLOCKED(3),
    WAITING(4),
    OBJECT_WAIT(5),
    PARKED(6),
    SLEEPING(7),

    UNKNOWN(99);

    private final int val;

    ThreadState(final int val) {
      this.val = val;
    }

    public int getVal() {
      return this.val;
    }

    /**
     * Get the {@link ThreadState} from an interger value.
     *
     * @param value Integer value.
     * @return ThreadState
     */
    public static ThreadState valueOf(final int value) {
      for (final ThreadState threadState : ThreadState.values()) {
        if (threadState.getVal() == value) {
          return threadState;
        }
      }

      return ThreadState.UNKNOWN;
    }
  }

  /** Thread flag. */
  @SuppressWarnings("checkstyle:JavadocVariable")
  public enum ThreadFlag {
    SUSPENDED(1),
    INTERRUPTED(2),
    NATIVE(4);

    private final int val;

    ThreadFlag(final int val) {
      this.val = val;
    }

    public int getVal() {
      return this.val;
    }

    /**
     * Get the {@link ThreadFlag} from an interger value.
     *
     * @param value Integer value.
     * @return ThreadFlag
     */
    public static ThreadFlag valueOf(final int value) {
      for (final ThreadFlag threadFlag : ThreadFlag.values()) {
        if (threadFlag.getVal() == value) {
          return threadFlag;
        }
      }

      return null;
    }

    /**
     * Get the {@link ThreadFlag}s from an interger value.
     *
     * @param flags Integer value.
     * @return ThreadFlags
     */
    public static Set<ThreadFlag> flagsOf(final int flags) {
      final Set<ThreadFlag> set = EnumSet.noneOf(ThreadFlag.class);
      for (final ThreadFlag flag : EnumSet.allOf(ThreadFlag.class)) {
        if ((flags & flag.getVal()) != 0) {
          set.add(flag);
        }
      }
      return set;
    }
  }

  private final int priority;
  private final boolean daemon;
  private final Set<ThreadFlag> threadFlags;
  private final ThreadState threadState;
  private final String name;

  /**
   * Constructor.
   *
   * @param priority Priority.
   * @param daemon Daemon.
   * @param name Name.
   * @param threadState Thread state.
   * @param threadFlags Thread flags.
   */
  public ThreadInfoResponse(
      final int priority,
      final boolean daemon,
      final String name,
      final ThreadState threadState,
      final Set<ThreadFlag> threadFlags) {
    this.priority = priority;
    this.daemon = daemon;
    this.name = name;
    this.threadState = threadState;
    this.threadFlags = threadFlags;
  }

  public String getName() {
    return this.name;
  }

  public boolean getDaemon() {
    return this.daemon;
  }

  public int getPriority() {
    return this.priority;
  }

  public ThreadState getThreadState() {
    return this.threadState;
  }

  public Set<ThreadFlag> getThreadFlags() {
    return this.threadFlags;
  }

  @Override
  public String toString() {
    return String.format(
        "%s@%s(\"%s\", %s, %s, %s, %s)",
        this.getClass().getName(),
        Integer.toHexString(this.hashCode()),
        this.name,
        this.priority,
        this.daemon,
        this.threadState,
        this.threadFlags);
  }

  @Override
  public RequestType getRequestType() {
    return RequestType.GET_THREAD_INFO;
  }

  /**
   * Decode message from buffer.
   *
   * @param buffer Buffer containing message.
   * @return Decoded message.
   */
  public static ThreadInfoResponse decode(final ByteBuffer buffer) {
    final int priority = (int) ByteBufferHelper.readUInt32(buffer, OFFSET_PRIORITY);
    final boolean daemon = ByteBufferHelper.readUInt32(buffer, OFFSET_DAEMON) != 0;
    final int flags = (int) ByteBufferHelper.readUInt32(buffer, OFFSET_FLAGS);
    final String name = ByteBufferHelper.readString(buffer, OFFSET_NAME_LENGTH);

    final ThreadState threadState = ThreadState.valueOf(flags & BYTE_1_MASK);
    final Set<ThreadFlag> threadFlags = ThreadFlag.flagsOf((flags >> BYTE_2_SHIFT) & BYTE_1_MASK);

    return new ThreadInfoResponse(priority, daemon, name, threadState, threadFlags);
  }
}
