package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.Nullable;
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
      final @Nullable String moduleName,
      final @Nullable String doc,
      final @Nullable AstNode node,
      final String name,
      final List<String> uses) {
    super(location, moduleName, doc, node);
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
  public String getPackage() {
    return this.name;
  }

  @Override
  public PackageDefinition getWithoutNode() {
    return new PackageDefinition(
        this.getLocation(), this.getModuleName(), this.getDoc(), null, name, uses);
  }

  @Override
  public String toString() {
    return String.format(
        "%s@%s(%s)",
        this.getClass().getName(), Integer.toHexString(this.hashCode()), this.getName());
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
        && Objects.equals(other.getName(), this.getName())
        && Objects.equals(other.getDoc(), this.getDoc())
        && Objects.equals(other.name, this.name)
        && Objects.equals(other.uses, this.uses);
  }
}
