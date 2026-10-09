package nl.ramsolutions.sw.sonar.sensors;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import nl.ramsolutions.sw.ConfigurationLocator;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

/**
 * Tells the user that the rule toggles, rule settings and ignored paths of a {@code
 * magik-lint.properties} file have no effect under SonarQube, where the quality profile decides.
 */
final class RuleTogglesNotice {

  private static final Logger LOGGER = Loggers.get(RuleTogglesNotice.class);
  private static final String KEY_ENABLED = "enabled";
  private static final String KEY_DISABLED = "disabled";
  private static final String KEY_IGNORE = "ignore";
  // Every setting that is not a rule setting lives under this prefix.
  private static final String TOOL_SETTING_PREFIX = "magik.";

  private RuleTogglesNotice() {}

  /**
   * Log one INFO line when a properties file that applies to one of the directories has rule
   * toggles, rule settings or ignored paths.
   *
   * @param directories Directories of the scanned files.
   */
  static void logIfIgnored(final Collection<Path> directories) {
    final List<Path> files = RuleTogglesNotice.findFilesWithToggles(directories);
    if (files.isEmpty()) {
      return;
    }

    final String message = RuleTogglesNotice.describe(files);
    LOGGER.info(message);
  }

  /**
   * Build the notice for the files found.
   *
   * @param files Properties files with toggles, not empty.
   * @return The line to log.
   */
  static String describe(final List<Path> files) {
    final Path first = files.get(0);
    final int others = files.size() - 1;
    final String more = others > 0 ? " and " + others + " more" : "";
    return "The rule toggles (enabled, disabled), rule settings and ignored paths in %s%s"
            .formatted(first, more)
        + " are not used under SonarQube; the quality profile decides which rules run and how they"
        + " are set, and sonar.exclusions which files are skipped";
  }

  /**
   * Find the properties files within the scanned tree that have {@code enabled}, {@code disabled}
   * or {@code ignore} entries, or a rule setting ({@code <check>.<parameter>}). A file found by the
   * environment, home directory or {@code /etc} is not part of the project and is skipped.
   *
   * @param directories Directories of the scanned files.
   * @return Distinct properties files with toggles.
   */
  static List<Path> findFilesWithToggles(final Collection<Path> directories) {
    final Set<Path> located = new LinkedHashSet<>();
    for (final Path directory : directories) {
      final Path file = ConfigurationLocator.locateConfiguration(directory);
      if (file != null && RuleTogglesNotice.isInProject(file, directory)) {
        located.add(file.toAbsolutePath().normalize());
      }
    }

    final List<Path> withToggles = new ArrayList<>();
    for (final Path file : located) {
      if (RuleTogglesNotice.hasToggles(file)) {
        withToggles.add(file);
      }
    }
    return withToggles;
  }

  private static boolean isInProject(final Path file, final Path directory) {
    final Path fileDir = file.toAbsolutePath().normalize().getParent();
    final Path scannedDir = directory.toAbsolutePath().normalize();
    return fileDir != null && scannedDir.startsWith(fileDir);
  }

  private static boolean hasToggles(final Path file) {
    final Properties properties = new Properties();
    try (InputStream stream = Files.newInputStream(file)) {
      properties.load(stream);
    } catch (final IOException exception) {
      LOGGER.debug("Cannot read {}: {}", file, exception.getMessage());
      return false;
    }

    final Set<String> keys = properties.stringPropertyNames();
    return keys.stream().anyMatch(RuleTogglesNotice::isIgnoredUnderSonar);
  }

  private static boolean isIgnoredUnderSonar(final String key) {
    if (key.equals(KEY_ENABLED) || key.equals(KEY_DISABLED) || key.equals(KEY_IGNORE)) {
      return true;
    }

    return key.contains(".") && !key.startsWith(TOOL_SETTING_PREFIX);
  }
}
