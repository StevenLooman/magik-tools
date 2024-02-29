package nl.ramsolutions.sw.magik.debugadapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import nl.ramsolutions.sw.magik.debugadapter.slap.ErrorMessage;
import nl.ramsolutions.sw.magik.debugadapter.slap.ISlapProtocol;
import nl.ramsolutions.sw.magik.debugadapter.slap.ISlapResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.RequestType;
import nl.ramsolutions.sw.magik.debugadapter.slap.SlapErrorException;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.ErrorResponse;
import org.eclipse.lsp4j.debug.Source;
import org.eclipse.lsp4j.debug.SourceBreakpoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for BreakpointManager. */
@SuppressWarnings("checkstyle:MagicNumber")
class BreakpointManagerTest {

  /**
   * VSCode runs from module directory, mvn runs from project directory.
   *
   * @return Proper {@link Path} to use.
   */
  private Path getPath(final String relativePath) {
    final Path path = Path.of(".").toAbsolutePath().getParent();
    if (path.endsWith("magik-debug-adapter")) {
      return Path.of("..").resolve(relativePath);
    }
    return Path.of(".").resolve(relativePath);
  }

  @BeforeEach
  void disableLogger() {
    final Logger logger = Logger.getLogger(BreakpointManager.class.getName());
    logger.setUseParentHandlers(false);
  }

  @Test
  void testAddBreakpoint() throws IOException, InterruptedException, ExecutionException {
    final TestSlapProtocol slapProtocol = new TestSlapProtocol();
    final BreakpointManager manager = new BreakpointManager(slapProtocol, null);

    final SourceBreakpoint sourceBreakpoint = new SourceBreakpoint();
    sourceBreakpoint.setLine(17);
    final Source source = new Source();
    source.setPath(getPath("magik-debug-adapter/src/test/resources/bpt.magik").toString());
    final BreakpointManager.MagikBreakpoint breakpoint =
        manager.addBreakpoint(source, sourceBreakpoint);
    assertThat(breakpoint.getMethodName()).isEqualTo("user:bpt.t()");
    assertThat(breakpoint.getMethodLine()).isZero();
    assertThat(breakpoint.getCondition()).isNull();
    assertThat(breakpoint.getBreakpointId()).isNotEqualTo(ISlapProtocol.INVALID_BREAKPOINT_ID);
  }

  @Test
  void testAddBreakpointInvalidLine() throws IOException, InterruptedException, ExecutionException {
    final TestSlapProtocol slapProtocol =
        new TestSlapProtocol() {
          @Override
          public CompletableFuture<ISlapResponse> setBreakpoint(String method, int line) {
            ErrorResponse errorResponse =
                new ErrorResponse(RequestType.BREAKPOINT_SET, ErrorMessage.INVALID_LINE_NUMBER);
            SlapErrorException exception = new SlapErrorException(errorResponse);

            CompletableFuture<ISlapResponse> future = new CompletableFuture<>();
            future.completeExceptionally(exception);
            return future;
          }
        };
    final BreakpointManager manager = new BreakpointManager(slapProtocol, null);

    final SourceBreakpoint sourceBreakpoint = new SourceBreakpoint();
    sourceBreakpoint.setLine(18);
    final Source source = new Source();
    source.setPath(getPath("magik-debug-adapter/src/test/resources/bpt.magik").toString());
    final BreakpointManager.MagikBreakpoint breakpoint =
        manager.addBreakpoint(source, sourceBreakpoint);
    assertThat(breakpoint.getMethodName()).isEqualTo("user:bpt.t()");
    assertThat(breakpoint.getMethodLine()).isEqualTo(18);
    assertThat(breakpoint.getCondition()).isNull();
    assertThat(breakpoint.getMessage()).isEqualTo("INVALID_LINE_NUMBER");
    assertThat(breakpoint.getBreakpointId()).isEqualTo(ISlapProtocol.INVALID_BREAKPOINT_ID);
  }

  @Test
  void testAddBreakpointMethodNotFound()
      throws IOException, InterruptedException, ExecutionException {
    final TestSlapProtocol slapProtocol =
        new TestSlapProtocol() {
          @Override
          public CompletableFuture<ISlapResponse> setBreakpoint(String method, int line) {
            final ErrorResponse errorResponse =
                new ErrorResponse(RequestType.BREAKPOINT_SET, ErrorMessage.METHOD_NOT_FOUND);
            final SlapErrorException exception = new SlapErrorException(errorResponse);

            final CompletableFuture<ISlapResponse> future = new CompletableFuture<>();
            future.completeExceptionally(exception);
            return future;
          }
        };
    final BreakpointManager manager = new BreakpointManager(slapProtocol, null);

    final SourceBreakpoint sourceBreakpoint = new SourceBreakpoint();
    sourceBreakpoint.setLine(18);
    final Source source = new Source();
    source.setPath(getPath("magik-debug-adapter/src/test/resources/bpt.magik").toString());
    final BreakpointManager.MagikBreakpoint breakpoint =
        manager.addBreakpoint(source, sourceBreakpoint);
    assertThat(breakpoint.getMethodName()).isEqualTo("user:bpt.t()");
    assertThat(breakpoint.getMethodLine()).isEqualTo(18);
    assertThat(breakpoint.getCondition()).isNull();
    assertThat(breakpoint.getMessage()).isEqualTo("METHOD_NOT_FOUND");
    assertThat(breakpoint.getBreakpointId()).isEqualTo(ISlapProtocol.INVALID_BREAKPOINT_ID);
  }
}
