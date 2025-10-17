package nl.ramsolutions.sw.checks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.MagikToolsProperties;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

/** {@link Check} specific configuration. */
public class ChecksConfiguration {

  private static final String KEY_DISABLED_CHECKS = "disabled";
  private static final String KEY_ENABLED_CHECKS = "enabled";
  private static final String KEY_IGNORED_PATHS = "ignore";

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
