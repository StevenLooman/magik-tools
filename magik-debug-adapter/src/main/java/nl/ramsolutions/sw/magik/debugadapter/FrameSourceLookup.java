package nl.ramsolutions.sw.magik.debugadapter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import nl.ramsolutions.sw.magik.debugadapter.breakpoints.SourceLookup;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.ThreadStackResponse;

/** Source lookup for the breakpoints package, backed by the frame source resolver. */
class FrameSourceLookup implements SourceLookup {

  private final FrameSourceResolver sourceResolver;
  private final PathMapper pathMapper;

  /**
   * Constructor.
   *
   * @param sourceResolver Resolves the source file of a stack frame.
   * @param pathMapper Path mapping.
   */
  FrameSourceLookup(final FrameSourceResolver sourceResolver, final PathMapper pathMapper) {
    this.sourceResolver = sourceResolver;
    this.pathMapper = pathMapper;
  }

  @Override
  public Path findSource(final long threadId, final ThreadStackResponse.StackElement frame)
      throws IOException, InterruptedException, ExecutionException {
    return this.sourceResolver.determinePath(threadId, frame);
  }

  @Override
  public Path mapPath(final Path agentPath) {
    return this.pathMapper.applyMapping(agentPath);
  }
}
