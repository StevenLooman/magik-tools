package nl.ramsolutions.sw.magik.debugadapter.slap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import nl.ramsolutions.sw.magik.debugadapter.slap.events.BreakpointEvent;
import nl.ramsolutions.sw.magik.debugadapter.slap.events.DisconnectedEvent;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.EvalResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.ThreadStackResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Tests for {@link SlapProtocol}, against a fake agent on a loopback socket. */
@SuppressWarnings("checkstyle:MagicNumber")
class SlapProtocolTest {

  private static final String RECEIVER_THREAD_NAME = "slap-protocol-receiver";
  private static final int HANDSHAKE_LENGTH = 32;
  private static final int REQUEST_HEADER_LENGTH = 16;
  private static final int REPLY_HEADER_LENGTH = 20;
  private static final int MESSAGE_HEADER_LENGTH = 8;
  private static final int UNKNOWN_TYPE = 99;
  private static final long VERSION = 7;
  private static final long TIMEOUT_SECONDS = 5;

  /** Fake debug agent: does the handshake, then reads requests and writes crafted bytes. */
  private static final class FakeAgent implements AutoCloseable {

    private final ServerSocket serverSocket;
    private final CompletableFuture<Socket> clientFuture = new CompletableFuture<>();

    FakeAgent(final int handshakeSplit) throws IOException {
      final InetAddress loopback = InetAddress.getLoopbackAddress();
      this.serverSocket = new ServerSocket(0, 1, loopback);

      final Thread acceptThread = new Thread(() -> this.acceptClient(handshakeSplit));
      acceptThread.setName("fake-agent-accept");
      acceptThread.setDaemon(true);
      acceptThread.start();
    }

    int getPort() {
      return this.serverSocket.getLocalPort();
    }

    private void acceptClient(final int handshakeSplit) {
      try {
        final Socket socket = this.serverSocket.accept();
        socket.setTcpNoDelay(true);

        final InputStream input = socket.getInputStream();
        input.readNBytes(16);

        final byte[] handshake = this.createHandshake();
        final OutputStream output = socket.getOutputStream();
        output.write(handshake, 0, handshakeSplit);
        output.flush();
        SlapProtocolTest.pause();
        output.write(handshake, handshakeSplit, handshake.length - handshakeSplit);
        output.flush();

        this.clientFuture.complete(socket);
      } catch (final IOException exception) {
        this.clientFuture.completeExceptionally(exception);
      }
    }

    private byte[] createHandshake() {
      final ByteBuffer buffer = ByteBuffer.allocate(HANDSHAKE_LENGTH);
      buffer.order(ByteOrder.LITTLE_ENDIAN);
      final byte[] agentId = "SwanOnAUnicycle\0".getBytes(StandardCharsets.UTF_8);
      buffer.put(agentId);
      buffer.put((byte) 1);
      buffer.position(20);
      buffer.putInt((int) VERSION);
      return buffer.array();
    }

    private Socket awaitClient() throws InterruptedException, ExecutionException, TimeoutException {
      return this.clientFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    /** Read one request (whole message) and return its bytes. */
    byte[] readRequest() throws Exception {
      final Socket socket = this.awaitClient();
      final InputStream input = socket.getInputStream();
      final byte[] lengthBytes = input.readNBytes(4);
      final ByteBuffer lengthBuffer = ByteBuffer.wrap(lengthBytes);
      lengthBuffer.order(ByteOrder.LITTLE_ENDIAN);
      final int length = lengthBuffer.getInt();
      final byte[] rest = input.readNBytes(length - 4);

      final byte[] request = new byte[length];
      System.arraycopy(lengthBytes, 0, request, 0, 4);
      System.arraycopy(rest, 0, request, 4, rest.length);
      return request;
    }

    /** Write bytes to the client in one write. */
    void writeBytes(final byte[] bytes) throws Exception {
      final Socket socket = this.awaitClient();
      final OutputStream output = socket.getOutputStream();
      output.write(bytes);
      output.flush();
    }

    /** Write bytes to the client in separate writes, with a pause in between. */
    void writePieces(final byte[] bytes, final int... pieceLengths) throws Exception {
      final Socket socket = this.awaitClient();
      final OutputStream output = socket.getOutputStream();
      int offset = 0;
      for (final int pieceLength : pieceLengths) {
        output.write(bytes, offset, pieceLength);
        output.flush();
        offset += pieceLength;
        SlapProtocolTest.pause();
      }
      output.write(bytes, offset, bytes.length - offset);
      output.flush();
    }

    void closeClient() throws Exception {
      final Socket socket = this.awaitClient();
      socket.close();
    }

    @Override
    public void close() throws IOException {
      if (this.clientFuture.isDone() && !this.clientFuture.isCompletedExceptionally()) {
        final Socket socket = this.clientFuture.join();
        socket.close();
      }
      this.serverSocket.close();
    }
  }

