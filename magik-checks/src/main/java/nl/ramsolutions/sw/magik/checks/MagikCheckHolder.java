package nl.ramsolutions.sw.magik.checks;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.check.Rule;

/**
 * MagicCheck holder/factory.
 */
public class MagikCheckHolder {

    /**
     * Parameter to check.
     */
    public static class Parameter {

        private final String name;
        private final String description;
        private final Object value;

        /**
         * Constructor.
         * @param name Name of parameter.
         * @param description Description of parameter.
         * @param value Value of parameter.
         */
        public Parameter(final String name, final String description, final @Nullable Object value) {
            this.name = name;
            this.description = description;
            this.value = value;
        }

        public String getName() {
            return this.name;
        }

        public String getDescription() {
            return this.description;
        }

        @CheckForNull
        public Object getValue() {
            return this.value;
        }
    }

    private final Class<? extends MagikCheck> checkClass;
    private final Set<Parameter> parameters;
    private final boolean enabled;
    private MagikCheckMetadata metadata;

    /**
     * Constructor.
     *
     * @param checkClass Check class to wrap.
     * @param parameters Parameters.
     * @param enabled Check is enabled.
     */
    public MagikCheckHolder(
            final Class<? extends MagikCheck> checkClass, final Set<Parameter> parameters, final boolean enabled) {
        this.checkClass = checkClass;
        this.parameters = parameters;
        this.enabled = enabled;
        this.metadata = null;
    }

    /**
     * Get the wrapped check.
     *
     * @return Check
     * @throws ReflectiveOperationException -
     */
    public MagikCheck createCheck() throws ReflectiveOperationException {
        final MagikCheck check = this.checkClass.getDeclaredConstructor().newInstance();
        check.setHolder(this);

        for (final Parameter parameter : this.parameters) {
            final String name = parameter.getName();
            final Object value = parameter.getValue();
            check.setParameter(name, value);
        }

        return check;
    }

    /**
     * Test if check is enabled.
     *
     * @return True if enabled, false if not.
     */
    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * Get the {@link MagikCheck} class.
     * @return {@link MagikCheck} class.
     */
    public Class<? extends MagikCheck> getCheckClass() {
        return this.checkClass;
    }

    /**
     * Get all parameters.
     *
     * @return List of ParameterInfo
     * @throws IllegalAccessException -
     */
    public Iterable<Parameter> getParameters() throws IllegalAccessException {
        return this.parameters;
    }

    /**
     * Get metadata for the {@link MagikCheck}.
     * @return Metadata.
     * @throws IOException -
     */
    public MagikCheckMetadata getMetadata() throws IOException {
        if (this.metadata == null) {
            synchronized (this) {
                // determine path
                final String simpleName = this.checkClass.getSimpleName();
                final String name = simpleName.endsWith("TypedCheck")
                    ? simpleName.substring(0, simpleName.length() - "TypedCheck".length()) // strip TypedCheck
                    : simpleName.substring(0, simpleName.length() - "Check".length()); // strip Check
                final String filename = String.format(
                        "/%s/%s.json",
                        CheckList.PROFILE_DIR, name);

                // parse json
                final Gson gson = new Gson();
                try (InputStream inputStream = this.getClass().getResourceAsStream(filename)) {
                    final InputStreamReader reader = new InputStreamReader(inputStream);
                    final JsonReader jsonReader = gson.newJsonReader(reader);
                    this.metadata = gson.fromJson(jsonReader, MagikCheckMetadata.class);
                }
            }
        }

        return this.metadata;
    }

    /**
     * Get the check key.
     * @return The check key.
     */
    public String getCheckKey() {
        final Rule annotation = this.checkClass.getAnnotation(Rule.class);
        return annotation.key();
    }

    /**
     * Get the check key, kebab-cased.
     * @return The check key, kebab-cased.
     */
    public String getCheckKeyKebabCase() {
        final String checkKey = this.getCheckKey();
        return MagikCheckHolder.toKebabCase(checkKey);
    }

    /**
     * Utility method to convert camel case    to kebab case.
     * @param string String in camel case.
     * @return String in kebab case.
     */
    public static String toKebabCase(String string) {
        final Pattern pattern = Pattern.compile("(?=[A-Z][a-z])");
        final Matcher matcher = pattern.matcher(string);
        final String stringKebab = matcher.replaceAll("-").toLowerCase();
        if (stringKebab.startsWith("-")) {
            return stringKebab.substring(1);
        }
        return stringKebab;
    }

    @Override
    public String toString() {
        return String.format(
            "%s@%s(%s)",
            this.getClass().getName(), Integer.toHexString(this.hashCode()),
            this.getCheckKey());
    }

}
