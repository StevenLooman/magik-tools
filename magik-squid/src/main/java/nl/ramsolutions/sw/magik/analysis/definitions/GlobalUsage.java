package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Objects;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;

/** Global usage. */
public class GlobalUsage {

  private final TypeString typeName;
  private final @Nullable Location location;
  private final @Nullable AstNode node;

  /**
   * Constructor.
   *
   * @param typeName Type name.
   * @param location Location of use.
   */
  public GlobalUsage(
      final TypeString typeName, final @Nullable Location location, final @Nullable AstNode node) {
    this.typeName = typeName;
    this.location = location;
    this.node = node;
  }

  public TypeString getTypeName() {
    return this.typeName;
  }

  @CheckForNull
  public Location getLocation() {
    return this.location;
  }

  @CheckForNull
  public AstNode getNode() {
    return this.node;
  }

  public GlobalUsage getWithoutNode() {
    return new GlobalUsage(this.typeName, this.location, null);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.typeName);
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

    final GlobalUsage other = (GlobalUsage) obj;
    // Location is not tested!
    return Objects.equals(other.getTypeName(), this.getTypeName());
  }
}