  private final BlockingQueue<ISlapEvent> events = new LinkedBlockingQueue<>();
  private FakeAgent agent;
  private SlapProtocol protocol;

  @AfterEach
  void tearDown() throws Exception {
    if (this.protocol != null) {
      this.protocol.close();
    }
    if (this.agent != null) {
      this.agent.close();
    }
    SlapProtocolTest.awaitReceiverThreadEnded();
  }

  private static void pause() {
    try {
      Thread.sleep(100);
    } catch (final InterruptedException exception) {
      final Thread currentThread = Thread.currentThread();
      currentThread.interrupt();
    }
  }

  private static boolean isReceiverThreadAlive() {
    final Map<Thread, StackTraceElement[]> stackTraces = Thread.getAllStackTraces();
    final Set<Thread> threads = stackTraces.keySet();
    for (final Thread thread : threads) {
      final String name = thread.getName();
      if (thread.isAlive() && RECEIVER_THREAD_NAME.equals(name)) {
        return true;
      }
    }
    return false;
  }

  private static void awaitReceiverThreadEnded() throws InterruptedException {
    final long timeoutNanos = TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
    final long deadline = System.nanoTime() + timeoutNanos;
    while (SlapProtocolTest.isReceiverThreadAlive() && System.nanoTime() < deadline) {
      Thread.sleep(10);
    }
  }

  private void connectProtocol(final int handshakeSplit) throws Exception {
    this.connectProtocol(handshakeSplit, this.events::add);
  }

  private void connectProtocol(
      final int handshakeSplit, final SlapProtocol.SlapEventListener listener) throws Exception {
    this.agent = new FakeAgent(handshakeSplit);
    final int port = this.agent.getPort();
    this.protocol = new SlapProtocol("127.0.0.1", port, listener);
    this.protocol.connect();
  }

  /** Create a message of only a header and some uint32 fields. */
  private byte[] createMessage(final ResponseType responseType, final int... fields) {
    final ByteBuffer buffer = ByteBuffer.allocate(MESSAGE_HEADER_LENGTH + 4 * fields.length);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(buffer.capacity());
    final int responseVal = responseType.getVal();
    buffer.putInt(responseVal);
    for (final int field : fields) {
      buffer.putInt(field);
    }
    return buffer.array();
  }

  /** Send an evaluate, answer it with {@code before} followed by its reply, and get the result. */
  private String evaluateAfter(final byte[] before) throws Exception {
    final CompletableFuture<ISlapResponse> future = this.protocol.evaluate(1, 0, "x");
    this.agent.readRequest();
    final byte[] reply = this.createEvalReply("after");
    final byte[] bytes = this.concatenate(before, reply);
    this.agent.writeBytes(bytes);
    return this.getResult(future);
  }

  /** Listener which throws on a breakpoint event and records the other events. */
  private SlapProtocol.SlapEventListener createThrowingListener(final Throwable throwable) {
    return event -> {
      if (!(event instanceof BreakpointEvent)) {
        this.events.add(event);
      } else if (throwable instanceof Error error) {
        throw error;
      } else {
        throw (RuntimeException) throwable;
      }
    };
  }

