package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.time.Instant;
import java.util.Objects;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;

/** Inheritance definition: one directed edge {@code child extends parent}. */
public class InheritanceDefinition extends MagikDefinition {

  private final TypeString childTypeName;
  private final TypeString parentTypeName;

  /**
   * Constructor.
   *
   * @param location Location of definition.
   * @param timestamp Timestamp of definition.
   * @param moduleName Name of module this edge is defined in.
   * @param doc Doc.
   * @param node Node for definition.
   * @param childTypeName Child (owner) type.
   * @param parentTypeName Parent type the child extends.
   */
  @SuppressWarnings({"checkstyle:ParameterNumber", "java:S107"})
  public InheritanceDefinition(
      final @Nullable Location location,
      final @Nullable Instant timestamp,
      final @Nullable String moduleName,
      final @Nullable String doc,
      final @Nullable AstNode node,
      final TypeString childTypeName,
      final TypeString parentTypeName) {
    this(
        location,
        timestamp,
        moduleName,
        doc,
        node,
        childTypeName,
        parentTypeName,
        Provenance.UNKNOWN);
  }

  /**
   * Constructor.
   *
   * @param location Location of definition.
   * @param timestamp Timestamp of definition.
   * @param moduleName Name of module this edge is defined in.
   * @param doc Doc.
   * @param node Node for definition.
   * @param childTypeName Child (owner) type.
   * @param parentTypeName Parent type the child extends.
   * @param provenance Provenance.
   */
  @SuppressWarnings({"checkstyle:ParameterNumber", "java:S107"})
  public InheritanceDefinition(
      final @Nullable Location location,
      final @Nullable Instant timestamp,
      final @Nullable String moduleName,
      final @Nullable String doc,
      final @Nullable AstNode node,
      final TypeString childTypeName,
      final TypeString parentTypeName,
      final Provenance provenance) {
    super(location, timestamp, moduleName, doc, node, provenance);
    this.childTypeName = childTypeName;
    this.parentTypeName = parentTypeName;
  }

  public TypeString getChildTypeName() {
    return this.childTypeName;
  }

  public TypeString getParentTypeName() {
    return this.parentTypeName;
  }

  @Override
  public String getName() {
    return this.parentTypeName.getFullString();
  }

  @Override
  public InheritanceDefinition getBareDefinition() {
    return new InheritanceDefinition(
        this.getLocation(),
        this.getTimestamp(),
        this.getModuleName(),
        this.getDoc(),
        null,
        this.childTypeName,
        this.parentTypeName,
        this.getProvenance());
  }

  @Override
  public InheritanceDefinition withProvenance(final Provenance provenance) {
    return new InheritanceDefinition(
        this.getLocation(),
        this.getTimestamp(),
        this.getModuleName(),
        this.getDoc(),
        this.getNode(),
        this.childTypeName,
        this.parentTypeName,
        provenance);
  }

  @Override
  public String toString() {
    return "%s@%s(%s -> %s)"
        .formatted(
            this.getClass().getName(),
            Integer.toHexString(this.hashCode()),
            this.childTypeName.getFullString(),
            this.parentTypeName.getFullString());
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        this.getLocation(),
        this.getModuleName(),
        this.getDoc(),
        this.childTypeName,
        this.parentTypeName);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || this.getClass() != obj.getClass()) {
      return false;
    }
    final InheritanceDefinition other = (InheritanceDefinition) obj;
    return Objects.equals(this.getLocation(), other.getLocation())
        && Objects.equals(this.getModuleName(), other.getModuleName())
        && Objects.equals(this.getDoc(), other.getDoc())
        && Objects.equals(this.childTypeName, other.childTypeName)
        && Objects.equals(this.parentTypeName, other.parentTypeName);
  }
}
