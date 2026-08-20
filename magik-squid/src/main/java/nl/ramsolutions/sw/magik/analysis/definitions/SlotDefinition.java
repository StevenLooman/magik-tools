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
   * @param location Location of definition.
   * @param timestamp Timestamp of definition.
   * @param moduleName Name of module this slot is defined in.
   * @param doc Slot doc.
   * @param node Node for definition.
   * @param ownerTypeName Name of the exemplar owning this slot.
   * @param name Name of slot.
   * @param typeName Type of slot.
   */
  @SuppressWarnings({"checkstyle:ParameterNumber", "java:S107"})
  public SlotDefinition(
      final @Nullable Location location,
      final @Nullable Instant timestamp,
      final @Nullable String moduleName,
      final @Nullable String doc,
      final @Nullable AstNode node,
      final TypeString ownerTypeName,
      final String name,
      final TypeString typeName) {
    this(
        location,
        timestamp,
        moduleName,
        doc,
        node,
        ownerTypeName,
        name,
        typeName,
        Provenance.UNKNOWN);
  }

  /**
   * Constructor.
   *
   * @param location Location of definition.
   * @param timestamp Timestamp of definition.
   * @param moduleName Name of module this slot is defined in.
   * @param doc Slot doc.
   * @param node Node for definition.
   * @param ownerTypeName Name of the exemplar owning this slot.
   * @param name Name of slot.
   * @param typeName Type of slot.
   * @param provenance Provenance.
   */
  @SuppressWarnings({"checkstyle:ParameterNumber", "java:S107"})
  public SlotDefinition(
      final @Nullable Location location,
      final @Nullable Instant timestamp,
      final @Nullable String moduleName,
      final @Nullable String doc,
      final @Nullable AstNode node,
      final TypeString ownerTypeName,
      final String name,
      final TypeString typeName,
      final Provenance provenance) {
    super(location, timestamp, moduleName, doc, node, provenance);
    this.ownerTypeName = ownerTypeName;
    this.name = name;
    this.typeName = typeName;
  }

  public TypeString getOwnerTypeName() {
    return this.ownerTypeName;
  }

  @Override
  public String getName() {
    return this.name;
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
        this.typeName,
        this.getProvenance());
  }

  @Override
  public SlotDefinition withProvenance(final Provenance provenance) {
    return new SlotDefinition(
        this.getLocation(),
        this.getTimestamp(),
        this.getModuleName(),
        this.getDoc(),
        this.getNode(),
        this.ownerTypeName,
        this.name,
        this.typeName,
        provenance);
  }

  @Override
  public String toString() {
    return "%s@%s(%s.%s)"
        .formatted(
            this.getClass().getName(),
            Integer.toHexString(this.hashCode()),
            this.ownerTypeName.getFullString(),
            this.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        this.getLocation(),
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
        && Objects.equals(other.getModuleName(), this.getModuleName())
        && Objects.equals(other.getDoc(), this.getDoc())
        && Objects.equals(other.ownerTypeName, this.ownerTypeName)
        && Objects.equals(other.name, this.name)
        && Objects.equals(other.typeName, this.typeName);
  }
}