  private void assertDisconnected(final CompletableFuture<ISlapResponse> future) throws Exception {
    final ISlapEvent event = this.events.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    assertThat(event).isInstanceOf(DisconnectedEvent.class);
    final Throwable thrown = catchThrowable(() -> future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
    assertThat(thrown).isInstanceOf(ExecutionException.class);
    SlapProtocolTest.awaitReceiverThreadEnded();
    final boolean receiverAlive = SlapProtocolTest.isReceiverThreadAlive();
    assertThat(receiverAlive).isFalse();
  }

  private byte[] createEvalReply(final String result) {
    final byte[] resultBytes = result.getBytes(StandardCharsets.UTF_8);
    final ByteBuffer buffer = ByteBuffer.allocate(REPLY_HEADER_LENGTH + resultBytes.length);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(buffer.capacity());
    final int replyVal = ResponseType.REPLY.getVal();
    final int evaluateVal = RequestType.EVALUATE.getVal();
    buffer.putInt(replyVal);
    buffer.putInt(evaluateVal);
    buffer.putInt(0);
    buffer.putInt(resultBytes.length);
    buffer.put(resultBytes);
    return buffer.array();
  }

  /** Create an evaluate reply which claims a result longer than the message. */
  private byte[] createTruncatedEvalReply() {
    final ByteBuffer buffer = ByteBuffer.allocate(REPLY_HEADER_LENGTH);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(buffer.capacity());
    final int replyVal = ResponseType.REPLY.getVal();
    final int evaluateVal = RequestType.EVALUATE.getVal();
    buffer.putInt(replyVal);
    buffer.putInt(evaluateVal);
    buffer.putInt(0);
    buffer.putInt(1000);
    return buffer.array();
  }

  /** Create a reply of a multi-part reply, of which the part is a stack element or the end. */
  private byte[] createStackMessage(final int field, final String name, final String language) {
    final byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
    final byte[] languageBytes = language.getBytes(StandardCharsets.UTF_8);
    final ByteBuffer buffer = ByteBuffer.allocate(28 + nameBytes.length + languageBytes.length);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(buffer.capacity());
    final int replyVal = ResponseType.REPLY.getVal();
    final int stackVal = RequestType.GET_THREAD_STACK.getVal();
    buffer.putInt(replyVal);
    buffer.putInt(stackVal);
    buffer.putInt(field);
    buffer.putInt(4);
    buffer.putInt(nameBytes.length);
    buffer.putInt(languageBytes.length);
    buffer.put(nameBytes);
    buffer.put(languageBytes);
    return buffer.array();
  }

  private byte[] createStackStart() {
    final int stackVal = RequestType.GET_THREAD_STACK.getVal();
    return this.createMessage(ResponseType.REPLY, stackVal, 0);
  }

  private byte[] createStackEnd() {
    final int stackVal = RequestType.GET_THREAD_STACK.getVal();
    return this.createMessage(ResponseType.REPLY, stackVal, -1);
  }

  private Throwable getFailure(final CompletableFuture<ISlapResponse> future) {
    final Throwable thrown = catchThrowable(() -> future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
    assertThat(thrown).isInstanceOf(ExecutionException.class);
    return thrown.getCause();
  }

  private String createText(final int length) {
    final StringBuilder builder = new StringBuilder(length);
    for (int i = 0; i < length; ++i) {
      builder.append((char) ('a' + i % 26));
    }
    return builder.toString();
  }

  private String getResult(final CompletableFuture<ISlapResponse> future) throws Exception {
    final ISlapResponse response = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    final EvalResponse evalResponse = (EvalResponse) response;
    return evalResponse.getResult();
  }

  private byte[] concatenate(final byte[] first, final byte[] second) {
    final byte[] result = Arrays.copyOf(first, first.length + second.length);
    System.arraycopy(second, 0, result, first.length, second.length);
    return result;
  }

  @Test
  void testConnectReadsHandshakeSentInPieces() throws Exception {
    this.connectProtocol(10);

    final long version = this.protocol.getVersion();
    assertThat(version).isEqualTo(VERSION);
  }

  @Test
  void testReplyLongerThanReceiveBuffer() throws Exception {
    this.connectProtocol(HANDSHAKE_LENGTH);
    final String expected = this.createText(200_000);

    final CompletableFuture<ISlapResponse> future = this.protocol.evaluate(1, 0, "x");
    final byte[] request = this.agent.readRequest();
    assertThat(request).hasSize(REQUEST_HEADER_LENGTH + 4 + 1);
    final byte[] reply = this.createEvalReply(expected);
    this.agent.writeBytes(reply);

    final String result = this.getResult(future);
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void testLongReplyFollowedByShortReply() throws Exception {
    this.connectProtocol(HANDSHAKE_LENGTH);
    final String longText = this.createText(150_000);

    final CompletableFuture<ISlapResponse> longFuture = this.protocol.evaluate(1, 0, "x");
    this.agent.readRequest();
    final byte[] longReply = this.createEvalReply(longText);
    this.agent.writeBytes(longReply);
    final String longResult = this.getResult(longFuture);

    final CompletableFuture<ISlapResponse> shortFuture = this.protocol.evaluate(1, 0, "y");
    this.agent.readRequest();
    final byte[] shortReply = this.createEvalReply("short");
    this.agent.writeBytes(shortReply);
    final String shortResult = this.getResult(shortFuture);

    assertThat(longResult).isEqualTo(longText);
    assertThat(shortResult).isEqualTo("short");
  }

  @Test
  void testTwoRepliesInOneWrite() throws Exception {
    this.connectProtocol(HANDSHAKE_LENGTH);

    final CompletableFuture<ISlapResponse> firstFuture = this.protocol.evaluate(1, 0, "x");
    final CompletableFuture<ISlapResponse> secondFuture = this.protocol.evaluate(1, 0, "y");
    this.agent.readRequest();
    this.agent.readRequest();
    final byte[] firstReply = this.createEvalReply("first");
    final byte[] secondReply = this.createEvalReply("second");
    final byte[] replies = this.concatenate(firstReply, secondReply);
    this.agent.writeBytes(replies);

    final String firstResult = this.getResult(firstFuture);
    final String secondResult = this.getResult(secondFuture);
    assertThat(firstResult).isEqualTo("first");
    assertThat(secondResult).isEqualTo("second");
  }

  @Test
  void testReplySplitOverSeveralWrites() throws Exception {
    this.connectProtocol(HANDSHAKE_LENGTH);

    final CompletableFuture<ISlapResponse> future = this.protocol.evaluate(1, 0, "x");
    this.agent.readRequest();
    final byte[] reply = this.createEvalReply("split over several writes");
    // First piece ends inside the length field.
    this.agent.writePieces(reply, 2, 10, 5);

    final String result = this.getResult(future);
    assertThat(result).isEqualTo("split over several writes");
  }

  @Test
  void testEndOfStreamDisconnects() throws Exception {
    this.connectProtocol(HANDSHAKE_LENGTH);

    final CompletableFuture<ISlapResponse> future = this.protocol.evaluate(1, 0, "x");
    this.agent.readRequest();
    this.agent.closeClient();

    final ISlapEvent event = this.events.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    assertThat(event).isInstanceOf(DisconnectedEvent.class);
    final Throwable thrown = catchThrowable(() -> future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
    assertThat(thrown).isInstanceOf(ExecutionException.class);
    final Throwable cause = thrown.getCause();
    assertThat(cause).isInstanceOf(SlapException.class);
    SlapProtocolTest.awaitReceiverThreadEnded();
    final boolean receiverAlive = SlapProtocolTest.isReceiverThreadAlive();
    assertThat(receiverAlive).isFalse();
    final boolean connected = this.protocol.isConnected();
    assertThat(connected).isFalse();

    // Exactly one DisconnectedEvent.
    final ISlapEvent extraEvent = this.events.poll(300, TimeUnit.MILLISECONDS);
    assertThat(extraEvent).isNull();
  }

  @Test
  void testUnknownResponseTypeIsSkipped() throws Exception {
    this.connectProtocol(HANDSHAKE_LENGTH);
    final ByteBuffer buffer = ByteBuffer.allocate(MESSAGE_HEADER_LENGTH);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(MESSAGE_HEADER_LENGTH);
    buffer.putInt(UNKNOWN_TYPE);
    final byte[] unknown = buffer.array();

    final String result = this.evaluateAfter(unknown);

    assertThat(result).isEqualTo("after");
  }

  @Test
  void testReplyToUnknownRequestTypeIsSkipped() throws Exception {
    this.connectProtocol(HANDSHAKE_LENGTH);
    final byte[] unknown = this.createMessage(ResponseType.REPLY, UNKNOWN_TYPE, 0);

    final String result = this.evaluateAfter(unknown);

    assertThat(result).isEqualTo("after");
  }

  @Test
  void testErrorForUnknownRequestTypeIsSkipped() throws Exception {
    this.connectProtocol(HANDSHAKE_LENGTH);
    final byte[] unknown = this.createMessage(ResponseType.ERROR, UNKNOWN_TYPE, 1);

    final String result = this.evaluateAfter(unknown);

    assertThat(result).isEqualTo("after");
  }

  @Test
  void testUnknownEventTypeIsSkipped() throws Exception {
    this.connectProtocol(HANDSHAKE_LENGTH);
    final byte[] unknown = this.createMessage(ResponseType.EVENT, UNKNOWN_TYPE, 0);

    final String result = this.evaluateAfter(unknown);

    assertThat(result).isEqualTo("after");
  }

  @Test
  void testListenerExceptionIsSkipped() throws Exception {
    final IllegalStateException exception = new IllegalStateException("listener failed");
    final SlapProtocol.SlapEventListener listener = this.createThrowingListener(exception);
    this.connectProtocol(HANDSHAKE_LENGTH, listener);
    final int eventVal = EventType.BREAKPOINT.getVal();
    final byte[] breakpointEvent = this.createMessage(ResponseType.EVENT, eventVal, 1, 3);

    final String result = this.evaluateAfter(breakpointEvent);

    assertThat(result).isEqualTo("after");
  }

  @Test
  void testFatalErrorInListenerDisconnects() throws Exception {
    final Error error = new Error("fatal");
    final SlapProtocol.SlapEventListener listener = this.createThrowingListener(error);
    this.connectProtocol(HANDSHAKE_LENGTH, listener);
    final CompletableFuture<ISlapResponse> future = this.protocol.evaluate(1, 0, "x");
    this.agent.readRequest();
    final int eventVal = EventType.BREAKPOINT.getVal();
    final byte[] breakpointEvent = this.createMessage(ResponseType.EVENT, eventVal, 1, 3);

    this.agent.writeBytes(breakpointEvent);

    this.assertDisconnected(future);
  }

  @Test
  void testMessageLongerThanCapDisconnects() throws Exception {
    this.connectProtocol(HANDSHAKE_LENGTH);
    final CompletableFuture<ISlapResponse> future = this.protocol.evaluate(1, 0, "x");
    this.agent.readRequest();
    final ByteBuffer buffer = ByteBuffer.allocate(MESSAGE_HEADER_LENGTH);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(256 * 1024 * 1024);
    final int replyVal = ResponseType.REPLY.getVal();
    buffer.putInt(replyVal);
    final byte[] header = buffer.array();

    this.agent.writeBytes(header);

    this.assertDisconnected(future);
  }

  @Test
  void testMalformedReplyFailsItsOwnRequest() throws Exception {
    this.connectProtocol(HANDSHAKE_LENGTH);
    final CompletableFuture<ISlapResponse> firstFuture = this.protocol.evaluate(1, 0, "x");
    final CompletableFuture<ISlapResponse> secondFuture = this.protocol.evaluate(1, 0, "y");
    this.agent.readRequest();
    this.agent.readRequest();
    final byte[] malformed = this.createTruncatedEvalReply();
    final byte[] good = this.createEvalReply("second");
    final byte[] replies = this.concatenate(malformed, good);

    this.agent.writeBytes(replies);

    final Throwable failure = this.getFailure(firstFuture);
    assertThat(failure).isInstanceOf(SlapException.class);
    assertThat(failure).hasMessageContaining("EVALUATE");
    final String secondResult = this.getResult(secondFuture);
    assertThat(secondResult).isEqualTo("second");
    final String laterResult = this.evaluateAfter(new byte[0]);
    assertThat(laterResult).isEqualTo("after");
  }

  @Test
  void testMalformedErrorFailsItsOwnRequest() throws Exception {
    this.connectProtocol(HANDSHAKE_LENGTH);
    final CompletableFuture<ISlapResponse> firstFuture = this.protocol.evaluate(1, 0, "x");
    final CompletableFuture<ISlapResponse> secondFuture = this.protocol.evaluate(1, 0, "y");
    this.agent.readRequest();
    this.agent.readRequest();
    // An error message without its error field.
    final int evaluateVal = RequestType.EVALUATE.getVal();
    final byte[] malformed = this.createMessage(ResponseType.ERROR, evaluateVal);
    final byte[] good = this.createEvalReply("second");
    final byte[] replies = this.concatenate(malformed, good);

    this.agent.writeBytes(replies);

    final Throwable failure = this.getFailure(firstFuture);
    assertThat(failure).isInstanceOf(SlapException.class);
    final String secondResult = this.getResult(secondFuture);
    assertThat(secondResult).isEqualTo("second");
  }

  @Test
  void testMalformedPartOfMultiPartReplyFailsItsOwnRequest() throws Exception {
    this.connectProtocol(HANDSHAKE_LENGTH);
    final CompletableFuture<ISlapResponse> firstFuture = this.protocol.getThreadStack(1);
    this.agent.readRequest();
    final byte[] start = this.createStackStart();
    // A part which claims a name longer than the message.
    final byte[] malformed = this.createStackMessage(0, "", "");
    malformed[20] = 100;
    final byte[] startAndMalformed = this.concatenate(start, malformed);
    this.agent.writeBytes(startAndMalformed);

    final Throwable failure = this.getFailure(firstFuture);
    assertThat(failure).isInstanceOf(SlapException.class);

    // A second request is pending while the rest of the first reply comes in.
    final CompletableFuture<ISlapResponse> secondFuture = this.protocol.getThreadStack(1);
    this.agent.readRequest();
    final byte[] part = this.createStackMessage(1, "m()", "Magik");
    final byte[] end = this.createStackEnd();
    final byte[] partAndEnd = this.concatenate(part, end);
    this.agent.writeBytes(partAndEnd);

    // The rest of the first reply is skipped, up to its end, it does not complete the second.
    SlapProtocolTest.pause();
    SlapProtocolTest.pause();
    assertThat(secondFuture).isNotDone();

    final byte[] secondPart = this.createStackMessage(2, "n()", "Magik");
    final byte[] startAndPart = this.concatenate(start, secondPart);
    final byte[] second = this.concatenate(startAndPart, end);
    this.agent.writeBytes(second);

    final ISlapResponse response = secondFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    final ThreadStackResponse stackResponse = (ThreadStackResponse) response;
    final List<ThreadStackResponse.StackElement> frames = stackResponse.getStackFrames();
    assertThat(frames).hasSize(1);
    final ThreadStackResponse.StackElement frame = frames.get(0);
    final String frameName = frame.getName();
    assertThat(frameName).isEqualTo("n()");
  }
}
