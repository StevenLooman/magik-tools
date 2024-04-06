package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Objects;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;

/** Slot definition. */
public class SlotDefinition extends Definition {

  private final String name;
  private final TypeString typeName;

  /**
   * Constructor.
   *
   * @param location
   * @param node
   * @param name
   * @param typeName
   */
  public SlotDefinition(
      final @Nullable Location location,
      final @Nullable String moduleName,
      final @Nullable String doc,
      final @Nullable AstNode node,
      final String name,
      final TypeString typeName) {
    super(location, moduleName, doc, node);
    this.name = name;
    this.typeName = typeName;
  }

  public String getName() {
    return name;
  }

  public TypeString getTypeName() {
    return this.typeName;
  }

  @Override
  public String getPackage() {
    return null;
  }

  @Override
  public SlotDefinition getWithoutNode() {
    return new SlotDefinition(
        this.getLocation(), this.getModuleName(), this.getDoc(), null, this.name, this.typeName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        this.getLocation(), this.getModuleName(), this.getDoc(), this.name, this.typeName);
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

    final SlotDefinition other = (SlotDefinition) obj;
    return Objects.equals(other.getLocation(), this.getLocation())
        && Objects.equals(other.getName(), this.getName())
        && Objects.equals(other.getDoc(), this.getDoc())
        && Objects.equals(other.name, this.name)
        && Objects.equals(other.typeName, this.typeName);
  }
}
