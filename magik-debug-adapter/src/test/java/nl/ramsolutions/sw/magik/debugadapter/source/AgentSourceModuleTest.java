package nl.ramsolutions.sw.magik.debugadapter.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import nl.ramsolutions.sw.magik.debugadapter.PathMapper;
import nl.ramsolutions.sw.magik.debugadapter.TestSlapProtocol;
import nl.ramsolutions.sw.magik.debugadapter.slap.ErrorMessage;
import nl.ramsolutions.sw.magik.debugadapter.slap.ISlapResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.RequestType;
import nl.ramsolutions.sw.magik.debugadapter.slap.SlapErrorException;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.ErrorResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.SourceFileResponse;
import org.junit.jupiter.api.Test;

/** Tests for {@link AgentSourceModule}. */
class AgentSourceModuleTest {

  /** Answers SOURCE_FILE with a fixed failure or filename, and records the methods asked. */
  private static final class SourceFileSlapProtocol extends TestSlapProtocol {

    private final ErrorMessage errorMessage;
    private final String filename;
    private final List<String> methods = new ArrayList<>();

    private SourceFileSlapProtocol(final ErrorMessage errorMessage, final String filename) {
      this.errorMessage = errorMessage;
      this.filename = filename;
    }

    @Override
    public CompletableFuture<ISlapResponse> getSourceFile(final String method) {
      this.methods.add(method);
      if (this.errorMessage != null) {
        final ErrorResponse error = new ErrorResponse(RequestType.SOURCE_FILE, this.errorMessage);
        final SlapErrorException exception = new SlapErrorException(error);
        return CompletableFuture.failedFuture(exception);
      }

      final SourceFileResponse response = new SourceFileResponse(this.filename);
      return CompletableFuture.completedFuture(response);
    }

    private List<String> getMethodsForTest() {
      return this.methods;
    }
  }

  @Test
  void testAgentSourceModuleMapsTheAgentsPath() throws Exception {
    final SourceFileSlapProtocol slapProtocol = new SourceFileSlapProtocol(null, "/srv/sw/a.magik");
    final Map<Path, Path> mapping = Map.of(Path.of("/srv/sw"), Path.of("/home/me/sw"));
    final PathMapper pathMapper = new PathMapper(mapping);
    final AgentSourceModule module = new AgentSourceModule(slapProtocol, pathMapper);

    final Optional<Path> path = module.tryResolve("user:a.m()");

    assertThat(path).contains(Path.of("/home/me/sw/a.magik"));
    final List<String> methods = slapProtocol.getMethodsForTest();
    assertThat(methods).containsExactly("user:a.m()");
  }

  @Test
  void testMethodNotFoundAnswersEmpty() throws Exception {
    final SourceFileSlapProtocol slapProtocol =
        new SourceFileSlapProtocol(ErrorMessage.METHOD_NOT_FOUND, null);
    final PathMapper pathMapper = new PathMapper(Map.of());
    final AgentSourceModule module = new AgentSourceModule(slapProtocol, pathMapper);

    final Optional<Path> path = module.tryResolve("user:a.m()");

    assertThat(path).isEmpty();
  }

  @Test
  void testOtherAgentErrorsAreRethrown() {
    final SourceFileSlapProtocol slapProtocol =
        new SourceFileSlapProtocol(ErrorMessage.THREAD_NOT_SUSPENDED, null);
    final PathMapper pathMapper = new PathMapper(Map.of());
    final AgentSourceModule module = new AgentSourceModule(slapProtocol, pathMapper);

    final Throwable thrown = catchThrowable(() -> module.tryResolve("user:a.m()"));

    assertThat(thrown).isInstanceOf(ExecutionException.class);
  }
}
