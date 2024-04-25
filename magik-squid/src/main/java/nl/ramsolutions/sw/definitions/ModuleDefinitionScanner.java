package nl.ramsolutions.sw.definitions;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.definitions.api.SwModuleDefinitionGrammar;
import nl.ramsolutions.sw.definitions.parser.SwModuleDefParser;
import nl.ramsolutions.sw.magik.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * module.def file scanner.
 *
 * <p>Scans for module.def files. Aborts scanning when a different product is found.
 */
public final class ModuleDefinitionScanner {

  private static class ModuleDefFileVisitor extends SimpleFileVisitor<Path> {

    private Set<ModuleDefinition> modules = new HashSet<>();
    private final Path startPath;

    ModuleDefFileVisitor(final Path startPath) {
      this.startPath = startPath;
    }

    public Set<ModuleDefinition> getModules() {
      return this.modules;
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
        ModuleDefinition swModule = ModuleDefinitionScanner.readModuleDefinition(path);
        this.modules.add(swModule);
      } catch (IOException exception) {
        LOGGER.error(exception.getMessage(), exception);
      }
    }
  }

  /** Module definition filename. */
  public static final String SW_MODULE_DEF = "module.def";

  private static final String UNDEFINED_MODULE_NAME = "_undefined_module";
  private static final String UNDEFINED_MODULE_VERSION = "_undefined_version";

  private static final Map<Path, Path> CACHE = new ConcurrentHashMap<>();
  private static final Path DOES_NOT_EXIST = Path.of("DOES_NOT_EXIST");

  private static final Logger LOGGER = LoggerFactory.getLogger(ModuleDefinitionScanner.class);

  private static final Path SW_MODULE_DEF_PATH = Path.of(ModuleDefinitionScanner.SW_MODULE_DEF);
  private static final Path SW_PRODUCT_DEF_PATH = Path.of(ProductDefinitionScanner.SW_PRODUCT_DEF);

  private ModuleDefinitionScanner() {}

  public static void resetCache() {
    ModuleDefinitionScanner.CACHE.clear();
  }

  /**
   * Scan for `module.def` files.
   *
   * @param path Path to scan from.
   * @return Set of found {@link ModuleDefinition}s.
   * @throws IOException
   */
  public static Set<ModuleDefinition> scanModules(final Path path) throws IOException {
    final ModuleDefFileVisitor fileVistor = new ModuleDefFileVisitor(path);
    Files.walkFileTree(path, fileVistor);
    return fileVistor.getModules();
  }

  @CheckForNull
  private static Path moduleDefAtPath(final Path startPath) {
    final Path cachedPath = ModuleDefinitionScanner.CACHE.get(startPath);
    if (cachedPath != null) {
      if (cachedPath == ModuleDefinitionScanner.DOES_NOT_EXIST) {
        return null;
      }

      return cachedPath;
    }

    Path path = startPath;
    while (path != null) {
      final Path moduleDefPath = path.resolve(SW_MODULE_DEF_PATH);
      if (Files.exists(moduleDefPath)) {
        ModuleDefinitionScanner.CACHE.put(startPath, moduleDefPath);
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
  public static ModuleDefinition swModuleForPath(final Path startPath) throws IOException {
    final Path moduleDefPath = ModuleDefinitionScanner.moduleDefAtPath(startPath);
    if (moduleDefPath == null) {
      return null;
    }

    return ModuleDefinitionScanner.readModuleDefinition(moduleDefPath);
  }

  /**
   * Read module.def file.
   *
   * @param path Path to {@code module.def} file.
   * @return Parsed module definition.
   * @throws IOException -
   */
  public static ModuleDefinition readModuleDefinition(final Path path) throws IOException {
    final SwModuleDefParser parser = new SwModuleDefParser();
    final AstNode node = parser.parse(path);

    final String moduleName;
    final String baseVersion;
    final String currentVersion;
    final AstNode moduleIdentNode =
        node.getFirstChild(SwModuleDefinitionGrammar.MODULE_IDENTIFICATION);
    if (moduleIdentNode != null) {
      final AstNode nameNode = moduleIdentNode.getFirstChild(SwModuleDefinitionGrammar.MODULE_NAME);
      moduleName = nameNode.getTokenValue();

      final List<AstNode> versionNodes =
          moduleIdentNode.getChildren(SwModuleDefinitionGrammar.VERSION);
      final AstNode baseVersionNode = versionNodes.get(0);
      baseVersion = baseVersionNode.getTokenValue();
      final AstNode currentVersionNode = versionNodes.size() > 1 ? versionNodes.get(1) : null;
      currentVersion = currentVersionNode != null ? currentVersionNode.getTokenValue() : null;
    } else {
      moduleName = ModuleDefinitionScanner.UNDEFINED_MODULE_NAME;
      baseVersion = ModuleDefinitionScanner.UNDEFINED_MODULE_VERSION;
      currentVersion = null;
    }
    final AstNode requiresNode = node.getFirstChild(SwModuleDefinitionGrammar.REQUIRES);
    final List<String> requireds =
        requiresNode != null
            ? requiresNode.getDescendants(SwModuleDefinitionGrammar.MODULE_REF).stream()
                .map(AstNode::getTokenValue)
                .toList()
            : Collections.emptyList();

    final AstNode descriptionNode = node.getFirstChild(SwModuleDefinitionGrammar.DESCRIPTION);
    final String description =
        descriptionNode != null
            ? descriptionNode.getChildren(SwModuleDefinitionGrammar.FREE_LINES).stream()
                .map(n -> n.getTokenValue())
                .collect(Collectors.joining("\n"))
            : null;

    final URI uri = path.toUri();
    final Location location = new Location(uri);
    return new ModuleDefinition(
        location, moduleName, baseVersion, currentVersion, description, requireds);
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
    final Path fileName = path.getFileName();
    final Path searchPath =
        fileName.toString().toLowerCase().endsWith(".magik") ? path.getParent() : path;

    final ModuleDefinition swModule;
    try {
      swModule = ModuleDefinitionScanner.swModuleForPath(searchPath);
    } catch (final IOException exception) {
      throw new IllegalStateException(exception);
    }

    if (swModule == null) {
      return null;
    }

    return swModule.getName();
  }
}
