package nl.ramsolutions.sw.magik.analysis.typing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import nl.ramsolutions.sw.magik.analysis.typing.types.MagikType;
import nl.ramsolutions.sw.magik.analysis.typing.types.Method;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads class info from generated libraries. E.g.,
 * core/sw_core/libs/sw_core.emailer.1.jar/class_info
 */
public final class ClassInfoTypeKeeperReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClassInfoTypeKeeperReader.class);

    private static final String NEXT_NOT_SLASH_PATTERN = "[^/]+";

    private final Path path;
    private final ITypeKeeper typeKeeper;

    /**
     * Constructor.
     * @param path Path to jar file.
     * @param typeKeeper Typekeeper to fill.
     */
    private ClassInfoTypeKeeperReader(final Path path, final ITypeKeeper typeKeeper) {
        this.path = path;
        this.typeKeeper = typeKeeper;
    }

    private void run() throws IOException {
        try (ZipFile zipFile = new ZipFile(this.path.toFile())) {
            final ZipEntry zipEntry = zipFile.getEntry("class_info");
            if (zipEntry == null) {
                return;
            }

            try (InputStream stream = zipFile.getInputStream(zipEntry);
                 InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.ISO_8859_1)) {
                this.parseClassInfo(reader);
            }
        }
    }

    private void parseClassInfo(final InputStreamReader streamReader) throws IOException {
        try (LineNumberReader reader = new LineNumberReader(streamReader)) {
            String line = reader.readLine();
            while (line != null) {
                final String[] tokens = line.split(" ");
                final String token = tokens.length > 0 ? tokens[0] : "";
                switch (token) {
                    case "method":
                        this.readMethod(line, reader);
                        break;

                    case "slotted_class":
                        this.readSlottedClass(line, reader);
                        break;

                    case "mixin":
                        this.readMixin(line, reader);
                        break;

                    default:
                        LOGGER.warn("Unknown token: {}", token);
                        throw new UnsupportedOperationException("Unknown token: " + token);
                }
                line = reader.readLine();
            }
        }
    }

    private void readMethod(final String line, final BufferedReader reader) throws IOException {
        // 1 : "method" <class name> <method name> <parameters> <-- possibly no assignment parameter
        // 2 : n ["private"/"classconst"/"classvar"/"iter"]*
        //       ["basic"/"restricted"/"internal"/pragma]* source_file
        // 3+: <n lines of comments>
        // Line 1
        // Ignore <global> and <condition> for now.
        final MagikType type;
        final String methodName;
        try (Scanner scanner = new Scanner(line)) {
            scanner.next(); // "method"

            final String className = scanner.next();
            if (!"<global>".equals(className)
                && !"<condition>".equals(className)) {
                final TypeString typeString = TypeString.ofIdentifier(className, TypeString.DEFAULT_PACKAGE);
                methodName = scanner.next();
                if (!this.typeKeeper.hasType(typeString)) {
                    LOGGER.debug("Type not found: {}, for method: {}", className, methodName);
                    type = null;
                } else {
                    type = (MagikType) this.typeKeeper.getType(typeString);
                }

                // Skip parameters.
                scanner.nextLine();
            } else {
                scanner.nextLine(); // Skip line
                type = null;
                methodName = null;
            }
        }

        // Line 2
        int commentLineCount = 0;
        try (Scanner scanner = new Scanner(reader.readLine())) {
            commentLineCount = scanner.nextInt();

            // Read pragmas.
            final List<String> pragmas = new ArrayList<>();
            final List<String> skipList = List.of("private", "classconst", "classvar", "iter");
            while (scanner.hasNext(NEXT_NOT_SLASH_PATTERN)) {
                final String pragma = scanner.next();
                if (skipList.contains(pragma)) {
                    continue;
                }
                pragmas.add(pragma);
            }

            // Skip path.
            scanner.nextLine();  // NOSONAR
        }

        // Line 3+
        final StringBuilder commentBuilder = new StringBuilder();
        for (int i = 0; i < commentLineCount; ++i) {
            final String commentLine = reader.readLine();
            commentBuilder.append(commentLine);
            commentBuilder.append('\n');
        }
        final String comment = commentBuilder.toString();
        if (type != null) {
            for (final Method method : type.getMethods(methodName)) {
                method.setDoc(comment);
            }
        }

        reader.readLine();  // NOSONAR
    }

    private void readSlottedClass(final String line, final BufferedReader reader) throws IOException {
        // 1 : "slotted_class" <class name> <slots>    <-- also slots of inherited exemplars
        // 2 : <base classes>
        // 3 : n pragma source_file
        // 4+: <n lines of comments>
        // Line 1
        final MagikType type;
        try (Scanner scanner = new Scanner(line)) {
            scanner.next(); // "slotted_class"

            final String identifier = scanner.next();
            final TypeString typeString = TypeString.ofIdentifier(identifier, TypeString.DEFAULT_PACKAGE);
            if (!this.typeKeeper.hasType(typeString)) {
                LOGGER.debug("Type not found: {}", identifier);  // NOSONAR
                type = null;
            } else {
                type = (MagikType) this.typeKeeper.getType(typeString);
            }

            scanner.nextLine(); // skip slots.
        }

        // Line 2
        final String parentClassesLine = reader.readLine();
        final String[] parentClasses = parentClassesLine.split(" ");
        for (final String parentClass : parentClasses) {
            final TypeString parentTypeString = TypeString.ofIdentifier(parentClass, TypeString.DEFAULT_PACKAGE);
            if (!this.typeKeeper.hasType(parentTypeString)) {
                LOGGER.debug("Type not found: {}", parentClass);  // NOSONAR
            }
        }

        // Line 3
        int commentLineCount = 0;
        try (Scanner scanner = new Scanner(reader.readLine())) {
            commentLineCount = scanner.nextInt();

            // Pragmas.
            final List<String> pragmas = new ArrayList<>();
            while (scanner.hasNext(NEXT_NOT_SLASH_PATTERN)) {
                final String pragma = scanner.next();
                pragmas.add(pragma);
            }

            // Skip path
            scanner.nextLine();
        }

        // Line 4+
        final StringBuilder commentBuilder = new StringBuilder();
        for (int i = 0; i < commentLineCount; ++i) {
            final String commentLine = reader.readLine();
            commentBuilder.append(commentLine);
            commentBuilder.append('\n');
        }
        if (type != null) {
            final String comment = commentBuilder.toString();
            type.setDoc(comment);
        }

        reader.readLine();  // NOSONAR
    }

    private void readMixin(final String line, final BufferedReader reader) throws IOException {
        // 1 : "mixin" <class name>
        // 2 : "."
        // 3 : n pragma source_file
        // 4+: <n lines of comments>
        // Line 1
        final MagikType type;
        try (Scanner scanner = new Scanner(line)) {
            scanner.next(); // "mixin"

            final String identifier = scanner.next();
            final TypeString typeString = TypeString.ofIdentifier(identifier, TypeString.DEFAULT_PACKAGE);
            if (!this.typeKeeper.hasType(typeString)) {
                LOGGER.debug("Type not found: {}", identifier);  // NOSONAR
                type = null;
            } else {
                type = (MagikType) this.typeKeeper.getType(typeString);
            }

            scanner.nextLine();
        }

        // Line 2
        reader.readLine();  // NOSONAR; skip "."

        // Line 3
        final int commentLineCount;
        try (Scanner scanner = new Scanner(reader.readLine())) {
            commentLineCount = scanner.nextInt();

            // Read pragmas.
            final List<String> pragmas = new ArrayList<>();
            while (scanner.hasNext(NEXT_NOT_SLASH_PATTERN)) {
                final String pragma = scanner.next();
                pragmas.add(pragma);
            }

            // Skip path
            scanner.nextLine();
        }

        // Line 4+
        final StringBuilder commentBuilder = new StringBuilder();
        for (int i = 0; i < commentLineCount; ++i) {
            final String commentLine = reader.readLine();
            commentBuilder.append(commentLine);
            commentBuilder.append('\n');
        }
        if (type != null) {
            final String comment = commentBuilder.toString();
            type.setDoc(comment);
        }

        reader.readLine();  // NOSONAR
    }

    /**
     * Read types from a jar/class_info file.
     *
     * @param path             Path to jar file.
     * @param typeKeeper {@link TypeKeeper} to fill.
     * @throws IOException -
     */
    public static void readTypes(final Path path, final ITypeKeeper typeKeeper) throws IOException {
        final ClassInfoTypeKeeperReader reader = new ClassInfoTypeKeeperReader(path, typeKeeper);
        reader.run();
    }

    /**
     * Read libs directory.
     * @param libsPath Path to libs directory.
     * @param typeKeeper {@link TypeKeeper} to fill.
     * @throws IOException -
     */
    public static void readLibsDirectory(final Path libsPath, final ITypeKeeper typeKeeper) throws IOException {
        try (Stream<Path> paths = Files.list(libsPath)) {
            paths
                .filter(Files::isRegularFile)
                .forEach(libPath -> {
                    LOGGER.trace("Reading lib: {}", libPath);
                    try {
                        ClassInfoTypeKeeperReader.readTypes(libPath, typeKeeper);
                    } catch (IOException exception) {
                        LOGGER.error(exception.getMessage(), exception);
                    }
                });
        } catch (IOException exception) {
            LOGGER.error(exception.getMessage(), exception);
        }
    }

}
