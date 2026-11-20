package nl.ramsolutions.sw.magik.debugadapter.breakpoints;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.ThreadStackResponse;

/** Finds the source file of a stack frame, and maps the paths of the agent. */
public interface SourceLookup {

  /**
   * Find the source file of a stack frame.
   *
   * @param threadId Thread ID.
   * @param frame Stack frame of Magik code. The answer for a frame of another language is not
   *     defined.
   * @return Mapped path to the source file, or null for a frame without a source file, such as a
   *     loop body or a procedure.
   * @throws IOException -
   * @throws InterruptedException -
   * @throws ExecutionException -
   */
  @CheckForNull
  Path findSource(long threadId, ThreadStackResponse.StackElement frame)
      throws IOException, InterruptedException, ExecutionException;

  /**
   * Map a path of the agent to a path of this machine.
   *
   * @param agentPath Path as the agent knows it.
   * @return Mapped path, or the path itself if no mapping applies.
   */
  Path mapPath(Path agentPath);
}
