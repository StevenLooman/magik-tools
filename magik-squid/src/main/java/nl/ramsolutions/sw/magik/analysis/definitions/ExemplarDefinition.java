package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.time.Instant;
import java.util.Objects;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;

/** Exemplar definition. */
public class ExemplarDefinition extends MagikDefinition implements ITypeStringDefinition {

  /** Exemplar sort. */
  @SuppressWarnings("checkstyle:JavadocVariable")
  public enum Sort {
    UNDEFINED,
    SLOTTED,
    INDEXED,
    MIXIN,
    INTRINSIC,
    OBJECT;
  }

  private final Sort sort;
  private final TypeString typeName;
  private final @Nullable Pragma pragma;

  /**
   * Constructor.
   *
   * @param moduleName Name of module where this is defined.
   * @param node Node for definition.
   * @param sort Type of exemplar.
   * @param typeName Name of slotted exemplar.
   * @param pragma Pragma.
   */
  @SuppressWarnings({"checkstyle:ParameterNumber", "java:S107"})
  public ExemplarDefinition(
      final @Nullable Location location,
      final @Nullable Instant timestamp,
      final @Nullable String moduleName,
      final @Nullable String doc,
      final @Nullable AstNode node,
      final Sort sort,
      final TypeString typeName,
      final @Nullable Pragma pragma) {
    super(location, timestamp, moduleName, doc, node);

    if (!typeName.isSingle()) {
      throw new IllegalStateException();
    }

    this.sort = sort;
    this.typeName = typeName;
    this.pragma = pragma;
  }

  @CheckForNull
  public Pragma getPragma() {
    return this.pragma;
  }

  @Override
  public TypeString getTypeString() {
    return this.typeName;
  }

  public Sort getSort() {
    return this.sort;
  }

  @Override
  public String getName() {
    return this.typeName.getFullString();
  }

  @Override
  public ExemplarDefinition getBareDefinition() {
    return new ExemplarDefinition(
        this.getLocation(),
        this.getTimestamp(),
        this.getModuleName(),
        this.getDoc(),
        null,
        this.sort,
        this.typeName,
        this.pragma != null ? this.pragma.getBarePragma() : null);
  }

  @Override
  public String toString() {
    return "%s@%s(%s)"
        .formatted(
            this.getClass().getName(),
            Integer.toHexString(this.hashCode()),
            this.getTypeString().getFullString());
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        this.getLocation(),
        this.getModuleName(),
        this.getDoc(),
        this.sort,
        this.typeName,
        this.pragma);
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

    final ExemplarDefinition other = (ExemplarDefinition) obj;
    return Objects.equals(this.getLocation(), other.getLocation())
        && Objects.equals(this.getModuleName(), other.getModuleName())
        && Objects.equals(this.getDoc(), other.getDoc())
        && Objects.equals(this.sort, other.sort)
        && Objects.equals(this.typeName, other.typeName)
        && Objects.equals(this.pragma, other.pragma);
  }
}
