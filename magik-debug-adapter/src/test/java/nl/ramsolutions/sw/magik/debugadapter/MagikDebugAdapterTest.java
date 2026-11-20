package nl.ramsolutions.sw.magik.debugadapter;

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
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import nl.ramsolutions.sw.magik.debugadapter.slap.events.BreakpointEvent;
import nl.ramsolutions.sw.magik.debugadapter.slap.events.ThreadStartedEvent;
import org.eclipse.lsp4j.debug.ConfigurationDoneArguments;
import org.eclipse.lsp4j.debug.DisconnectArguments;
import org.eclipse.lsp4j.debug.EvaluateArguments;
import org.eclipse.lsp4j.debug.EvaluateResponse;
import org.eclipse.lsp4j.debug.StoppedEventArguments;
import org.eclipse.lsp4j.debug.StoppedEventArgumentsReason;
import org.eclipse.lsp4j.debug.ThreadEventArguments;
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException;
import org.junit.jupiter.api.Test;

/** Tests for MagikDebugAdapter. */
@SuppressWarnings("checkstyle:MagicNumber")
class MagikDebugAdapterTest {

  private static final long TIMEOUT_SECONDS = 5;
  private static final int HANDSHAKE_LENGTH = 32;
  private static final int CLIENT_ID_LENGTH = 16;
  private static final int VERSION_OFFSET = 20;

  /** Fake agent: accepts one connection and does the handshake. */
  private static final class FakeAgent implements AutoCloseable {

    private final ServerSocket serverSocket;

    FakeAgent() throws IOException {
      final InetAddress loopback = InetAddress.getLoopbackAddress();
      this.serverSocket = new ServerSocket(0, 1, loopback);

      final Thread acceptThread = new Thread(this::acceptClient);
      acceptThread.setName("fake-agent-accept");
      acceptThread.setDaemon(true);
      acceptThread.start();
    }

    int getPort() {
      return this.serverSocket.getLocalPort();
    }

    private void acceptClient() {
      try (Socket socket = this.serverSocket.accept()) {
        final InputStream input = socket.getInputStream();
        input.readNBytes(CLIENT_ID_LENGTH);

        final byte[] handshake = this.createHandshake();
        final OutputStream output = socket.getOutputStream();
        output.write(handshake);
        output.flush();

        // Keep the connection open until the client closes it.
        input.read();
      } catch (final IOException exception) {
        // Server socket closed or client gone, nothing to do.
      }
    }

    private byte[] createHandshake() {
      final ByteBuffer buffer = ByteBuffer.allocate(HANDSHAKE_LENGTH);
      buffer.order(ByteOrder.LITTLE_ENDIAN);
      final byte[] agentId = "SwanOnAUnicycle\0".getBytes(StandardCharsets.UTF_8);
      buffer.put(agentId);
      buffer.position(VERSION_OFFSET);
      buffer.putInt(1);
      return buffer.array();
    }

    @Override
    public void close() throws IOException {
      this.serverSocket.close();
    }
  }

  @Test
  void testConfigurationDoneCompletes() {
    final MagikDebugAdapter adapter = new MagikDebugAdapter();

    final ConfigurationDoneArguments args = new ConfigurationDoneArguments();
    final CompletableFuture<Void> future = adapter.configurationDone(args);

    assertThat(future).isCompleted();
  }

  @Test
  void testEvaluateWithoutFrameIdFailsWithResponseError() {
    final MagikDebugAdapter adapter = new MagikDebugAdapter();

    final EvaluateArguments args = new EvaluateArguments();
    args.setExpression("1 + 1");
    final CompletableFuture<EvaluateResponse> future = adapter.evaluate(args);

    final Throwable thrown = catchThrowable(() -> future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
    assertThat(thrown).isInstanceOf(ExecutionException.class);
    final Throwable cause = thrown.getCause();
    assertThat(cause).isInstanceOf(ResponseErrorException.class);
    final String message = cause.getMessage();
    assertThat(message).contains("stack frame").contains("stopped thread");
  }

  @Test
  void testAttachCreatesManagersBeforeInitialized() throws Exception {
    final MagikDebugAdapter adapter = new MagikDebugAdapter();
    final RecordingDebugProtocolClient client = new RecordingDebugProtocolClient();
    adapter.connect(client);

    try (FakeAgent agent = new FakeAgent()) {
      // An IDE sends breakpoints, and the agent may send events, as soon as it sees `initialized`.
      client.setInitializedHook(
          () -> {
            final ThreadStartedEvent threadStarted = new ThreadStartedEvent(3);
            adapter.processEvent(threadStarted);
            final BreakpointEvent breakpointHit = new BreakpointEvent(99, 3);
            adapter.processEvent(breakpointHit);
          });

      final Map<String, Object> connect =
          Map.of("host", "127.0.0.1", "port", (double) agent.getPort());
      final Map<String, Object> args = Map.of("connect", connect);
      final CompletableFuture<Void> attachFuture = adapter.attach(args);
      attachFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

      final List<ThreadEventArguments> threadEvents = client.getThreadEvents();
      assertThat(threadEvents).hasSize(1);
      final List<StoppedEventArguments> stoppedEvents = client.getStoppedEvents();
      assertThat(stoppedEvents).hasSize(1);
      final StoppedEventArguments stopped = stoppedEvents.get(0);
      final String reason = stopped.getReason();
      assertThat(reason).isEqualTo(StoppedEventArgumentsReason.BREAKPOINT);
    } finally {
      final DisconnectArguments disconnectArgs = new DisconnectArguments();
      final CompletableFuture<Void> disconnectFuture = adapter.disconnect(disconnectArgs);
      disconnectFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }
  }
}
