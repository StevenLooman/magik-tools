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
import nl.ramsolutions.sw.magik.debugadapter.breakpoints.AnyErrorFilter;
import nl.ramsolutions.sw.magik.debugadapter.breakpoints.ExceptionFilter;
import nl.ramsolutions.sw.magik.debugadapter.breakpoints.HitAction;
import nl.ramsolutions.sw.magik.debugadapter.breakpoints.HitContext;
import nl.ramsolutions.sw.magik.debugadapter.breakpoints.StopHitAction;
import nl.ramsolutions.sw.magik.debugadapter.slap.events.BreakpointEvent;
import nl.ramsolutions.sw.magik.debugadapter.slap.events.ThreadStartedEvent;
import org.eclipse.lsp4j.debug.Capabilities;
import org.eclipse.lsp4j.debug.ConfigurationDoneArguments;
import org.eclipse.lsp4j.debug.DisconnectArguments;
import org.eclipse.lsp4j.debug.EvaluateArguments;
import org.eclipse.lsp4j.debug.EvaluateResponse;
import org.eclipse.lsp4j.debug.ExceptionBreakpointsFilter;
import org.eclipse.lsp4j.debug.InitializeRequestArguments;
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
  void testInitializeDoesNotAdvertiseLogpointsWhenNoActionSupportsThem() throws Exception {
    final MagikDebugAdapter adapter =
        new MagikDebugAdapter() {
          @Override
          protected List<HitAction> createHitActions() {
            return List.of(new StopHitAction());
          }
        };

    final InitializeRequestArguments args = new InitializeRequestArguments();
    final CompletableFuture<Capabilities> future = adapter.initialize(args);

    final Capabilities capabilities = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    final Boolean supportsLogPoints = capabilities.getSupportsLogPoints();
    assertThat(supportsLogPoints).isNull();
  }

  @Test
  void testInitializeAdvertisesLogpointsWhenAnActionSupportsThem() throws Exception {
    final HitAction logpointAction =
        new HitAction() {
          @Override
          public boolean handle(final HitContext context) {
            return false;
          }

          @Override
          public boolean supportsLogpoints() {
            return true;
          }
        };
    final MagikDebugAdapter adapter =
        new MagikDebugAdapter() {
          @Override
          protected List<HitAction> createHitActions() {
            return List.of(logpointAction, new StopHitAction());
          }
        };

    final InitializeRequestArguments args = new InitializeRequestArguments();
    final CompletableFuture<Capabilities> future = adapter.initialize(args);

    final Capabilities capabilities = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    final Boolean supportsLogPoints = capabilities.getSupportsLogPoints();
    assertThat(supportsLogPoints).isTrue();
  }

  @Test
  void testAdvertisedFiltersAreUnchanged() throws Exception {
    final MagikDebugAdapter adapter =
        new MagikDebugAdapter() {
          @Override
          protected List<ExceptionFilter> createExceptionFilters() {
            return List.of(new AnyErrorFilter());
          }
        };

    final InitializeRequestArguments args = new InitializeRequestArguments();
    final CompletableFuture<Capabilities> future = adapter.initialize(args);

    final Capabilities capabilities = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    final ExceptionBreakpointsFilter[] filters = capabilities.getExceptionBreakpointFilters();
    assertThat(filters).hasSize(1);
    final ExceptionBreakpointsFilter filter = filters[0];
    final String label = filter.getLabel();
    assertThat(label).isEqualTo("Any error condition");
    final String filterId = filter.getFilter();
    assertThat(filterId).isEqualTo("_self.taxonomy.includes?(:error)");
    final String description = filter.getDescription();
    assertThat(description).isNull();
    final Boolean isDefault = filter.getDefault_();
    assertThat(isDefault).isNull();
    final Boolean supportsCondition = filter.getSupportsCondition();
    assertThat(supportsCondition).isNull();
  }

  @Test
  void testInitializeAdvertisesTheFiltersOfTheFactory() throws Exception {
    final ExceptionFilter extraFilter =
        new ExceptionFilter() {
          @Override
          public String getId() {
            return "extra";
          }

          @Override
          public String getLabel() {
            return "Extra";
          }

          @Override
          public String getCondition() {
            return "True";
          }
        };
    final MagikDebugAdapter adapter =
        new MagikDebugAdapter() {
          @Override
          protected List<ExceptionFilter> createExceptionFilters() {
            return List.of(new AnyErrorFilter(), extraFilter);
          }
        };

    final InitializeRequestArguments args = new InitializeRequestArguments();
    final CompletableFuture<Capabilities> future = adapter.initialize(args);

    final Capabilities capabilities = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    final ExceptionBreakpointsFilter[] filters = capabilities.getExceptionBreakpointFilters();
    assertThat(filters).hasSize(2);
    final ExceptionBreakpointsFilter extra = filters[1];
    final String extraFilterId = extra.getFilter();
    assertThat(extraFilterId).isEqualTo("extra");
    final String extraLabel = extra.getLabel();
    assertThat(extraLabel).isEqualTo("Extra");
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
  void testEvaluateWithFrameIdBeforeAttachFailsWithResponseError() {
    final MagikDebugAdapter adapter = new MagikDebugAdapter();

    final EvaluateArguments args = new EvaluateArguments();
    args.setExpression("1 + 1");
    args.setFrameId(1);
    final CompletableFuture<EvaluateResponse> future = adapter.evaluate(args);

    final Throwable thrown = catchThrowable(() -> future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
    assertThat(thrown).isInstanceOf(ExecutionException.class);
    final Throwable cause = thrown.getCause();
    assertThat(cause).isInstanceOf(ResponseErrorException.class);
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
