package nl.ramsolutions.sw;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * .magik-tools-ignore file handler.
 *
 * <p>Keeps track of added {@literal .magik-tools-ignore} files and determines whether a file should
 * be ignored.
 */
public final class IgnoreHandler {

  private static final String IGNORE_FILENAME = ".magik-tools-ignore";
  private static final Logger LOGGER = LoggerFactory.getLogger(IgnoreHandler.class);

  private final Map<Path, Set<PathMatcher>> cache = new HashMap<>();

  public void reset() {
    this.cache.clear();
  }

  /**
   * Add a found .magik-tools-ignore file. Reads entries and stores these.
   *
   * <p>Will raise an IllegalArgumentException exception when any file with a name other than
   * {@literal .magik-tools-ignore} is added.
   *
   * @param path Path to .magik-tools-ignore to add.
   */
  private Set<PathMatcher> readIgnoreFile(final Path path) {
    LOGGER.debug("Add ignore file: {}", path);
    final FileSystem fileSystem = path.getFileSystem();
    if (!fileSystem.getPathMatcher("glob:**/" + IGNORE_FILENAME).matches(path)) {
      throw new IllegalArgumentException();
    }

    final Path parentPath = path.getParent();
    if (parentPath == null) {
      throw new IllegalArgumentException();
    }

    final Path basePath = parentPath.toAbsolutePath();
    try (Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8)) {
      return lines
          .map(String::trim)
          .filter(line -> !line.isBlank())
          .filter(line -> !line.startsWith("#")) // Comments.
          .map(
              line ->
                  (basePath.toString() + fileSystem.getSeparator() + line).replace("\\", "\\\\"))
          .map(pattern -> fileSystem.getPathMatcher("glob:" + pattern))
          .collect(Collectors.toSet());
    } catch (final IOException exception) {
      throw new IllegalStateException(exception);
    }
  }

  private Set<PathMatcher> getMatchers(final Path path) {
    // In cache, return it.
    if (this.cache.containsKey(path)) {
      return this.cache.get(path);
    }

    // Find ignore file.
    final Path ignorePath = path.resolve(IGNORE_FILENAME);
    if (Files.exists(ignorePath)) {
      final Set<PathMatcher> matchers = this.readIgnoreFile(ignorePath);
      this.cache.put(path, matchers);
      return matchers;
    }

    // Iterate upwards to find ignore file.
    final Path parentPath = path.getParent();
    if (parentPath == null) {
      return Set.of();
    }

    return this.getMatchers(parentPath);
  }

  /**
   * Test if {@code path} is ignored via a {@literal .magik-tools-ignore} file.
   *
   * @param path Path to check.
   * @return true if ignored, false otherwise.
   */
  public boolean isIgnored(final Path path) {
    final Set<PathMatcher> matchers = this.getMatchers(path);
    return matchers.stream().anyMatch(pathMatcher -> pathMatcher.matches(path));
  }
}
