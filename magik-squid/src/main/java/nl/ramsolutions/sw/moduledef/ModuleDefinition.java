package nl.ramsolutions.sw.moduledef;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import nl.ramsolutions.sw.IDefinition;
import nl.ramsolutions.sw.magik.Location;

/** Smallworld module. */
public class ModuleDefinition implements IDefinition {

  private final @Nullable Location location;
  private final @Nullable Instant timestamp;
  private final String name;
  private final @Nullable String product;
  private final String baseVersion;
  private final @Nullable String currentVersion;
  private final @Nullable String description;
  private final List<ModuleUsage> requiredModules;
  private final List<ModuleUsage> testModules;
  private final @Nullable String language;
  private final @Nullable String conditionMessageAccessor;

  /**
   * Constructor.
   *
   * @param location {@link Location} of the module definition.
   * @param name Name of module.
   * @param product Name of product.
   * @param baseVersion Base version.
   * @param currentVersion Current version.
   * @param description Description.
   * @param requiredModules List of required modules.
   * @param testsModules List of test modules.
   */
  @SuppressWarnings({"checkstyle:ParameterNumber", "java:S107"})
  public ModuleDefinition(
      final @Nullable Location location,
      final @Nullable Instant timestamp,
      final String name,
      final @Nullable String product,
      final String baseVersion,
      final @Nullable String currentVersion,
      final @Nullable String description,
      final List<ModuleUsage> requiredModules,
      final List<ModuleUsage> testModules) {
    this(
        location,
        timestamp,
        name,
        product,
        baseVersion,
        currentVersion,
        description,
        requiredModules,
        testModules,
        null,
        null);
  }

  /**
   * Constructor.
   *
   * @param location {@link Location} of the module definition.
   * @param name Name of module.
   * @param product Name of product.
   * @param baseVersion Base version.
   * @param currentVersion Current version.
   * @param description Description.
   * @param requiredModules List of required modules.
   * @param testsModules List of test modules.
   * @param language Language of the module, e.g. {@code en_gb}.
   * @param conditionMessageAccessor Name of the message handler for the messages of the conditions
   *     which are defined by the module.
   */
  @SuppressWarnings({"checkstyle:ParameterNumber", "java:S107"})
  public ModuleDefinition(
      final @Nullable Location location,
      final @Nullable Instant timestamp,
      final String name,
      final @Nullable String product,
      final String baseVersion,
      final @Nullable String currentVersion,
      final @Nullable String description,
      final List<ModuleUsage> requiredModules,
      final List<ModuleUsage> testModules,
      final @Nullable String language,
      final @Nullable String conditionMessageAccessor) {
    this.location = location;
    this.timestamp = timestamp;
    this.name = name;
    this.product = product;
    this.baseVersion = baseVersion;
    this.currentVersion = currentVersion;
    this.description = description;
    this.requiredModules = List.copyOf(requiredModules);
    this.testModules = List.copyOf(testModules);
    this.language = language;
    this.conditionMessageAccessor = conditionMessageAccessor;
  }

  public String getName() {
    return this.name;
  }

  @CheckForNull
  public Location getLocation() {
    return this.location;
  }

  public Instant getTimestamp() {
    return this.timestamp;
  }

  @Override
  public IDefinition getBareDefinition() {
    return this;
  }

  @CheckForNull
  public String getProduct() {
    return this.product;
  }

  public String getBaseVersion() {
    return this.baseVersion;
  }

  @CheckForNull
  public String getCurrentVersion() {
    return this.currentVersion;
  }

  @CheckForNull
  public String getDescription() {
    return this.description;
  }

  public List<ModuleUsage> getRequiredModules() {
    return Collections.unmodifiableList(this.requiredModules);
  }

  public List<ModuleUsage> getTestModules() {
    return Collections.unmodifiableList(this.testModules);
  }

  /**
   * Get the language of the module, e.g. {@code en_gb}.
   *
   * @return Language, if any.
   */
  @CheckForNull
  public String getLanguage() {
    return this.language;
  }

  /**
   * Get the name of the message handler for the messages of the conditions which are defined by the
   * module.
   *
   * @return Name of the message handler, if any.
   */
  @CheckForNull
  public String getConditionMessageAccessor() {
    return this.conditionMessageAccessor;
  }

  @Override
  public String toString() {
    return "%s@%s(%s, %s, %s)"
        .formatted(
            this.getClass().getName(),
            Integer.toHexString(this.hashCode()),
            this.getName(),
            this.getBaseVersion(),
            this.getCurrentVersion());
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null) {
      return false;
    }

    if (this.getClass() != obj.getClass()) {
      return false;
    }

    final ModuleDefinition otherSwModule = (ModuleDefinition) obj;
    return Objects.equals(otherSwModule.getLocation(), this.getLocation())
        && Objects.equals(otherSwModule.getName(), this.getName())
        && Objects.equals(otherSwModule.getBaseVersion(), this.getBaseVersion())
        && Objects.equals(otherSwModule.getCurrentVersion(), this.getCurrentVersion());
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.location, this.name, this.baseVersion, this.currentVersion);
  }
}
