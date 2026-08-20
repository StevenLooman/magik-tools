package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import nl.ramsolutions.sw.magik.Location;

/** Package definition. */
public class PackageDefinition extends MagikDefinition {

  private final String name;
  private final List<String> uses;

  /**
   * Constructor.
   *
   * @param moduleName Module where this package is defined.
   * @param node Node of package definition.
   * @param name Name of package.
   * @param uses Uses by package.
   */
  public PackageDefinition(
      final @Nullable Location location,
      final @Nullable Instant timestamp,
      final @Nullable String moduleName,
      final @Nullable String doc,
      final @Nullable AstNode node,
      final String name,
      final List<String> uses) {
    this(location, timestamp, moduleName, doc, node, name, uses, Provenance.UNKNOWN);
  }

  /**
   * Constructor.
   *
   * @param moduleName Module where this package is defined.
   * @param node Node of package definition.
   * @param name Name of package.
   * @param uses Uses by package.
   * @param provenance Provenance.
   */
  @SuppressWarnings({"checkstyle:ParameterNumber", "java:S107"})
  public PackageDefinition(
      final @Nullable Location location,
      final @Nullable Instant timestamp,
      final @Nullable String moduleName,
      final @Nullable String doc,
      final @Nullable AstNode node,
      final String name,
      final List<String> uses,
      final Provenance provenance) {
    super(location, timestamp, moduleName, doc, node, provenance);
    this.name = name;
    this.uses = List.copyOf(uses);
  }

  @Override
  public String getName() {
    return this.name;
  }

  public List<String> getUses() {
    return Collections.unmodifiableList(this.uses);
  }

  @Override
  public PackageDefinition getBareDefinition() {
    return new PackageDefinition(
        this.getLocation(),
        this.getTimestamp(),
        this.getModuleName(),
        this.getDoc(),
        null,
        this.name,
        this.uses,
        this.getProvenance());
  }

  @Override
  public PackageDefinition withProvenance(final Provenance provenance) {
    return new PackageDefinition(
        this.getLocation(),
        this.getTimestamp(),
        this.getModuleName(),
        this.getDoc(),
        this.getNode(),
        this.name,
        this.uses,
        provenance);
  }

  @Override
  public String toString() {
    return "%s@%s(%s)"
        .formatted(this.getClass().getName(), Integer.toHexString(this.hashCode()), this.getName());
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        this.getLocation(), this.getModuleName(), this.getDoc(), this.name, this.uses);
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

    final PackageDefinition other = (PackageDefinition) obj;
    return Objects.equals(other.getLocation(), this.getLocation())
        && Objects.equals(other.getModuleName(), this.getModuleName())
        && Objects.equals(other.getDoc(), this.getDoc())
        && Objects.equals(other.name, this.name)
        && Objects.equals(other.uses, this.uses);
  }
}
