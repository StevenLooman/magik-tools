package nl.ramsolutions.sw.checks;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.OpenedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

/** {@link Check} specific configuration. */
public class ChecksConfiguration {

  private static final String KEY_DISABLED_CHECKS = "disabled";
  private static final String KEY_ENABLED_CHECKS = "enabled";
  private static final String KEY_IGNORED_PATHS = "ignore";

  private static final Logger LOGGER = LoggerFactory.getLogger(ChecksConfiguration.class);

  private final MagikToolsProperties properties;
  private final List<Class<? extends Check>> checkClasses;

  /**
   * Constructor which reads properties from {@code path}.
   *
   * @param checkClasses {@link Class}es of {@link Check}s.
   * @param properties Properties to use.
   */
  public ChecksConfiguration(
      final List<Class<? extends Check>> checkClasses, final MagikToolsProperties properties) {
    this.checkClasses = checkClasses;
    this.properties = properties;
  }

  public List<String> getIgnores() {
    return this.properties.getPropertyList(KEY_IGNORED_PATHS);
  }

  /**
   * Test whether {@code openedFile} is ignored, according to its own properties.
   *
   * @param openedFile {@link OpenedFile} to test.
   * @return True if the file is ignored, false otherwise.
   */
  public static boolean isFileIgnored(final OpenedFile openedFile) {
    final MagikToolsProperties fileProperties = openedFile.getProperties();
    final List<String> ignores = fileProperties.getPropertyList(KEY_IGNORED_PATHS);
    final URI uri = openedFile.getUri();
    final Path path = Path.of(uri);
    final boolean isIgnored =
        ignores.stream()
            .map(ChecksConfiguration::createPathMatcher)
            .filter(Objects::nonNull)
            .anyMatch(matcher -> matcher.matches(path));
    if (isIgnored) {
      LOGGER.trace("Thread: {}, ignoring file: {}", Thread.currentThread().getName(), path);
    }
    return isIgnored;
  }

  /**
   * Build a {@link PathMatcher} for {@code ignore}, or null if {@code ignore} is not a usable
   * pattern.
   *
   * @param ignore Pattern to build a {@link PathMatcher} for.
   * @return {@link PathMatcher}, or null if the pattern is unusable.
   */
  @CheckForNull
  private static PathMatcher createPathMatcher(final String ignore) {
    final FileSystem fs = FileSystems.getDefault();
    try {
      return fs.getPathMatcher(ignore);
    } catch (final IllegalArgumentException | UnsupportedOperationException exception) {
      LOGGER.warn(
          "Ignoring invalid '{}' pattern: '{}'."
              + " Patterns must start with 'glob:' or 'regex:', for example 'glob:{}'.",
          KEY_IGNORED_PATHS,
          ignore,
          ignore);
      return null;
    }
  }

  /**
   * Get {@link Check}s, each contained by a {@link CheckHolder}.
   *
   * @return List of {@link CheckHolder}s, each containing a {@link Check} and its configured
   *     parameters.
   */
  public List<CheckHolder> getAllChecks() {
    final List<CheckHolder> holders = new ArrayList<>();

    final List<String> disableds = this.properties.getPropertyList(KEY_DISABLED_CHECKS);
    final List<String> enableds = this.properties.getPropertyList(KEY_ENABLED_CHECKS);

    for (final Class<?> checkClass : this.checkClasses) {
      final String checkKey = ChecksConfiguration.checkKey(checkClass);
      final boolean checkEnabled;
      if (enableds.contains(checkKey)) {
        checkEnabled = true;
      } else if (disableds.contains(checkKey) || disableds.contains("all")) {
        checkEnabled = false;
      } else {
        // No explicit configuration, use default state
        checkEnabled = checkClass.getAnnotation(DisabledByDefault.class) == null;
      }

      // Gather parameters from Check, value from config.
      final Set<CheckHolder.Parameter> parameters =
          Arrays.stream(checkClass.getFields())
              .map(field -> field.getAnnotation(RuleProperty.class))
              .filter(Objects::nonNull)
              .map(
                  ruleProperty -> {
                    final String propertyKey = ChecksConfiguration.propertyKey(ruleProperty);
                    final String configKey = checkKey + "." + propertyKey;
                    if (!this.properties.hasProperty(configKey)) {
                      return null;
                    }

                    // Store parameter.
                    final String description = ruleProperty.description();
                    final CheckHolder.Parameter parameter;
                    if (ruleProperty.type().equals("INTEGER")) {
                      final Integer configValue = this.properties.getPropertyInteger(configKey);
                      parameter = new CheckHolder.Parameter(configKey, description, configValue);
                    } else if (ruleProperty.type().equals("STRING")) {
                      final String configValue = this.properties.getPropertyString(configKey);
                      parameter = new CheckHolder.Parameter(configKey, description, configValue);
                    } else if (ruleProperty.type().equals("BOOLEAN")) {
                      final Boolean configValue = this.properties.getPropertyBoolean(configKey);
                      parameter = new CheckHolder.Parameter(configKey, description, configValue);
                    } else {
                      throw new IllegalStateException(
                          "Unknown type for property: " + ruleProperty.type());
                    }

                    return parameter;
                  })
              .filter(Objects::nonNull)
              .collect(Collectors.toSet());

      @SuppressWarnings("unchecked")
      final CheckHolder holder =
          new CheckHolder((Class<Check>) checkClass, parameters, checkEnabled);
      holders.add(holder);
    }
    return holders;
  }

  private static String checkKey(final Class<?> checkClass) {
    final Rule annotation = checkClass.getAnnotation(Rule.class);
    final String checkKey = annotation.key();
    return CheckHolder.toKebabCase(checkKey);
  }

  private static String propertyKey(final RuleProperty ruleProperty) {
    return ruleProperty.key().replace(" ", "-");
  }
}
