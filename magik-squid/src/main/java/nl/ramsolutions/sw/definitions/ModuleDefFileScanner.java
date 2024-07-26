package nl.ramsolutions.sw.definitions;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import nl.ramsolutions.sw.magik.ModuleDefFile;
import nl.ramsolutions.sw.magik.ProductDefFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * module.def file scanner.
 *
 * <p>Scans for module.def files. Aborts scanning when a different product is found.
 */
public final class ModuleDefFileScanner {

  private static class ModuleDefFileVisitor extends SimpleFileVisitor<Path> {

    private Set<ModuleDefFile> moduleDefFiles = new HashSet<>();
    private final Path startPath;

    ModuleDefFileVisitor(final Path startPath) {
      this.startPath = startPath;
    }

    public Set<ModuleDefFile> getModuleDefFiles() {
      return this.moduleDefFiles;
    }

    @Override
    public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs)
        throws IOException {
      final Path productDefPath = dir.resolve(SW_PRODUCT_DEF_PATH);
      if (!dir.equals(this.startPath) && Files.exists(productDefPath)) {
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
        final ModuleDefFile moduleDefFile = new ModuleDefFile(path, null, null);
        this.moduleDefFiles.add(moduleDefFile);
      } catch (final IOException exception) {
        LOGGER.error(exception.getMessage(), exception);
        throw new IllegalStateException();
      }
    }
  }

  /** Module definition filename. */
  public static final String SW_MODULE_DEF = "module.def";

  private static final Map<Path, Path> CACHE = new ConcurrentHashMap<>();
  private static final Path DOES_NOT_EXIST = Path.of("DOES_NOT_EXIST");

  private static final Logger LOGGER = LoggerFactory.getLogger(ModuleDefFileScanner.class);

  private static final Path SW_MODULE_DEF_PATH = Path.of(ModuleDefFileScanner.SW_MODULE_DEF);
  private static final Path SW_PRODUCT_DEF_PATH = Path.of(ProductDefFileScanner.SW_PRODUCT_DEF);

  private ModuleDefFileScanner() {}

  public static void resetCache() {
    ModuleDefFileScanner.CACHE.clear();
  }

  /**
   * Scan for `module.def` files.
   *
   * @param path Path to scan from.
   * @return Set of found {@link ModuleDefinition}s.
   * @throws IOException
   */
  public static Set<ModuleDefFile> scanModuleFiles(final Path path) throws IOException {
    final ModuleDefFileVisitor fileVistor = new ModuleDefFileVisitor(path);
    Files.walkFileTree(path, fileVistor);
    return fileVistor.getModuleDefFiles();
  }

  @CheckForNull
  private static Path moduleDefPathForPath(final Path startPath) {
    final Path cachedPath = ModuleDefFileScanner.CACHE.get(startPath);
    if (cachedPath != null) {
      if (cachedPath == ModuleDefFileScanner.DOES_NOT_EXIST) {
        return null;
      }

      return cachedPath;
    }

    Path path = startPath;
    while (path != null) {
      final Path moduleDefPath = path.resolve(SW_MODULE_DEF_PATH);
      if (Files.exists(moduleDefPath)) {
        ModuleDefFileScanner.CACHE.put(startPath, moduleDefPath);
        return moduleDefPath;
      }

      path = path.getParent();
    }

    return null;
  }

  /**
   * Get module from a given path, iterates upwards to find module.def file.
   *
   * @param startPath Path to start at.
   * @return Parsed module definition.
   * @throws IOException -
   */
  @CheckForNull
  public static ModuleDefFile getModuleDefFileForPath(final Path startPath) throws IOException {
    final Path moduleDefPath = ModuleDefFileScanner.moduleDefPathForPath(startPath);
    if (moduleDefPath == null) {
      return null;
    }

    return new ModuleDefFile(moduleDefPath, null, null);
  }

  /**
   * Read module.def file.
   *
   * @param path Path to {@code module.def} file.
   * @return Parsed module definition.
   * @throws IOException -
   */
  public static ModuleDefinition readModuleDefinition(final Path path) throws IOException {
    final ModuleDefinitionParser parser = new ModuleDefinitionParser();
    final ModuleDefFile moduleDefFile = new ModuleDefFile(path, null, null);
    final ProductDefFile productDefFile = ProductDefFileScanner.getProductDefFileForPath(path);
    final ProductDefinition productDefinition =
        productDefFile != null ? productDefFile.getProductDefinition() : null;
    return parser.parseDefinition(moduleDefFile, productDefinition);
  }

  /**
   * Get the module name from a given uri.
   *
   * @param uri {@link URI} to start searching from.
   * @return SwModule name, or null if no module was found.
   */
  @CheckForNull
  public static String getModuleName(final URI uri) {
    if (!uri.getScheme().equals("file")) {
      // For unit tests.
      return null;
    }

    final Path path = Path.of(uri);
    final ModuleDefFile moduleDefFile;
    try {
      moduleDefFile = ModuleDefFileScanner.getModuleDefFileForPath(path);
    } catch (final IOException exception) {
      throw new IllegalStateException(exception);
    }

    if (moduleDefFile == null) {
      return null;
    }

    return moduleDefFile.getModuleDefinition().getName();
  }
}
