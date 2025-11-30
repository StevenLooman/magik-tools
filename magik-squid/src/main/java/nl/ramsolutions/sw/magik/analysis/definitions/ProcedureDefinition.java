package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;

/** Procedure definition. */
public class ProcedureDefinition extends MagikDefinition
    implements ITypeStringDefinition, ICallableDefinition {

  private static final String DEFAULT_NAME = "_unnamed";

  /** Procedure definition modifier. */
  @SuppressWarnings("checkstyle:JavadocVariable")
  public enum Modifier {
    ITER,
  }

  private final Set<Modifier> modifiers;
  private final TypeString typeName;
  private final @Nullable String procedureName;
  private final List<ParameterDefinition> parameters;
  private final ExpressionResultString returnTypes;
  private final ExpressionResultString loopTypes;
  private final List<GlobalUsage> usedGlobals;
  private final List<MethodUsage> usedMethods;
  private final List<ConditionUsage> usedConditions;
  private final @Nullable Pragma pragma;

  /**
   * Constructor.
   *
   * @param moduleName Module name.
   * @param node Node.
   * @param modifiers Modifiers.
   * @param typeName Type name.
   * @param procedureName Procedure name.
   * @param parameters Parameters.
   * @param pragma Pragma.
   * @param doc Doc.
   * @param returnTypes Return types.
   * @param loopTypes Loop types.
   */
  @SuppressWarnings({"checkstyle:ParameterNumber", "java:S107"})
  public ProcedureDefinition(
      final @Nullable Location location,
      final @Nullable Instant timestamp,
      final @Nullable String moduleName,
      final @Nullable String doc,
      final @Nullable AstNode node,
      final Set<Modifier> modifiers,
      final TypeString typeName,
      final @Nullable String procedureName,
      final List<ParameterDefinition> parameters,
      final @Nullable Pragma pragma,
      final ExpressionResultString returnTypes,
      final ExpressionResultString loopTypes) {
    super(location, timestamp, moduleName, doc, node);
    this.modifiers = Set.copyOf(modifiers);
    this.typeName = typeName;
    this.procedureName = procedureName;
    this.parameters = List.copyOf(parameters);
    this.pragma = pragma;
    this.returnTypes = returnTypes;
    this.loopTypes = loopTypes;
    this.usedGlobals = Collections.emptyList();
    this.usedMethods = Collections.emptyList();
    this.usedConditions = Collections.emptyList();
  }

  /**
   * Constructor.
   *
   * @param moduleName Module name.
   * @param node Node.
   * @param modifiers Modifiers.
   * @param typeName Type name.
   * @param procedureName Procedure name.
   * @param parameters Parameters.
   * @param pragma Pragma.
   * @param doc Doc.
   * @param returnTypes Return types.
   * @param loopTypes Loop types.
   */
  @SuppressWarnings({"checkstyle:ParameterNumber", "java:S107"})
  public ProcedureDefinition(
      final @Nullable Location location,
      final @Nullable Instant timestamp,
      final @Nullable String moduleName,
      final @Nullable String doc,
      final @Nullable AstNode node,
      final Set<Modifier> modifiers,
      final TypeString typeName,
      final @Nullable String procedureName,
      final List<ParameterDefinition> parameters,
      final @Nullable Pragma pragma,
      final ExpressionResultString returnTypes,
      final ExpressionResultString loopTypes,
      final List<GlobalUsage> usedGlobals,
      final List<MethodUsage> usedMethods,
      final List<ConditionUsage> usedConditions) {
    super(location, timestamp, moduleName, doc, node);
    this.modifiers = Set.copyOf(modifiers);
    this.typeName = typeName;
    this.procedureName = procedureName;
    this.parameters = List.copyOf(parameters);
    this.pragma = pragma;
    this.returnTypes = returnTypes;
    this.loopTypes = loopTypes;
    this.usedGlobals = Collections.unmodifiableList(usedGlobals);
    this.usedMethods = Collections.unmodifiableList(usedMethods);
    this.usedConditions = Collections.unmodifiableList(usedConditions);
  }

  public Set<Modifier> getModifiers() {
    return this.modifiers;
  }

  @Override
  public TypeString getTypeString() {
    return this.typeName;
  }

  @CheckForNull
  public String getProcedureName() {
    return this.procedureName;
  }

  public String getNameWithParameters() {
    final StringBuilder builder = new StringBuilder();

    // Type name.
    final String ownerName = this.getTypeString().getFullString();
    builder.append(ownerName);

    // Determine method name with parameters.
    final String methodName = "invoke()";
    final StringBuilder parametersBuilder = new StringBuilder();
    boolean firstParameter = true;
    ParameterDefinition.Modifier currentModifier = ParameterDefinition.Modifier.NONE;
    for (final ParameterDefinition parameterDefinition : this.parameters) {
      if (firstParameter) {
        firstParameter = false;
      } else {
        parametersBuilder.append(", ");
      }

      final ParameterDefinition.Modifier newModifier = parameterDefinition.getModifier();
      if (currentModifier != newModifier && newModifier != ParameterDefinition.Modifier.NONE) {
        parametersBuilder.append("_" + newModifier.name().toLowerCase());
        parametersBuilder.append(" ");
      }
      currentModifier = newModifier;

      parametersBuilder.append(parameterDefinition.getName());
    }
    final String parametersStr = parametersBuilder.toString();

    if (methodName.startsWith("[")) {
      builder.append("[");
      builder.append(parametersStr);
      builder.append(methodName.substring(1)); // "]<<" or "]^<<""
    } else {
      builder.append(".");
      int bracketIndex = methodName.indexOf('(');
      if (bracketIndex != -1) {
        builder.append(methodName.substring(0, bracketIndex + 1));
        builder.append(parametersStr);
        builder.append(methodName.substring(bracketIndex + 1));
      } else {
        builder.append(methodName);
      }
    }

    return builder.toString();
  }

  @Override
  public List<ParameterDefinition> getParameters() {
    return this.parameters;
  }

  @CheckForNull
  public Pragma getPragma() {
    return this.pragma;
  }

  @Override
  public ExpressionResultString getReturnTypes() {
    return this.returnTypes;
  }

  @Override
  public ExpressionResultString getLoopTypes() {
    return this.loopTypes;
  }

  @Override
  public String getName() {
    return Objects.requireNonNullElse(this.procedureName, ProcedureDefinition.DEFAULT_NAME);
  }

  public List<GlobalUsage> getUsedGlobals() {
    return Collections.unmodifiableList(this.usedGlobals);
  }

  public List<MethodUsage> getUsedMethods() {
    return Collections.unmodifiableList(this.usedMethods);
  }

  public List<ConditionUsage> getUsedConditions() {
    return Collections.unmodifiableList(this.usedConditions);
  }

  @Override
  public ProcedureDefinition getBareDefinition() {
    return new ProcedureDefinition(
        this.getLocation(),
        this.getTimestamp(),
        this.getModuleName(),
        this.getDoc(),
        null,
        this.modifiers,
        this.typeName,
        this.procedureName,
        this.parameters.stream().map(ParameterDefinition::getBareDefinition).toList(),
        this.pragma != null ? this.pragma.getBarePragma() : null,
        this.returnTypes,
        this.loopTypes,
        this.usedGlobals.stream().map(GlobalUsage::getWithoutNode).toList(),
        this.usedMethods.stream().map(MethodUsage::getWithoutNode).toList(),
        this.usedConditions.stream().map(ConditionUsage::getWithoutNode).toList());
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        this.getLocation(),
        this.getTimestamp(),
        this.getModuleName(),
        this.getDoc(),
        this.modifiers,
        this.typeName,
        this.procedureName,
        this.parameters,
        this.pragma,
        this.returnTypes,
        this.loopTypes);
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

    final ProcedureDefinition other = (ProcedureDefinition) obj;
    return Objects.equals(other.getLocation(), this.getLocation())
        && Objects.equals(other.getName(), this.getName())
        && Objects.equals(other.getDoc(), this.getDoc())
        && Objects.equals(this.modifiers, other.modifiers)
        && Objects.equals(this.typeName, other.typeName)
        && Objects.equals(this.procedureName, other.procedureName)
        && Objects.equals(this.parameters, other.parameters)
        && Objects.equals(this.pragma, other.pragma)
        && Objects.equals(this.returnTypes, other.returnTypes)
        && Objects.equals(this.loopTypes, other.loopTypes);
  }
}
