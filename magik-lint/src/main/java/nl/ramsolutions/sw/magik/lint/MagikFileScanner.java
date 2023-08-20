package nl.ramsolutions.sw.magik.lint;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Magik file scanner.
 */
public final class MagikFileScanner {

    private MagikFileScanner() {
    }

    /**
     * Get files from args.
     * @param args Arguments.
     * @return Paths to files from arguments.
     * @throws IOException -
     */
    public static Collection<Path> getFilesFromArgs(final String[] args) throws IOException {
        final List<Path> files = new ArrayList<>();
        for (final String arg : args) {
            final Path path = Path.of(arg);
            final Collection<Path> filesFromArg = MagikFileScanner.scanMagikFiles(path);
            files.addAll(filesFromArg);
        }
        return files;
    }

    /**
     * Scan for .magik files from the start path.
     * @param start Path to scan from.
     * @return Collection with Paths to magik files.
     * @throws IOException -
     */
    public static Collection<Path> scanMagikFiles(final Path start) throws IOException {
        final List<Path> files = new ArrayList<>();

        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) {
                if (!dir.endsWith(".")
                    && dir.toFile().isHidden()) {
                    return FileVisitResult.SKIP_SUBTREE;
                }

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
                if (!attrs.isSymbolicLink()
                    && file.getFileName().toString().toLowerCase().endsWith(".magik")) {
                    files.add(file);
                }

                return FileVisitResult.CONTINUE;
            }
        });

        return files;
    }

}
