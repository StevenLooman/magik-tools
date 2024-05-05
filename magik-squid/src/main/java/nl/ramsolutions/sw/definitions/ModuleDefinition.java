package nl.ramsolutions.sw.definitions;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinition;

/** Smallworld module. */
public class ModuleDefinition implements IDefinition {

  private final @Nullable Location location;
  private final String name;
  private final @Nullable String product;
  private final String baseVersion;
  private final @Nullable String currentVersion;
  private final @Nullable String description;
  private final List<String> requireds;

  /**
   * Constructor.
   *
   * @param name Name of module.
   * @param location {@link Location} of the module definition.
   */
  public ModuleDefinition(
      final @Nullable Location location,
      final String name,
      final @Nullable String product,
      final String baseVersion,
      final @Nullable String currentVersion,
      final @Nullable String description,
      final List<String> requireds) {
    this.location = location;
    this.name = name;
    this.product = product;
    this.baseVersion = baseVersion;
    this.currentVersion = currentVersion;
    this.description = description;
    this.requireds = List.copyOf(requireds);
  }

  public String getName() {
    return this.name;
  }

  @CheckForNull
  public Location getLocation() {
    return this.location;
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

  public List<String> getRequireds() {
    return Collections.unmodifiableList(this.requireds);
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
