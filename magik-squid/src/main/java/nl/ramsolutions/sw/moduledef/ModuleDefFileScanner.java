package nl.ramsolutions.sw.moduledef;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.IgnoreHandler;

/** Module.def file scanner. */
public final class ModuleDefFileScanner {

  public static final String SW_MODULE_DEF = "module.def";
  public static final String SW_PRODUCT_DEF = "product.def";

  private final IgnoreHandler ignoreHandler;

  public ModuleDefFileScanner(final IgnoreHandler ignoreHandler) {
    this.ignoreHandler = ignoreHandler;
  }

  /**
   * Get the `module.def` files from the given path.
   *
   * @param fromPath Path to walk from, most likely a directory.
   * @return Stream of {@link Path}s to `module.def` files.
   * @throws IOException -
   */
  @CheckForNull
  public Set<Path> getModuleDefFiles(final Path fromPath) throws IOException {
    return Files.walk(fromPath)
        .filter(Files::isRegularFile)
        .filter(this::isModuleDefFile)
        .filter(this::notIgnored)
        .collect(Collectors.toSet());
  }

  /**
   * Get owning `product.def` from a given path.
   *
   * <p>Iterates upwards to find product.def file.
   *
   * @param startPath Path to start at.
   * @return Path to `product.def` file if found, null otherwise.
   */
  @CheckForNull
  public static Path getProductDefFileForPath(final Path startPath) {
    Path path = startPath;
    while (path != null) {
      final Path productDefPath = path.resolve(SW_PRODUCT_DEF);
      final File productDefFile = productDefPath.toFile();
      if (productDefFile.exists()) {
        return productDefPath;
      }

      path = path.getParent();
    }

    return null;
  }

  /**
   * Get owning `module.def` from a given path.
   *
   * <p>Iterates upwards to find product.def file.
   *
   * @param startPath Path to start at.
   * @return Path to `product.def` file if found, null otherwise.
   */
  @CheckForNull
  public static Path getModuleDefFileForPath(final Path startPath) {
    Path path = startPath;
    while (path != null) {
      final Path moduleDefPath = path.resolve(SW_MODULE_DEF);
      final File moduleDefFile = moduleDefPath.toFile();
      if (moduleDefFile.exists()) {
        return moduleDefPath;
      }

      path = path.getParent();
    }

    return null;
  }

  private boolean notIgnored(final Path path) {
    return !this.ignoreHandler.isIgnored(path);
  }

  private boolean isModuleDefFile(final Path path) {
    return path.getFileName().toString().equalsIgnoreCase(ModuleDefFileScanner.SW_MODULE_DEF);
  }
}
