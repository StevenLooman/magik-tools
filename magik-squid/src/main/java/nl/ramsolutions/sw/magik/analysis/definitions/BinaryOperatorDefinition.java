package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.time.Instant;
import java.util.Objects;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;

/** Binary operator definition. */
public class BinaryOperatorDefinition extends MagikDefinition {

  private final String operator;
  private final TypeString lhsTypeName;
  private final TypeString rhsTypeName;
  private final TypeString resultTypeName;

  /**
   * Constructor.
   *
   * @param moduleName Module name.
   * @param node Node for definition.
   * @param operator Operator name.
   * @param lhsTypeName Left Hand Side type.
   * @param rhsTypeName Right Hand Side type.
   * @param resultTypeName Result type.
   */
  @SuppressWarnings({"checkstyle:ParameterNumber", "java:S107"})
  public BinaryOperatorDefinition(
      final @Nullable Location location,
      final @Nullable Instant timestamp,
      final @Nullable String moduleName,
      final @Nullable String doc,
      final @Nullable AstNode node,
      final String operator,
      final TypeString lhsTypeName,
      final TypeString rhsTypeName,
      final TypeString resultTypeName) {
    super(location, timestamp, moduleName, doc, node);

    if (!lhsTypeName.isSingle()) {
      throw new IllegalStateException();
    }

    if (!rhsTypeName.isSingle()) {
      throw new IllegalStateException();
    }

    this.operator = operator;
    this.lhsTypeName = lhsTypeName;
    this.rhsTypeName = rhsTypeName;
    this.resultTypeName = resultTypeName;
  }

  public String getOperator() {
    return this.operator;
  }

  public TypeString getLhsTypeName() {
    return this.lhsTypeName;
  }

  public TypeString getRhsTypeName() {
    return this.rhsTypeName;
  }

  public TypeString getResultTypeName() {
    return this.resultTypeName;
  }

  @Override
  public String getName() {
    return this.lhsTypeName.getFullString()
        + " "
        + this.operator
        + " "
        + this.rhsTypeName.getFullString();
  }

  @Override
  public String getPackage() {
    return null;
  }

  @Override
  public MagikDefinition getWithoutNode() {
    return new BinaryOperatorDefinition(
        this.getLocation(),
        this.getTimestamp(),
        this.getModuleName(),
        this.getDoc(),
        null,
        this.operator,
        this.lhsTypeName,
        this.rhsTypeName,
        this.resultTypeName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        this.getLocation(),
        this.getTimestamp(),
        this.getModuleName(),
        this.getDoc(),
        this.operator,
        this.lhsTypeName,
        this.rhsTypeName,
        this.resultTypeName);
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

    final BinaryOperatorDefinition other = (BinaryOperatorDefinition) obj;
    return Objects.equals(this.getLocation(), other.getLocation())
        && Objects.equals(this.getModuleName(), other.getModuleName())
        && Objects.equals(this.getDoc(), other.getDoc())
        && Objects.equals(this.operator, other.operator)
        && Objects.equals(this.lhsTypeName, other.lhsTypeName)
        && Objects.equals(this.rhsTypeName, other.rhsTypeName)
        && Objects.equals(this.resultTypeName, other.resultTypeName);
  }
}
