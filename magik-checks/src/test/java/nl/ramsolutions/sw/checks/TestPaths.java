package nl.ramsolutions.sw.checks;

import java.net.URI;
import java.nio.file.Path;

/**
 * Test resource paths, resolved against whichever directory the tests are run from: VSCode runs
 * from the module directory, maven runs from the project directory.
 */
public final class TestPaths {

  private TestPaths() {}

  /**
   * Resolve a project-relative path against the current working directory.
   *
   * @param projectRelativePath Path relative to the project directory.
   * @return Resolved {@link Path}.
   */
  public static Path createPath(final Path projectRelativePath) {
    final Path currentPath = Path.of(".").toAbsolutePath().getParent();
    final Path basePath = currentPath.endsWith("magik-checks") ? Path.of("..") : Path.of(".");
    return basePath.resolve(projectRelativePath);
  }

  /**
   * Resolve a project-relative path against the current working directory, as a file-URI.
   *
   * @param projectRelativePath Path relative to the project directory.
   * @return Resolved file-{@link URI}.
   */
  public static URI createUri(final Path projectRelativePath) {
    final Path path = TestPaths.createPath(projectRelativePath);
    return path.toUri();
  }
}
