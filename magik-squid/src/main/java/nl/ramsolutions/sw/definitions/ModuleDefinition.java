package nl.ramsolutions.sw.definitions;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinition;

/** Smallworld module. */
public class ModuleDefinition implements IDefinition {

  private final @Nullable Location location;
  private final @Nullable Instant timestamp;
  private final String name;
  private final @Nullable String product;
  private final String baseVersion;
  private final @Nullable String currentVersion;
  private final @Nullable String description;
  private final List<ModuleUsage> usages;

  /**
   * Constructor.
   *
   * @param location {@link Location} of the module definition.
   * @param name Name of module.
   * @param product Name of product.
   * @param baseVersion Base version.
   * @param currentVersion Current version.
   * @param description Description.
   * @param usages List of requireds modules.
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
      final List<ModuleUsage> usages) {
    this.location = location;
    this.timestamp = timestamp;
    this.name = name;
    this.product = product;
    this.baseVersion = baseVersion;
    this.currentVersion = currentVersion;
    this.description = description;
    this.usages = List.copyOf(usages);
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

  public List<ModuleUsage> getUsages() {
    return Collections.unmodifiableList(this.usages);
  }

  @Override
  public String toString() {
    return String.format(
        "%s@%s(%s, %s, %s)",
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
