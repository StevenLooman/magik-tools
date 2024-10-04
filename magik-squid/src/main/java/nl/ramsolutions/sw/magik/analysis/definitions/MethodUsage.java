package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Objects;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;

/** Method usage. */
public class MethodUsage {

  // TODO: Shouldn't this have an (optional) AstNode, like the Definitions?

  private final TypeString typeName;
  private final String methodName;
  private final @Nullable Location location;
  private final @Nullable AstNode node;

  /**
   * Constructor.
   *
   * @param typeName Type name.
   * @param methodName Name of method.
   * @param location Location of use.
   */
  public MethodUsage(
      final TypeString typeName,
      final String methodName,
      final @Nullable Location location,
      final @Nullable AstNode node) {
    this.typeName = typeName;
    this.methodName = methodName;
    this.location = location;
    this.node = node;
  }

  /**
   * Constructor.
   *
   * @param typeRef Type reference.
   * @param methodName Name of method.
   */
  public MethodUsage(final TypeString typeRef, final String methodName) {
    this(typeRef, methodName, null, null);
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

  @CheckForNull
  public AstNode getNode() {
    return this.node;
  }

  public MethodUsage getWithoutNode() {
    return new MethodUsage(this.typeName, this.methodName, this.location, null);
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
