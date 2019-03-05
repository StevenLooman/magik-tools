package org.stevenlooman.sw.magik.lint;

import com.google.common.collect.Lists;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ConfigurationLocator {
  public final static String MAGIK_LINT_RC_FILENAME = "magik-lint.properties";
  public final static String HIDDEN_MAGIK_LINT_RC_FILENAME = ".magik-lint.properties";
  public final static String ENV_VAR_MAGIK_LINT_RC = "MAGIKLINTRC";

  // 1. magik-lint.properties in the current working directory
  // 2. .magik-lint.properties in the current working directory
  // 3. In any upper Smallworld product. This allows working on a product-by-product basis.
  // 4. The file named by environment variable MAGIKLINTRC
  // 5. .magik-lint.properties in your home directory
  // 6. /etc/magik-lint.properties

  public static Path locateConfiguration() {
    // 1. rc file in current dir
    Path path = Paths.get(MAGIK_LINT_RC_FILENAME);
    if (Files.exists(path)) {
      return path;
    }

    // 2. hidden rc file in current dir
    path = Paths.get(HIDDEN_MAGIK_LINT_RC_FILENAME);
    if (Files.exists(path)) {
      return path;
    }

    // 3. In any Smallworld product
    for (Path productPath: locateProductDirs()) {
      path = productPath.resolve(MAGIK_LINT_RC_FILENAME);
      if (Files.exists(path)) {
        return path;
      }
    }

    // 4. In env var MAGIKLINTRC
    String rcEnvVar = System.getenv(ENV_VAR_MAGIK_LINT_RC);
    if (rcEnvVar != null && Files.exists(Paths.get(rcEnvVar))) {
      return Paths.get(rcEnvVar);
    }

    // 5. In your home directory
    String homeEnvVar = System.getenv("HOME");
    if (homeEnvVar != null && Files.exists(Paths.get(homeEnvVar))) {
      path = Paths.get(homeEnvVar).resolve(HIDDEN_MAGIK_LINT_RC_FILENAME);
      if (Files.exists(path)) {
        return path;
      }
    }

    // 6. /etc/magik-lint.properties
    path = Paths.get("/etc/").resolve(MAGIK_LINT_RC_FILENAME);
    if (Files.exists(path)) {
      return path;
    }

    return null;
  }

  private static Collection<Path> locateProductDirs() {
    List<Path> dirs = Lists.newArrayList();
    Path path = Paths.get(".").toAbsolutePath();
    while (path != null && Files.exists(path)) {
      // check if product.def exists
      Path productDefPath = path.resolve("product.def");
      if (Files.exists(productDefPath)) {
        dirs.add(path);
      }

      path = path.getParent();
    }

    return Collections.emptyList();
  }

}
