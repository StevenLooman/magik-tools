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
  private final Set<GlobalUsage> usedGlobals;
  private final Set<MethodUsage> usedMethods;
  private final Set<ConditionUsage> usedConditions;

  /**
   * Constructor.
   *
   * @param moduleName Module name.
   * @param node Node.
   * @param modifiers Modifiers.
   * @param typeName Type name.
   * @param procedureName Procedure name.
   * @param parameters Parameters.
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
      final ExpressionResultString returnTypes,
      final ExpressionResultString loopTypes) {
    super(location, timestamp, moduleName, doc, node);
    this.modifiers = Set.copyOf(modifiers);
    this.typeName = typeName;
    this.procedureName = procedureName;
    this.parameters = List.copyOf(parameters);
    this.returnTypes = returnTypes;
    this.loopTypes = loopTypes;
    this.usedGlobals = Collections.emptySet();
    this.usedMethods = Collections.emptySet();
    this.usedConditions = Collections.emptySet();
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
      final ExpressionResultString returnTypes,
      final ExpressionResultString loopTypes,
      final Set<GlobalUsage> usedGlobals,
      final Set<MethodUsage> usedMethods,
      final Set<ConditionUsage> usedConditions) {
    super(location, timestamp, moduleName, doc, node);
    this.modifiers = Set.copyOf(modifiers);
    this.typeName = typeName;
    this.procedureName = procedureName;
    this.parameters = List.copyOf(parameters);
    this.returnTypes = returnTypes;
    this.loopTypes = loopTypes;
    this.usedGlobals = Collections.unmodifiableSet(usedGlobals);
    this.usedMethods = Collections.unmodifiableSet(usedMethods);
    this.usedConditions = Collections.unmodifiableSet(usedConditions);
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

  /**
   * Get topics.
   *
   * @return Topics.
   */
  public Set<String> getTopics() {
    // TODO: Implement.
    return Collections.emptySet();
  }

  public Set<GlobalUsage> getUsedGlobals() {
    return Collections.unmodifiableSet(this.usedGlobals);
  }

  public Set<MethodUsage> getUsedMethods() {
    return Collections.unmodifiableSet(this.usedMethods);
  }

  public Set<ConditionUsage> getUsedConditions() {
    return Collections.unmodifiableSet(this.usedConditions);
  }

  @Override
  public String getPackage() {
    if (this.typeName != null) {
      return this.typeName.getPakkage();
    }

    return TypeString.UNDEFINED.getPakkage();
  }

  @Override
  public ProcedureDefinition getWithoutNode() {
    return new ProcedureDefinition(
        this.getLocation(),
        this.getTimestamp(),
        this.getModuleName(),
        this.getDoc(),
        null,
        this.modifiers,
        this.typeName,
        this.procedureName,
        this.parameters.stream().map(ParameterDefinition::getWithoutNode).toList(),
        this.returnTypes,
        this.loopTypes,
        this.usedGlobals,
        this.usedMethods,
        this.usedConditions);
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
        && Objects.equals(this.returnTypes, other.returnTypes)
        && Objects.equals(this.loopTypes, other.loopTypes);
  }
}
