package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.time.Instant;
import java.util.Objects;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;

/** Definition of a global. */
public class GlobalDefinition extends MagikDefinition implements ITypeStringDefinition {

  private final TypeString typeName;
  private final TypeString aliasedTypeName;
  private final @Nullable Pragma pragma;

  /**
   * Constructor.
   *
   * @param moduleName Module name.
   * @param node Node.
   * @param typeName Type name.
   * @param aliasedTypeName Aliased type name.
   */
  public GlobalDefinition(
      final @Nullable Location location,
      final @Nullable Instant timestamp,
      final @Nullable String moduleName,
      final @Nullable String doc,
      final @Nullable AstNode node,
      final TypeString typeName,
      final TypeString aliasedTypeName,
      final @Nullable Pragma pragma) {
    super(location, timestamp, moduleName, doc, node);
    this.typeName = typeName;
    this.aliasedTypeName = aliasedTypeName;
    this.pragma = pragma;
  }

  public TypeString getTypeString() {
    return this.typeName;
  }

  public TypeString getAliasedTypeName() {
    return this.aliasedTypeName;
  }

  @Override
  public String getName() {
    return this.typeName.getFullString();
  }

  @CheckForNull
  public Pragma getPragma() {
    return this.pragma;
  }

  @Override
  public GlobalDefinition getBareDefinition() {
    return new GlobalDefinition(
        this.getLocation(),
        this.getTimestamp(),
        this.getModuleName(),
        this.getDoc(),
        null,
        this.typeName,
        this.aliasedTypeName,
        this.pragma != null ? this.pragma.getBarePragma() : null);
  }

  @Override
  public String toString() {
    return "%s@%s(%s, %s)"
        .formatted(
            this.getClass().getName(),
            Integer.toHexString(this.hashCode()),
            this.typeName.getFullString(),
            this.aliasedTypeName.getFullString());
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        this.getLocation(),
        this.getTimestamp(),
        this.getModuleName(),
        this.getDoc(),
        this.typeName,
        this.aliasedTypeName,
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

    final GlobalDefinition other = (GlobalDefinition) obj;
    return Objects.equals(this.getLocation(), other.getLocation())
        && Objects.equals(this.getModuleName(), other.getModuleName())
        && Objects.equals(this.getDoc(), other.getDoc())
        && Objects.equals(this.typeName, other.typeName)
        && Objects.equals(this.aliasedTypeName, other.aliasedTypeName)
        && Objects.equals(this.pragma, other.pragma);
  }
}
