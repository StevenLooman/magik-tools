package nl.ramsolutions.sw.definitions;

import com.sonar.sslr.api.AstNode;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.CheckForNull;
import nl.ramsolutions.sw.definitions.api.SwModuleDefGrammar;
import nl.ramsolutions.sw.definitions.parser.SwModuleDefParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * module.def file scanner.
 *
 * <p>
 * Scans for module.def files. Aborts scanning when a different product is found.
 * </p>
 */
public final class SwModuleScanner {

    private static class ModuleDefFileVisitor extends SimpleFileVisitor<Path> {

        private Set<SwModule> modules = new HashSet<>();
        private final Path startPath;

        ModuleDefFileVisitor(final Path startPath) {
            this.startPath = startPath;
        }

        public Set<SwModule> getModules() {
            return this.modules;
        }

        @Override
        public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
            final Path productDefPath = dir.resolve(SW_PRODUCT_DEF_PATH);
            if (!dir.equals(this.startPath)
                && Files.exists(productDefPath)) {
                // Don't scan in child products.
                return FileVisitResult.SKIP_SUBTREE;
            }

            final Path moduleDefPath = dir.resolve(SW_MODULE_DEF_PATH);
            if (Files.exists(moduleDefPath)) {
                this.addModule(moduleDefPath);
            }
            return FileVisitResult.CONTINUE;
        }

        private void addModule(final Path path) {
            try {
                SwModule swModule = SwModuleScanner.readModuleDefinition(path);
                this.modules.add(swModule);
            } catch (IOException exception) {
                LOGGER.error(exception.getMessage(), exception);
            }
        }

    }

    private static final Logger LOGGER = LoggerFactory.getLogger(SwModuleScanner.class);

    private static final Path SW_MODULE_DEF_PATH = Path.of(SwModule.SW_MODULE_DEF);
    private static final Path SW_PRODUCT_DEF_PATH = Path.of(SwProduct.SW_PRODUCT_DEF);

    private SwModuleScanner() {
    }

    /**
     * Scan for module.def files.
     * @param path Path to scan from.
     * @return    private static final Logger LOGGER = LoggerFactory.getLogger(MUnitTestItemProvider.class);

     * @throws IOException
     */
    public static Set<SwModule> scanModules(final Path path) throws IOException {
        final ModuleDefFileVisitor fileVistor = new ModuleDefFileVisitor(path);
        Files.walkFileTree(path, fileVistor);
        return fileVistor.getModules();
    }

    /**
     * Get module from a given path, iterates upwards to find module.def file.
     * @param startPath Path to start at.
     * @return Parsed module definition.
     * @throws IOException -
     */
    @CheckForNull
    public static SwModule moduleAtPath(final Path startPath) throws IOException {
        Path path = startPath;
        while (path != null) {
            final Path moduleDefPath = path.resolve(SW_MODULE_DEF_PATH);
            final File moduleDefFile = moduleDefPath.toFile();
            if (moduleDefFile.exists()) {
                return SwModuleScanner.readModuleDefinition(moduleDefPath);
            }

            path = path.getParent();
        }

        return null;
    }

    /**
     * Read module.def file.
     * @param path Path to {@code module.def} file.
     * @return Parsed module definition.
     * @throws IOException -
     */
    public static SwModule readModuleDefinition(final Path path) throws IOException {
        final SwModuleDefParser parser = new SwModuleDefParser();
        final AstNode node = parser.parse(path);
        final AstNode moduleIdentNode = node.getFirstChild(SwModuleDefGrammar.MODULE_IDENTIFICATION);
        final AstNode identfierNode = moduleIdentNode.getFirstChild(SwModuleDefGrammar.IDENTIFIER);
        final String moduleName = identfierNode.getTokenValue();
        return new SwModule(moduleName, path);
    }

}
