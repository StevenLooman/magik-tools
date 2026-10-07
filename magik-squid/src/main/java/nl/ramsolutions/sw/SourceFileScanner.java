package nl.ramsolutions.sw;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemLoopException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Source file scanner. */
public class SourceFileScanner {

  public static final String SW_PRODUCT_DEF = "product.def";
  public static final String SW_MODULE_DEF = "module.def";
  public static final String SW_LOAD_LIST = "load_list.txt";
  public static final String SW_PATCH_LIST = "patch_list.txt";

  public static final Predicate<Path> MAGIK_FILE_FILTER =
      path -> {
        final String fileName = path.getFileName().toString().toLowerCase();
        return !fileName.startsWith(".")
            && !fileName.startsWith("#")
            && fileName.endsWith(".magik");
      };
  public static final Predicate<Path> MODULE_DEF_FILE_FILTER =
      path -> path.getFileName().toString().equalsIgnoreCase(SW_MODULE_DEF);
  public static final Predicate<Path> PRODUCT_DEF_FILE_FILTER =
      path -> path.getFileName().toString().equalsIgnoreCase(SW_PRODUCT_DEF);
  public static final Predicate<Path> LOAD_LIST_FILE_FILTER =
      path -> {
        final String fileName = path.getFileName().toString().toLowerCase();
        return fileName.equals(SW_LOAD_LIST) || fileName.equals(SW_PATCH_LIST);
      };
  public static final Predicate<Path> ANY_MAGIK_RELATED_FILE_FILTER =
      MAGIK_FILE_FILTER
          .or(MODULE_DEF_FILE_FILTER)
          .or(PRODUCT_DEF_FILE_FILTER)
          .or(LOAD_LIST_FILE_FILTER);

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
   * <p>Symbolic links are followed: {@code fromPath} may itself be a link to a directory, and links
   * to directories inside the tree are descended into. The returned paths are expressed through
   * {@code fromPath} as given (and through the link names inside the tree), not as real paths.
   *
   * <p>A link that leads back into a directory it is being walked from (a link to one of its own
   * ancestors, or one link of a mutual pair) is skipped with one warning for that link, and every
   * other file is still returned; with a mutual pair, each file in the pair is returned once per
   * route. A dangling link inside the tree, including a link to itself, is skipped silently. A file
   * reachable by more than one route, such as through two links to one directory, is returned once
   * per route. A directory inside the tree that cannot be read is skipped with a warning. A {@code
   * fromPath} that does not exist (including a dangling link) or cannot be read raises an {@link
   * IOException}.
   *
   * @param fromPath Path to walk from, most likely a directory.
   * @return Stream of paths to filtered files.
   * @throws IOException -
   */
  public Stream<Path> getFiles(final Path fromPath) throws IOException {
    if (!Files.exists(fromPath)) {
      throw new NoSuchFileException(fromPath.toString());
    }

    final List<Path> files = new ArrayList<>();
    final FileVisitor<Path> visitor =
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
            if (SourceFileScanner.this.isWantedFile(file)) {
              files.add(file);
            }
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFileFailed(final Path file, final IOException exception)
              throws IOException {
            if (exception instanceof FileSystemLoopException) {
              LOGGER.warn("Ignoring link loop: {}", file);
              return FileVisitResult.CONTINUE;
            }
            if (exception instanceof NoSuchFileException) {
              return FileVisitResult.CONTINUE;
            }
            if (exception instanceof AccessDeniedException && !file.equals(fromPath)) {
              LOGGER.warn("Ignoring unreadable path: {}", file);
              return FileVisitResult.CONTINUE;
            }
            throw exception;
          }
        };
    Files.walkFileTree(
        fromPath, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, visitor);
    return files.stream();
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

  private boolean isWantedFile(final Path path) {
    return Files.isRegularFile(path)
        && this.notIgnored(path)
        && this.sizeOk(path)
        && this.filter.test(path);
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
