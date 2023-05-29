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
 * <p>
 * Keeps track of added {@literal .magik-tools-ignore} files and determines whether a file should be ignored.
 * </p>
 */
public final class IgnoreHandler {

    private static final String IGNORE_FILENAME = ".magik-tools-ignore";

    private static final Logger LOGGER = LoggerFactory.getLogger(IgnoreHandler.class);

    private final Map<Path, Set<PathMatcher>> entries = new HashMap<>();

    /**
     * Add a found .magik-tools-ignore file. Reads entries and stores these.
     *
     * <p>
     * Will raise an IllegalArgumentException exception when any file with a name
     * other than {@literal .magik-tools-ignore} is added.
     * </p>
     * @param path Path to .magik-tools-ignore to add.
     */
    public void addIgnoreFile(final Path path) throws IOException {
        LOGGER.debug("Add ignore file: {}", path);
        if (!path.getFileSystem().getPathMatcher("glob:**/" + IGNORE_FILENAME).matches(path)) {
            throw new IllegalArgumentException();
        }

        final Path basePath = path.getParent().toAbsolutePath();
        final FileSystem fileSystem = path.getFileSystem();
        try (Stream<String> lines = Files.lines(path, StandardCharsets.ISO_8859_1)) {
            final Set<PathMatcher> pathMatchers = lines
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .filter(line -> !line.startsWith("#"))  // Comments.
                .map(line -> (basePath.toString() + fileSystem.getSeparator() + line).replace("\\", "\\\\"))
                // .peek(pattern -> LOGGER.debug("Using pattern: glob:{}", pattern))
                .map(pattern -> fileSystem.getPathMatcher("glob:" + pattern))
                .collect(Collectors.toSet());
            this.entries.put(path, pathMatchers);
        }
    }

    /**
     * Remove .magik-tools-ignore file.
     *
     * <p>
     * Will raise an IllegalArgumentException exception when any file with a name
     * other than {@literal .magik-tools-ignore} is removed.
     * </p>
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
     * <p>
     * A file is either ignored when:
     * - It is a non-regular file
     * - Starts with {@literal .}
     * - Starts with {@literal #}
     * - Is not a {@literal .magik} file
     * - It is matched through a {@literal .magik-tools-ignore} file
     * </p>
     * @param path Path to check.
     * @return true if ignored, false otherwise.
     */
    public boolean isIgnored(final Path path) {
        // Try defaults first.
        final Path filename = path.getFileName();
        final FileSystem fileSystem = path.getFileSystem();
        final PathMatcher matcher = fileSystem.getPathMatcher("glob:**/*.magik");
        if (!Files.isRegularFile(path)
            || filename.startsWith(".")
            || filename.startsWith("#")
            || !matcher.matches(path)) {
            return true;
        }

        // Consult all ignore files and check for a match.
        return this.entries.entrySet().stream()
            .flatMap(entry -> entry.getValue().stream())
            .anyMatch(pathMatcher -> pathMatcher.matches(path));
    }

}
