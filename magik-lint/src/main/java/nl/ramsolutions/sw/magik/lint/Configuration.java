package nl.ramsolutions.sw.magik.lint;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import nl.ramsolutions.sw.magik.checks.CheckList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.check.Rule;

/**
 * Configuration.
 */
public class Configuration {

    private static final Logger LOGGER = LoggerFactory.getLogger(Configuration.class);

    private final Properties properties = new Properties();

    /**
     * Constructor.
     */
    public Configuration() {
        this.setDisabledChecks();
    }

    /**
     * Constructor which reads properties from {{path}}.
     * @param path {{Path}} to read properties from.
     */
    public Configuration(final Path path) {
        LOGGER.debug("Reading configuration from: {}", path.toAbsolutePath());
        this.setDisabledChecks();
        this.readFileProperties(path);
    }

    private void setDisabledChecks() {
        final String disabled = this.getTemplatedCheckNames() + "," + this.getDisabledCheckNames();
        this.properties.put("disabled", disabled);
    }

    private String getTemplatedCheckNames() {
        return CheckList.getTemplatedChecks().stream()
            .map(checkClass -> checkClass.getAnnotation(org.sonar.check.Rule.class))
            .filter(Objects::nonNull)
            .map(Rule::key)
            .map(Configuration::toKebabCase)
            .collect(Collectors.joining(","));
    }

    private String getDisabledCheckNames() {
        return CheckList.getDisabledByDefaultChecks().stream()
            .map(checkClass -> checkClass.getAnnotation(org.sonar.check.Rule.class))
            .filter(Objects::nonNull)
            .map(Rule::key)
            .map(Configuration::toKebabCase)
            .collect(Collectors.joining(","));
    }

    private void readFileProperties(final Path path) {
        final File file = path.toFile();
        try (InputStream inputStream = new FileInputStream(file)) {
            this.properties.load(inputStream);
        } catch (FileNotFoundException exception) {
            // pass.
        } catch (IOException exception) {
            LOGGER.error(exception.getMessage(), exception);
        }
    }

    /**
     * Get a property.
     * @param key Key of property.
     * @return Values of property.
     */
    @CheckForNull
    public String getPropertyString(final String key) {
        return this.properties.getProperty(key);
    }

    /**
     * Get property as integer.
     *
     * @param key Key of property.
     * @return Value of property, as integer.
     */
    @CheckForNull
    public Integer getPropertyInt(final String key) {
        final String value = this.getPropertyString(key);
        if (value == null) {
            return null;
        }

        return Integer.valueOf(value);
    }

    /**
     * Get property as boolean.
     *
     * @param key Key of property.
     * @return Value of property, as boolean.
     */
    @CheckForNull
    public Boolean getPropertyBoolean(final String key) {
        final String value = this.getPropertyString(key);
        if (value == null) {
            return null;
        }

        return Boolean.valueOf(value);
    }

    /**
     * Test if property with key exists.
     *
     * @param key Key of property.
     * @return True if property exists, false if not.
     */
    public boolean hasProperty(final String key) {
        return this.properties.getProperty(key) != null;
    }

    /**
     * Set a property.
     * @param key Key of property.
     * @param value Value of property.
     */
    public void setProperty(final String key, final String value) {
        this.properties.setProperty(key, value);
    }

    /**
     * Log the configuration.
     */
    public void logProperties() {
        LOGGER.debug("Configuration:");
        final List<?> propertyNames = Collections.list(this.properties.propertyNames());
        for (final Object propertyName : propertyNames) {
            final String name = (String) propertyName;
            final Object propertyValue = this.properties.get(name);
            LOGGER.debug(" {}: {}", name, propertyValue);
        }
    }

    /**
     * Utility method to convert camel case to kebab case.
     * @param string String in camel case.
     * @return String in kebab case.
     */
    public static String toKebabCase(final String string) {
        final Pattern pattern = Pattern.compile("(?=[A-Z][a-z])");
        final Matcher matcher = pattern.matcher(string);
        final String stringKebab = matcher.replaceAll("-").toLowerCase();
        if (stringKebab.startsWith("-")) {
            return stringKebab.substring(1);
        }
        return stringKebab;
    }

}
