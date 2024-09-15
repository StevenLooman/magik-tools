package nl.ramsolutions.sw.magik;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import nl.ramsolutions.sw.IgnoreHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Magik file scanner. */
public final class MagikFileScanner {

  private static final Logger LOGGER = LoggerFactory.getLogger(MagikFileScanner.class);
  private static final long MAX_SIZE = 1024L * 1024L * 10L; // 10 MB
  private final IgnoreHandler ignoreHandler;

  public MagikFileScanner(final IgnoreHandler ignoreHandler) {
    this.ignoreHandler = ignoreHandler;
  }

  /**
   * Get the magik files from the given path.
   *
   * @param fromPath Path to walk from, most likely a directory.
   * @return Stream of paths to magik files.
   * @throws IOException -
   */
  public Stream<Path> getFiles(final Path fromPath) throws IOException {
    return Files.walk(fromPath)
        .filter(Files::isRegularFile)
        .filter(this::notIgnored)
        .filter(this::sizeOk)
        .filter(this::isMagikFile);
  }

  private boolean notIgnored(final Path path) {
    return !this.ignoreHandler.isIgnored(path);
  }

  private boolean sizeOk(final Path path) {
    try {
      final long size = Files.size(path);
      if (size > MagikFileScanner.MAX_SIZE) {
        LOGGER.warn(
            "Ignoring file: {}, due to size: {}, max size: {}",
            path,
            size,
            MagikFileScanner.MAX_SIZE);
        return false;
      }
    } catch (final IOException exception) {
      throw new IllegalStateException(exception);
    }

    return true;
  }

  private boolean isMagikFile(final Path path) {
    final String fileName = path.getFileName().toString().toLowerCase();
    return !fileName.startsWith(".") && !fileName.startsWith("#") && fileName.endsWith(".magik");
  }
}
