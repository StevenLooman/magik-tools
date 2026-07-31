package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Objects;
import nl.ramsolutions.sw.Usage;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;

/** Binary operator usage. */
public class BinaryOperatorUsage implements Usage {

  private final TypeString lhsTypeName;
  private final TypeString rhsTypeName;
  private final String operator;
  private final @Nullable Location location;
  private final @Nullable AstNode node;

  /**
   * Constructor.
   *
   * @param lhsTypeName Type name of left hand side.
   * @param rhsTypeName Type name of right hand side.
   * @param operator Operator.
   * @param location Location of use.
   * @param node Node of use.
   */
  public BinaryOperatorUsage(
      final TypeString lhsTypeName,
      final TypeString rhsTypeName,
      final String operator,
      final @Nullable Location location,
      final @Nullable AstNode node) {
    this.lhsTypeName = lhsTypeName;
    this.rhsTypeName = rhsTypeName;
    this.operator = operator;
    this.location = location;
    this.node = node;
  }

  public TypeString getLhsTypeName() {
    return this.lhsTypeName;
  }

  public TypeString getRhsTypeName() {
    return this.rhsTypeName;
  }

  public String getOperator() {
    return this.operator;
  }

  @CheckForNull
  public Location getLocation() {
    return this.location;
  }

  @CheckForNull
  public AstNode getNode() {
    return this.node;
  }

  public BinaryOperatorUsage getWithoutNode() {
    return new BinaryOperatorUsage(
        this.lhsTypeName, this.rhsTypeName, this.operator, this.location, null);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.lhsTypeName, this.rhsTypeName, this.operator);
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

    final BinaryOperatorUsage other = (BinaryOperatorUsage) obj;
    // Location is not tested!
    return Objects.equals(other.getLhsTypeName(), this.getLhsTypeName())
        && Objects.equals(other.getRhsTypeName(), this.getRhsTypeName())
        && Objects.equals(other.getOperator(), this.getOperator());
  }
}
