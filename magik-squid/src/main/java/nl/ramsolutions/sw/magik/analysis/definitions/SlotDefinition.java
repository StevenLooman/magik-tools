package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.time.Instant;
import java.util.Objects;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;

/** Slot definition. */
public class SlotDefinition extends MagikDefinition {

  private final TypeString ownerTypeName;
  private final String name;
  private final TypeString typeName;

  /**
   * Constructor.
   *
   * @param location Location.
   * @param timestamp Timestamp.
   * @param moduleName Module name.
   * @param doc Documentation.
   * @param node Node.
   * @param ownerTypeName Owner TypeString.
   * @param name Name.
   * @param typeName Type name.
   */
  public SlotDefinition(
      final @Nullable Location location,
      final @Nullable Instant timestamp,
      final @Nullable String moduleName,
      final @Nullable String doc,
      final @Nullable AstNode node,
      final TypeString ownerTypeName,
      final String name,
      final TypeString typeName) {
    super(location, timestamp, moduleName, doc, node);
    this.ownerTypeName = ownerTypeName;
    this.name = name;
    this.typeName = typeName;
  }

  public TypeString getOwnerTypeName() {
    return this.ownerTypeName;
  }

  public String getName() {
    return name;
  }

  public TypeString getTypeName() {
    return this.typeName;
  }

  @Override
  public SlotDefinition getBareDefinition() {
    return new SlotDefinition(
        this.getLocation(),
        this.getTimestamp(),
        this.getModuleName(),
        this.getDoc(),
        null,
        this.ownerTypeName,
        this.name,
        this.typeName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        this.getLocation(),
        this.getTimestamp(),
        this.getModuleName(),
        this.getDoc(),
        this.ownerTypeName,
        this.name,
        this.typeName);
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
        && Objects.equals(other.ownerTypeName, this.ownerTypeName)
        && Objects.equals(other.name, this.name)
        && Objects.equals(other.typeName, this.typeName);
  }
}
