package nl.ramsolutions.sw.magik.debugadapter.source;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/** Resolves the source file of a method. */
public interface SourceModule {

  /**
   * Try to resolve the source file of a method.
   *
   * @param qualifiedMethodName Package-qualified method name, without spaces.
   * @return Mapped path to the source file, or empty if this module does not know the method.
   * @throws IOException -
   * @throws InterruptedException -
   * @throws ExecutionException -
   */
  Optional<Path> tryResolve(String qualifiedMethodName)
      throws IOException, InterruptedException, ExecutionException;
}
