package nl.ramsolutions.sw.magik.analysis.definitions;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Objects;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;

/** Method usage. */
public class MethodUsage {

  // TODO: Shouldn't this have an AstNode, like the Definitions?

  private final TypeString typeName;
  private final String methodName;
  private final Location location;

  /**
   * Constructor.
   *
   * @param typeName Type name.
   * @param methodName Name of method.
   * @param location Location of use.
   */
  public MethodUsage(
      final TypeString typeName, final String methodName, final @Nullable Location location) {
    this.typeName = typeName;
    this.methodName = methodName;
    this.location = location;
  }

  /**
   * Constructor.
   *
   * @param typeRef Type reference.
   * @param methodName Name of method.
   */
  public MethodUsage(final TypeString typeRef, final String methodName) {
    this(typeRef, methodName, null);
  }

  public TypeString getTypeName() {
    return this.typeName;
  }

  public String getMethodName() {
    return this.methodName;
  }

  @CheckForNull
  public Location getLocation() {
    return this.location;
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.typeName, this.methodName);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null) {
      return false;
    }

    if (this.getClass() != obj.getClass()) {
      return false;
    }

    final MethodUsage other = (MethodUsage) obj;
    // Location is not tested!
    return Objects.equals(other.getTypeName(), this.getTypeName())
        && Objects.equals(other.getMethodName(), this.getMethodName());
  }
}
