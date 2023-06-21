package nl.ramsolutions.sw.magik.lint;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.CheckForNull;
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
     * 1. magik-lint.properties in the current directory
     * 2. magik-lint.properties in any upper Smallworld product or module.
     * 3. The file named by environment variable MAGIKLINTRC
     * 4. .magik-lint.properties in your home directory
     * 5. .magik-lint.properties in the System property `user.home`
     * 6. /etc/magik-lint.properties
     * @param searchPath Path to start looking from, a directory.
     * @return Return the path to the configuration to use.
     */
    public static Path locateConfiguration(final Path searchPath) {
        LOGGER.trace("Search path: {}", searchPath.toAbsolutePath());

        // 1. rc file in current dir.
        final Path currentDirPath = ConfigurationLocator.inCurrentDir(searchPath);
        if (currentDirPath != null) {
            return currentDirPath;
        }

        // 3. In any upper Smallworld product.
        final Path productDirPath = ConfigurationLocator.inProductDir(searchPath);
        if (productDirPath != null) {
            return productDirPath;
        }

        // 4. In env var MAGIKLINTRC.
        final Path rcEnvVarPath = ConfigurationLocator.rcEnvVar();
        if (rcEnvVarPath != null) {
            return rcEnvVarPath;
        }

        // 5. In home directory.
        final Path homeDirPath = ConfigurationLocator.inHomeDir();
        if (homeDirPath != null) {
            return homeDirPath;
        }

        // 6. In your home directory, Java style.
        final Path userHomePath = ConfigurationLocator.inUserHomeDir();
        if (userHomePath != null) {
            return userHomePath;
        }

        // 7. /etc/magik-lint.properties.
        final Path etcPath = ConfigurationLocator.inEtcDir();
        if (etcPath != null) {
            return etcPath;
        }

        LOGGER.trace("No configuration found");
        return null;
    }

    private static Collection<Path> locateProductOrModuleDirs(final Path searchPath) {
        final List<Path> dirs = new ArrayList<>();

        Path path = searchPath.toAbsolutePath();
        LOGGER.trace("Locating product.def/module.def in path (3): {}", path);

        while (path != null && Files.exists(path)) {
            // Test if product.def exists.
            final Path productDefPath = path.resolve("product.def");
            LOGGER.trace("Product.def path (3): {}", productDefPath);
            if (Files.exists(productDefPath)) {
                LOGGER.trace("Found product.def, adding path: {}", path);
                dirs.add(path);
            }

            // Test if module.def exists.
            final Path moduleDefPath = path.resolve("module.def");
            LOGGER.trace("Module.def path (3): {}", moduleDefPath);
            if (Files.exists(moduleDefPath)) {
                LOGGER.trace("Found module.def, adding path: {}", path);
                dirs.add(path);
            }

            path = path.getParent();
        }

        return dirs;
    }

    @CheckForNull
    private static Path inCurrentDir(final Path searchPath) {
        final Path magikLintRcPath = searchPath.resolve(MAGIK_LINT_RC_FILENAME);
        LOGGER.trace("Trying to get config at (1): {}", magikLintRcPath.toAbsolutePath());
        if (Files.exists(magikLintRcPath)) {
            return magikLintRcPath;
        }

        return null;
    }

    @CheckForNull
    private static Path inProductDir(final Path searchPath) {
        for (final Path defPath : ConfigurationLocator.locateProductOrModuleDirs(searchPath)) {
            final Path path = defPath.resolve(MAGIK_LINT_RC_FILENAME);
            LOGGER.trace("Trying to get config at (3): {}", path.toAbsolutePath());
            if (Files.exists(path)) {
                return path;
            }
        }

        return null;
    }

    @CheckForNull
    private static Path rcEnvVar() {
        final String rcEnvVar = System.getenv(ENV_VAR_MAGIK_LINT_RC);
        if (rcEnvVar != null) {
            final Path path = Path.of(rcEnvVar);
            LOGGER.trace("Trying to get config at (4): {}", path.toAbsolutePath());
            if (Files.exists(path)) {
                return path;
            }
        }

        return null;
    }

    @CheckForNull
    private static Path inHomeDir() {
        final String homeEnvVar = System.getenv("HOME");
        if (homeEnvVar != null) {
            final Path path = Path.of(homeEnvVar).resolve(HIDDEN_MAGIK_LINT_RC_FILENAME);
            LOGGER.trace("Trying to get config at (5): {}", path.toAbsolutePath());
            if (Files.exists(path)) {
                return path;
            }
        }

        return null;
    }

    @CheckForNull
    private static Path inUserHomeDir() {
        final String homeSystemProperty = System.getProperty("user.home");
        if (homeSystemProperty != null) {
            final Path path = Path.of(homeSystemProperty).resolve(HIDDEN_MAGIK_LINT_RC_FILENAME);
            LOGGER.trace("Trying to get config at (6): {}", path.toAbsolutePath());
            if (Files.exists(path)) {
                return path;
            }
        }

        return null;
    }

    @CheckForNull
    private static Path inEtcDir() {
        final Path etcPath = Path.of("/etc/").resolve(MAGIK_LINT_RC_FILENAME);
        LOGGER.trace("Trying to get config at (7): {}", etcPath.toAbsolutePath());
        if (Files.exists(etcPath)) {
            return etcPath;
        }

        return null;
    }

}
