package nl.ramsolutions.sw.magik.debugadapter.source;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import nl.ramsolutions.sw.magik.debugadapter.PathMapper;
import nl.ramsolutions.sw.magik.debugadapter.slap.ISlapProtocol;
import nl.ramsolutions.sw.magik.debugadapter.slap.ISlapResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.SlapErrorException;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.SourceFileResponse;

/** Resolves the source file of a method by asking the agent, and maps the answer. */
public class AgentSourceModule implements SourceModule {

  private final ISlapProtocol slapProtocol;
  private final PathMapper pathMapper;

  /**
   * Constructor.
   *
   * @param slapProtocol Protocol to the agent.
   * @param pathMapper Path mapping.
   */
  public AgentSourceModule(final ISlapProtocol slapProtocol, final PathMapper pathMapper) {
    this.slapProtocol = slapProtocol;
    this.pathMapper = pathMapper;
  }

  @Override
  public Optional<Path> tryResolve(final String qualifiedMethodName)
      throws IOException, InterruptedException, ExecutionException {
    try {
      final CompletableFuture<ISlapResponse> sourceFileFuture =
          this.slapProtocol.getSourceFile(qualifiedMethodName);
      final SourceFileResponse sourceFile = (SourceFileResponse) sourceFileFuture.get();
      final String filename = sourceFile.getFilename();
      final Path agentPath = Path.of(filename);
      final Path path = this.pathMapper.applyMapping(agentPath);
      return Optional.of(path);
    } catch (final ExecutionException exception) {
      SlapErrorException.rethrowUnlessMethodNotFound(exception);
      return Optional.empty();
    }
  }
}
