package nl.ramsolutions.sw.checks;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sonar.check.Rule;

/** {@link Check} holder/factory. */
public class CheckHolder {

  private static final Pattern KEBAB_CASE_RE = Pattern.compile("(?=[A-Z][a-z])");

  /** Parameter/value for check. */
  public static class Parameter {

    private final String name;
    private final String description;
    private final Object value;

    /**
     * Constructor.
     *
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

  private final Class<? extends Check> checkClass;
  private final Set<Parameter> parameters;
  private final boolean enabled;
  private CheckMetadata metadata;

  /**
   * Constructor.
   *
   * @param checkClass Check class to wrap.
   * @param parameters Parameters.
   * @param enabled Check is enabled.
   */
  public CheckHolder(
      final Class<? extends Check> checkClass,
      final Set<Parameter> parameters,
      final boolean enabled) {
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
  public Check createCheck() throws ReflectiveOperationException {
    final Check check = this.checkClass.getDeclaredConstructor().newInstance();
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
   * Get the {@link Check} class.
   *
   * @return {@link Check} class.
   */
  public Class<? extends Check> getCheckClass() {
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
   * Get metadata for the {@link Check}.
   *
   * @return Metadata.
   * @throws IOException -
   */
  public CheckMetadata getMetadata() throws IOException {
    if (this.metadata == null) {
      synchronized (this) {
        // Determine path.
        final String name = this.getCheckName();
        final String profileDir = this.getCheckProfileDir();
        final String filename = String.format("/%s/%s.json", profileDir, name);

        // Parse json.
        final Gson gson = new Gson();
        try (final InputStream inputStream = this.getClass().getResourceAsStream(filename)) {
          if (inputStream == null) {
            return null;
          }
          final InputStreamReader reader = new InputStreamReader(inputStream);
          final JsonReader jsonReader = gson.newJsonReader(reader);
          this.metadata = gson.fromJson(jsonReader, CheckMetadata.class);
        }
      }
    }

    return this.metadata;
  }

  private String getCheckName() {
    // TODO: Can't we do this better? Perhaps use the Rule annotation?
    final String simpleName = this.checkClass.getSimpleName();
    return simpleName.endsWith("TypedCheck")
        ? simpleName.substring(0, simpleName.length() - "TypedCheck".length()) // Strip TypedCheck.
        : simpleName.substring(0, simpleName.length() - "Check".length()); // Strip Check.
  }

  private String getCheckProfileDir() {
    // TODO: Can't we do this better?
    if (this.checkClass.getGenericSuperclass().equals(ProductDefCheck.class)) {
      return ProductDefCheckList.PROFILE_DIR;
    } else if (this.checkClass.getGenericSuperclass().equals(ModuleDefCheck.class)) {
      return ModuleDefCheckList.PROFILE_DIR;
    } else if (this.checkClass.getGenericSuperclass().equals(MagikCheck.class)) {
      return MagikCheckList.PROFILE_DIR;
    } else if (this.checkClass.getGenericSuperclass().equals(MagikTypedCheck.class)) {
      return MagikTypedCheckList.PROFILE_DIR;
    }

    throw new IllegalStateException(
        String.format(
            "Unknown check class: %s", this.checkClass.getGenericSuperclass().getTypeName()));
  }

  /**
   * Get the check key.
   *
   * @return The check key.
   */
  public String getCheckKey() {
    final Rule annotation = this.checkClass.getAnnotation(Rule.class);
    return annotation.key();
  }

  /**
   * Get the check key, kebab-cased.
   *
   * @return The check key, kebab-cased.
   */
  public String getCheckKeyKebabCase() {
    final String checkKey = this.getCheckKey();
    return CheckHolder.toKebabCase(checkKey);
  }

  /**
   * Utility method to convert camel case to kebab case.
   *
   * @param string String in camel case.
   * @return String in kebab case.
   */
  public static String toKebabCase(final String string) {
    final Matcher matcher = CheckHolder.KEBAB_CASE_RE.matcher(string);
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
        this.getClass().getName(), Integer.toHexString(this.hashCode()), this.getCheckKey());
  }
}
