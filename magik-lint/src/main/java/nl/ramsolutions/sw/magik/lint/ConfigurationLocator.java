package nl.ramsolutions.sw.magik.lint;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Locator for configuration file (magik-lint.properties).
 */
public final class ConfigurationLocator {

    /**
     * MagikLint RC filename.
     */
    public static final String MAGIK_LINT_RC_FILENAME = "magik-lint.properties";

    /**
     * Hidden MagikLint RC Filename.
     */
    public static final String HIDDEN_MAGIK_LINT_RC_FILENAME = ".magik-lint.properties";

    /**
     * Environment variable for MagikLint RC Filename.
     */
    public static final String ENV_VAR_MAGIK_LINT_RC = "MAGIKLINTRC";

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationLocator.class);

    private ConfigurationLocator() {
    }

    /**
     * Locate the configuration.
     * Steps, in order:
     * 1. magik-lint.properties in the current working directory
     * 2. .magik-lint.properties in the current working directory
     * 3. magik-lint.properties in any upper Smallworld product.
     *        This allows working on a product-by-product basis.
     * 4. The file named by environment variable MAGIKLINTRC
     * 5. .magik-lint.properties in your home directory
     * 6. .magik-lint.properties in the System property `user.home`
     * 7. /etc/magik-lint.properties
     * @return Return the path to the configuration to use.
     */
    public static Path locateConfiguration() {
        LOGGER.trace("Current working directory: {}", Path.of("").toAbsolutePath());

        // 1. rc file in current dir
        final Path magikLintRcPath = Path.of(MAGIK_LINT_RC_FILENAME);
        LOGGER.trace("Trying to get config at (1): {}", magikLintRcPath.toAbsolutePath());
        if (Files.exists(magikLintRcPath)) {
            return magikLintRcPath;
        }

        // 2. hidden rc file in current dir
        final Path hiddenMagikLintRcPath = Path.of(HIDDEN_MAGIK_LINT_RC_FILENAME);
        LOGGER.trace("Trying to get config at (2): {}", hiddenMagikLintRcPath.toAbsolutePath());
        if (Files.exists(hiddenMagikLintRcPath)) {
            return hiddenMagikLintRcPath;
        }

        // 3. In any Smallworld product
        for (final Path productPath : ConfigurationLocator.locateProductDirs()) {
            final Path path = productPath.resolve(MAGIK_LINT_RC_FILENAME);
            LOGGER.trace("Trying to get config at (3): {}", path.toAbsolutePath());
            if (Files.exists(path)) {
                return path;
            }
        }

        // 4. In env var MAGIKLINTRC
        final String rcEnvVar = System.getenv(ENV_VAR_MAGIK_LINT_RC);
        if (rcEnvVar != null) {
            final Path path = Path.of(rcEnvVar);
            LOGGER.trace("Trying to get config at (4): {}", path.toAbsolutePath());
            if (Files.exists(path)) {
                return path;
            }
        }

        // 5. In your home directory
        final String homeEnvVar = System.getenv("HOME");
        if (homeEnvVar != null) {
            final Path path = Path.of(homeEnvVar).resolve(HIDDEN_MAGIK_LINT_RC_FILENAME);
            LOGGER.trace("Trying to get config at (5): {}", path.toAbsolutePath());
            if (Files.exists(path)) {
                return path;
            }
        }

        // 6. In your home directory
        final String homeSystemProperty = System.getProperty("user.home");
        if (homeSystemProperty != null) {
            final Path homePropPath = Path.of(homeSystemProperty).resolve(HIDDEN_MAGIK_LINT_RC_FILENAME);
            LOGGER.trace("Trying to get config at (6): {}", homePropPath.toAbsolutePath());
            if (Files.exists(homePropPath)) {
                return homePropPath;
            }
        }

        // 7. /etc/magik-lint.properties
        final Path etcPath = Path.of("/etc/").resolve(MAGIK_LINT_RC_FILENAME);
        LOGGER.trace("Trying to get config at (7): {}", etcPath.toAbsolutePath());
        if (Files.exists(etcPath)) {
            return etcPath;
        }

        return null;
    }

    private static Collection<Path> locateProductDirs() {
        final List<Path> dirs = new ArrayList<>();

        Path path = Path.of("").toAbsolutePath();
        LOGGER.trace("Locating product.def in path (3): {}", path);

        while (path != null && Files.exists(path)) {
            // check if product.def exists
            Path productDefPath = path.resolve("product.def");
            LOGGER.trace("Product.def path (3): {}", productDefPath);
            if (Files.exists(productDefPath)) {
                LOGGER.trace("Found product.def, adding path: {}", path);
                dirs.add(path);
            }

            path = path.getParent();
        }

        return dirs;
    }

}
