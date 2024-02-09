package nl.ramsolutions.sw.magik.checks;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.MagikToolsProperties;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

/**
 * Configuration.
 */
public class MagikChecksConfiguration {

    private static final String KEY_DISABLED_CHECKS = "disabled";
    private static final String KEY_ENABLED_CHECKS = "enabled";
    private static final String KEY_IGNORED_PATHS = "ignore";

    private final MagikToolsProperties properties;
    private final List<Class<? extends MagikCheck>> checkClasses;

    /**
     * Constructor.
     * @param checkClasses {@link Class}es of {@link MagikCheck}s.
     * @throws IOException
     */
    public MagikChecksConfiguration(final List<Class<? extends MagikCheck>> checkClasses) throws IOException {
        this.checkClasses = checkClasses;
        this.properties = new MagikToolsProperties();
    }

    /**
     * Constructor which reads properties from {@code path}.
     * @param checkClasses {@link Class}es of {@link MagikCheck}s.
     * @param path {@link Path} to read properties from.
     * @throws IOException
     */
    public MagikChecksConfiguration(
            final List<Class<? extends MagikCheck>> checkClasses,
            final Path path)
            throws IOException {
        this.checkClasses = checkClasses;
        this.properties = new MagikToolsProperties(path);
    }

    public List<String> getIgnores() {
        return this.properties.getPropertyList(KEY_IGNORED_PATHS);
    }

    /**
     * Get {@link MagikCheck}s, each contained by a {@link MagikCheckHolder}.
     * @return
     */
    public List<MagikCheckHolder> getAllChecks() {
        final List<MagikCheckHolder> holders = new ArrayList<>();

        final List<String> disableds = this.properties.getPropertyList(KEY_DISABLED_CHECKS);
        final List<String> enableds = this.properties.getPropertyList(KEY_ENABLED_CHECKS);

        for (final Class<?> checkClass : this.checkClasses) {
            final String checkKey = MagikChecksConfiguration.checkKey(checkClass);
            final boolean checkEnabled =
                enableds.contains(checkKey)
                || !disableds.contains(checkKey) && !disableds.contains("all");

            // Gather parameters from MagikCheck, value from config.
            final Set<MagikCheckHolder.Parameter> parameters = Arrays.stream(checkClass.getFields())
                .map(field -> field.getAnnotation(RuleProperty.class))
                .filter(Objects::nonNull)
                .map(ruleProperty -> {
                    final String propertyKey = MagikChecksConfiguration.propertyKey(ruleProperty);
                    final String configKey = checkKey + "." + propertyKey;
                    if (!this.properties.hasProperty(configKey)) {
                        return null;
                    }

                    // Store parameter.
                    final String description = ruleProperty.description();
                    final MagikCheckHolder.Parameter parameter;
                    if (ruleProperty.type().equals("INTEGER")) {
                        final Integer configValue = this.properties.getPropertyInteger(configKey);
                        parameter = new MagikCheckHolder.Parameter(configKey, description, configValue);
                    } else if (ruleProperty.type().equals("STRING")) {
                        final String configValue = this.properties.getPropertyString(configKey);
                        parameter = new MagikCheckHolder.Parameter(configKey, description, configValue);
                    } else if (ruleProperty.type().equals("BOOLEAN")) {
                        final Boolean configValue = this.properties.getPropertyBoolean(configKey);
                        parameter = new MagikCheckHolder.Parameter(configKey, description, configValue);
                    } else {
                        throw new IllegalStateException("Unknown type for property: " + ruleProperty.type());
                    }

                    return parameter;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

            @SuppressWarnings("unchecked")
            final MagikCheckHolder holder =
                new MagikCheckHolder((Class<MagikCheck>) checkClass, parameters, checkEnabled);
            holders.add(holder);
        }
        return holders;
    }

    private static String checkKey(final Class<?> checkClass) {
        final Rule annotation = checkClass.getAnnotation(Rule.class);
        final String checkKey = annotation.key();
        return MagikCheckHolder.toKebabCase(checkKey);
    }

    private static String propertyKey(final RuleProperty ruleProperty) {
        return ruleProperty.key().replace(" ", "-");
    }

}
