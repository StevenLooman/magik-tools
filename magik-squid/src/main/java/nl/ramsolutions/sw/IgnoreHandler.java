package nl.ramsolutions.sw;

import java.io.IOException;
import java.net.URI;
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
import nl.ramsolutions.sw.magik.FileEvent;
import nl.ramsolutions.sw.magik.FileEvent.FileChangeType;
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

  private final Map<Path, Set<PathMatcher>> entries = new HashMap<>();

  /**
   * Constructor.
   *
   * @param fileEvent File event.
   * @throws IOException -
   */
  public void handleFileEvent(final FileEvent fileEvent) throws IOException {
    final URI uri = fileEvent.getUri();
    final Path path = Path.of(uri);
    final FileChangeType fileChangeType = fileEvent.getFileChangeType();
    if (fileChangeType == FileChangeType.DELETED) {
      this.getIndexableFiles(path)
          .filter(indexablePath -> indexablePath.toString().equalsIgnoreCase(IGNORE_FILENAME))
          .forEach(this::removeIgnoreFile);
    } else {
      this.getIndexableFiles(path)
          .filter(indexablePath -> indexablePath.toString().equalsIgnoreCase(IGNORE_FILENAME))
          .forEach(this::addIgnoreFile);
    }
  }

  /**
   * Get all (indexable) files, under {@link fromPath}, which are not ignored.
   *
   * @param fromPath Path to walk from, most likely a directory.
   * @return Stream of indexable files.
   * @throws IOException -
   */
  public Stream<Path> getIndexableFiles(final Path fromPath) throws IOException {
    return Files.walk(fromPath).filter(path -> !this.isIgnored(path));
  }

  /**
   * Add a found .magik-tools-ignore file. Reads entries and stores these.
   *
   * <p>Will raise an IllegalArgumentException exception when any file with a name other than
   * {@literal .magik-tools-ignore} is added.
   *
   * @param path Path to .magik-tools-ignore to add.
   */
  public void addIgnoreFile(final Path path) {
    LOGGER.debug("Add ignore file: {}", path);
    if (!path.getFileSystem().getPathMatcher("glob:**/" + IGNORE_FILENAME).matches(path)) {
      throw new IllegalArgumentException();
    }

    final Path parentPath = path.getParent();
    if (parentPath == null) {
      throw new IllegalArgumentException();
    }

    final Path basePath = parentPath.toAbsolutePath();
    final FileSystem fileSystem = path.getFileSystem();
    try (Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8)) {
      final Set<PathMatcher> pathMatchers =
          lines
              .map(String::trim)
              .filter(line -> !line.isBlank())
              .filter(line -> !line.startsWith("#")) // Comments.
              .map(
                  line ->
                      (basePath.toString() + fileSystem.getSeparator() + line)
                          .replace("\\", "\\\\"))
              .map(pattern -> fileSystem.getPathMatcher("glob:" + pattern))
              .collect(Collectors.toSet());
      this.entries.put(path, pathMatchers);
    } catch (final IOException exception) {
      LOGGER.error("Error indexing created file: " + path, exception);
    }
  }

  /**
   * Remove .magik-tools-ignore file.
   *
   * <p>Will raise an IllegalArgumentException exception when any file with a name other than
   * {@literal .magik-tools-ignore} is removed.
   *
   * @param path Path to removed {@literal .magik-tools-ignore} file.
   */
  public void removeIgnoreFile(final Path path) {
    LOGGER.debug("Remove ignore file: {}", path);
    if (!path.getFileSystem().getPathMatcher("glob:**/.magik-tools-ignore").matches(path)) {
      throw new IllegalArgumentException();
    }

    this.entries.remove(path);
  }

  /**
   * Test if {@code path} is ignored.
   *
   * <p>A file is either ignored when: - It is a non-regular file - Starts with {@literal .} -
   * Starts with {@literal #} - Is not a {@literal .magik} file - It is matched through a {@literal
   * .magik-tools-ignore} file
   *
   * @param path Path to check.
   * @return true if ignored, false otherwise.
   */
  public boolean isIgnored(final Path path) {
    // Try defaults first.
    final Path filename = path.getFileName();
    if (filename.startsWith(".") || filename.startsWith("#")) {
      return true;
    }

    // Consult all ignore files and check for a match.
    return this.entries.entrySet().stream()
        .flatMap(entry -> entry.getValue().stream())
        .anyMatch(pathMatcher -> pathMatcher.matches(path));
  }
}
