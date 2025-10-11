package nl.ramsolutions.sw;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Source file scanner. */
public class SourceFileScanner {

  public static final String SW_MODULE_DEF = "module.def";
  public static final String SW_PRODUCT_DEF = "product.def";

  public static final Predicate<Path> MAGIK_FILE_FILTER =
      path -> {
        final String fileName = path.getFileName().toString().toLowerCase();
        return !fileName.startsWith(".")
            && !fileName.startsWith("#")
            && fileName.endsWith(".magik");
      };
  public static final Predicate<Path> MODULE_DEF_FILE_FILTER =
      path -> path.getFileName().toString().equalsIgnoreCase("module.def");
  public static final Predicate<Path> PRODUCT_DEF_FILE_FILTER =
      path -> path.getFileName().toString().equalsIgnoreCase("product.def");
  public static final Predicate<Path> MAGIK_OR_PRODUCT_DEF =
      MAGIK_FILE_FILTER.or(PRODUCT_DEF_FILE_FILTER);
  public static final Predicate<Path> ANY_MAGIK_RELATED_FILE_FILTER =
      MAGIK_FILE_FILTER.or(MODULE_DEF_FILE_FILTER).or(PRODUCT_DEF_FILE_FILTER);

  private static final Logger LOGGER = LoggerFactory.getLogger(SourceFileScanner.class);
  private static final long MAX_SIZE = 1024L * 1024L * 10L; // 10 MB
  private final Predicate<Path> filter;
  private final IgnoreHandler ignoreHandler;

  public SourceFileScanner(final IgnoreHandler ignoreHandler, Predicate<Path> filter) {
    this.filter = filter;
    this.ignoreHandler = ignoreHandler;
  }

  /**
   * Get the filtered files from the given path.
   *
   * @param fromPath Path to walk from, most likely a directory.
   * @return Stream of paths to filtered files.
   * @throws IOException -
   */
  public Stream<Path> getFiles(final Path fromPath) throws IOException {
    return Files.walk(fromPath)
        .filter(Files::isRegularFile)
        .filter(this::notIgnored)
        .filter(this::sizeOk)
        .filter(this.filter);
  }

  /**
   * Search for a file with the given name upwards from the given path.
   *
   * @param startPath Path to start at.
   * @param searchedFilename Filename to search for.
   * @return Path to the found file, or null if not found.
   */
  @CheckForNull
  public static Path searchFileUpwards(final Path startPath, final String searchedFilename) {
    Path path = startPath;
    while (path != null) {
      final Path resolvedPath = path.resolve(searchedFilename);
      if (Files.exists(resolvedPath)) {
        return resolvedPath;
      }

      path = path.getParent();
    }

    return null;
  }

  private boolean notIgnored(final Path path) {
    return !this.ignoreHandler.isIgnored(path);
  }

  private boolean sizeOk(final Path path) {
    try {
      final long size = Files.size(path);
      if (size > SourceFileScanner.MAX_SIZE) {
        LOGGER.warn(
            "Ignoring file: {}, due to size: {}, max size: {}",
            path,
            size,
            SourceFileScanner.MAX_SIZE);
        return false;
      }
    } catch (final IOException exception) {
      throw new IllegalStateException(exception);
    }

    return true;
  }
}
