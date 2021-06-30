package org.stevenlooman.sw.magik.lint;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

public class ConfigurationLocator {

  static Logger logger = Logger.getLogger(ConfigurationLocator.class.getName());

  public static final String MAGIK_LINT_RC_FILENAME = "magik-lint.properties";
  public static final String HIDDEN_MAGIK_LINT_RC_FILENAME = ".magik-lint.properties";
  public static final String ENV_VAR_MAGIK_LINT_RC = "MAGIKLINTRC";

  /**
   * Locate the configuration.
   * Steps, in order:
   * 1. magik-lint.properties in the current working directory
   * 2. .magik-lint.properties in the current working directory
   * 3. magik-lint.properties in any upper Smallworld product.
   *    This allows working on a product-by-product basis.
   * 4. The file named by environment variable MAGIKLINTRC
   * 5. .magik-lint.properties in your home directory
   * 6. /etc/magik-lint.properties
   * @return Return the path to the configuration to use.
   */
  public static Path locateConfiguration() {
    logger.finest("Current working directory: " + Paths.get("").toAbsolutePath());

    // 1. rc file in current dir
    Path path = Paths.get(MAGIK_LINT_RC_FILENAME);
    logger.finest("Trying to get config at (1): " + path.toAbsolutePath());
    if (Files.exists(path)) {
      return path;
    }

    // 2. hidden rc file in current dir
    path = Paths.get(HIDDEN_MAGIK_LINT_RC_FILENAME);
    logger.finest("Trying to get config at (2): " + path.toAbsolutePath());
    if (Files.exists(path)) {
      return path;
    }

    // 3. In any Smallworld product
    for (Path productPath: locateProductDirs()) {
      path = productPath.resolve(MAGIK_LINT_RC_FILENAME);
      logger.finest("Trying to get config at (3): " + path.toAbsolutePath());
      if (Files.exists(path)) {
        return path;
      }
    }

    // 4. In env var MAGIKLINTRC
    String rcEnvVar = System.getenv(ENV_VAR_MAGIK_LINT_RC);
    if (rcEnvVar != null) {
      path = Paths.get(rcEnvVar);
      logger.finest("Trying to get config at (4): " + path.toAbsolutePath());
      if (Files.exists(path)) {
        return path;
      }
    }

    // 5. In your home directory
    String homeEnvVar = System.getProperty("user.home");
    if (homeEnvVar != null) {
      path = Paths.get(homeEnvVar).resolve(HIDDEN_MAGIK_LINT_RC_FILENAME);
      logger.finest("Trying to get config at (5): " + path.toAbsolutePath());
      if (Files.exists(path)) {
        return path;
      }
    }

    // 6. /etc/magik-lint.properties
    path = Paths.get("/etc/").resolve(MAGIK_LINT_RC_FILENAME);
    logger.finest("Trying to get config at (6): " + path.toAbsolutePath());
    if (Files.exists(path)) {
      return path;
    }

    return null;
  }

  private static Collection<Path> locateProductDirs() {
    List<Path> dirs = new ArrayList<>();

    Path path = Paths.get("").toAbsolutePath();
    logger.finest("Locating product.def in path (3): " + path);

    while (path != null && Files.exists(path)) {
      // check if product.def exists
      Path productDefPath = path.resolve("product.def");
      logger.finest("Product.def path (3): " + productDefPath);
      if (Files.exists(productDefPath)) {
        logger.finest("Found product.def, adding path: " + path);
        dirs.add(path);
      }

      path = path.getParent();
    }

    return dirs;
  }

}
