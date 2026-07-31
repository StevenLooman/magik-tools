package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;

/** Definition of a global. */
public class GlobalDefinition extends MagikDefinition implements ITypeStringDefinition {

  private final TypeString typeName;
  private final TypeString aliasedTypeName;
  private final @Nullable Pragma pragma;
  private final List<MethodUsage> usedMethods;
  private final List<GlobalUsage> usedGlobals;
  private final List<BinaryOperatorUsage> usedBinaryOperators;

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
    this(
        location,
        timestamp,
        moduleName,
        doc,
        node,
        typeName,
        aliasedTypeName,
        pragma,
        Collections.emptyList());
  }

  /**
   * Constructor.
   *
   * @param moduleName Module name.
   * @param node Node.
   * @param typeName Type name.
   * @param aliasedTypeName Aliased type name.
   * @param usedMethods Methods used by the global's initializing expression.
   */
  @SuppressWarnings({"checkstyle:ParameterNumber", "java:S107"})
  public GlobalDefinition(
      final @Nullable Location location,
      final @Nullable Instant timestamp,
      final @Nullable String moduleName,
      final @Nullable String doc,
      final @Nullable AstNode node,
      final TypeString typeName,
      final TypeString aliasedTypeName,
      final @Nullable Pragma pragma,
      final List<MethodUsage> usedMethods) {
    this(
        location,
        timestamp,
        moduleName,
        doc,
        node,
        typeName,
        aliasedTypeName,
        pragma,
        usedMethods,
        Collections.emptyList(),
        Collections.emptyList());
  }

  /**
   * Constructor.
   *
   * @param typeName Type name.
   * @param aliasedTypeName Aliased type name.
   * @param usedMethods Methods used by the global's initializing expression.
   * @param usedGlobals Globals used by the global's initializing expression.
   * @param usedBinaryOperators Binary operators used by the global's initializing expression.
   */
  @SuppressWarnings({"checkstyle:ParameterNumber", "java:S107"})
  public GlobalDefinition(
      final @Nullable Location location,
      final @Nullable Instant timestamp,
      final @Nullable String moduleName,
      final @Nullable String doc,
      final @Nullable AstNode node,
      final TypeString typeName,
      final TypeString aliasedTypeName,
      final @Nullable Pragma pragma,
      final List<MethodUsage> usedMethods,
      final List<GlobalUsage> usedGlobals,
      final List<BinaryOperatorUsage> usedBinaryOperators) {
    super(location, timestamp, moduleName, doc, node);
    this.typeName = typeName;
    this.aliasedTypeName = aliasedTypeName;
    this.pragma = pragma;
    this.usedMethods = List.copyOf(usedMethods);
    this.usedGlobals = List.copyOf(usedGlobals);
    this.usedBinaryOperators = List.copyOf(usedBinaryOperators);
  }

  public List<MethodUsage> getUsedMethods() {
    return Collections.unmodifiableList(this.usedMethods);
  }

  public List<GlobalUsage> getUsedGlobals() {
    return Collections.unmodifiableList(this.usedGlobals);
  }

  public List<BinaryOperatorUsage> getUsedBinaryOperators() {
    return Collections.unmodifiableList(this.usedBinaryOperators);
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
        this.pragma != null ? this.pragma.getBarePragma() : null,
        this.usedMethods.stream().map(MethodUsage::getWithoutNode).toList(),
        this.usedGlobals.stream().map(GlobalUsage::getWithoutNode).toList(),
        this.usedBinaryOperators.stream().map(BinaryOperatorUsage::getWithoutNode).toList());
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
