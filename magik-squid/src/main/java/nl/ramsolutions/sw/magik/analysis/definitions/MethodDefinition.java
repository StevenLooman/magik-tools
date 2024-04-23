package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/** Method definition. */
public class MethodDefinition extends Definition {

  /** Method definition modifier. */
  @SuppressWarnings("checkstyle:JavadocVariable")
  public enum Modifier {
    PRIVATE,
    ABSTRACT,
    ITER,
  }

  private final Set<Modifier> modifiers;
  private final TypeString typeName;
  private final String methodName;
  private final List<ParameterDefinition> parameters;
  private final @Nullable ParameterDefinition assignmentParameter;
  private final Set<String> topics;
  private final ExpressionResultString returnTypes;
  private final ExpressionResultString loopTypes;
  private final Set<GlobalUsage> usedGlobals;
  private final Set<MethodUsage> usedMethods;
  private final Set<SlotUsage> usedSlots;
  private final Set<ConditionUsage> usedConditions;

  /**
   * Constructor.
   *
   * @param moduleName Name of module this method is defined in.
   * @param node Node for definition.
   * @param typeName Name of exemplar.
   * @param methodName Name of method.
   * @param modifiers Modifiers for method.
   * @param parameters Parameters for method.
   * @param assignmentParameter Assignment parameter.
   * @param topics Topics.
   * @param doc Method doc.
   * @param returnTypes Return types.
   * @param loopTypes Loop types.
   */
  @SuppressWarnings({"checkstyle:ParameterNumber", "java:S107"})
  public MethodDefinition(
      final @Nullable Location location,
      final @Nullable String moduleName,
      final @Nullable String doc,
      final @Nullable AstNode node,
      final TypeString typeName,
      final String methodName,
      final Set<Modifier> modifiers,
      final List<ParameterDefinition> parameters,
      final @Nullable ParameterDefinition assignmentParameter,
      final Set<String> topics,
      final ExpressionResultString returnTypes,
      final ExpressionResultString loopTypes) {
    super(location, moduleName, doc, node);
    this.typeName = typeName;
    this.methodName = methodName;
    this.modifiers = Set.copyOf(modifiers);
    this.parameters = List.copyOf(parameters);
    this.assignmentParameter = assignmentParameter;
    this.topics = Set.copyOf(topics);
    this.returnTypes = returnTypes;
    this.loopTypes = loopTypes;
    this.usedGlobals = Collections.emptySet();
    this.usedMethods = Collections.emptySet();
    this.usedSlots = Collections.emptySet();
    this.usedConditions = Collections.emptySet();
  }

  /**
   * Constructor.
   *
   * @param moduleName Name of module this method is defined in.
   * @param node Node for definition.
   * @param typeName Name of exemplar.
   * @param methodName Name of method.
   * @param modifiers Modifiers for method.
   * @param parameters Parameters for method.
   * @param assignmentParameter Assignment parameter.
   * @param topics Topics.
   * @param doc Method doc.
   * @param returnTypes Return types.
   * @param loopTypes Loop types.
   */
  @SuppressWarnings({"checkstyle:ParameterNumber", "java:S107"})
  public MethodDefinition(
      final @Nullable Location location,
      final @Nullable String moduleName,
      final @Nullable String doc,
      final @Nullable AstNode node,
      final TypeString typeName,
      final String methodName,
      final Set<Modifier> modifiers,
      final List<ParameterDefinition> parameters,
      final @Nullable ParameterDefinition assignmentParameter,
      final Set<String> topics,
      final ExpressionResultString returnTypes,
      final ExpressionResultString loopTypes,
      final Set<GlobalUsage> usedGlobals,
      final Set<MethodUsage> usedMethods,
      final Set<SlotUsage> usedSlots,
      final Set<ConditionUsage> usedConditions) {
    super(location, moduleName, doc, node);
    this.typeName = typeName;
    this.methodName = methodName;
    this.modifiers = Set.copyOf(modifiers);
    this.parameters = List.copyOf(parameters);
    this.assignmentParameter = assignmentParameter;
    this.topics = Set.copyOf(topics);
    this.returnTypes = returnTypes;
    this.loopTypes = loopTypes;
    this.usedGlobals = Collections.unmodifiableSet(usedGlobals);
    this.usedMethods = Collections.unmodifiableSet(usedMethods);
    this.usedSlots = Collections.unmodifiableSet(usedSlots);
    this.usedConditions = Collections.unmodifiableSet(usedConditions);
  }

  /**
   * Get exemplar name.
   *
   * @return Name of exemplar.
   */
  public TypeString getTypeName() {
    return this.typeName;
  }

  /**
   * Get method name.
   *
   * @return Name of method.
   */
  public String getMethodName() {
    return this.methodName;
  }

  public String getMethodNameWithParameters() {
    final StringBuilder builder = new StringBuilder();

    // Determine method name with parameters.
    final String methodName = this.getMethodName();
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
      if (currentModifier != newModifier) {
        if (newModifier != ParameterDefinition.Modifier.NONE) {
          parametersBuilder.append("_" + newModifier.name().toLowerCase());
          parametersBuilder.append(" ");
        }
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
      final int bracketIndex = methodName.indexOf('(');
      if (bracketIndex != -1) {
        builder.append(methodName.substring(0, bracketIndex + 1));
        builder.append(parametersStr);
        builder.append(methodName.substring(bracketIndex + 1));
      } else {
        builder.append(methodName);
      }
    }

    final String assignmentParameterName =
        this.assignmentParameter != null ? this.assignmentParameter.getName() : null;
    if (assignmentParameterName != null) {
      builder.append(assignmentParameterName);
    }

    return builder.toString();
  }

  public String getNameWithParameters() {
    final StringBuilder builder = new StringBuilder();

    // Type name.
    final String ownerName = this.getTypeName().getFullString();
    builder.append(ownerName);

    // Method name.
    final String methodNameWithParameters = this.getMethodNameWithParameters();
    if (this.methodName.startsWith("[]")) {
      builder.append(methodNameWithParameters);
    } else {
      builder.append(".");
      builder.append(methodNameWithParameters);
    }

    return builder.toString();
  }

  @Override
  public String getName() {
    return this.methodName.startsWith("[")
        ? this.typeName.getFullString() + this.methodName
        : this.typeName.getFullString() + "." + this.methodName;
  }

  /**
   * Get modifiers.
   *
   * @return Modifiers.
   */
  public Set<Modifier> getModifiers() {
    return Collections.unmodifiableSet(this.modifiers);
  }

  /**
   * Get parameters.
   *
   * @return Parameters.
   */
  public List<ParameterDefinition> getParameters() {
    return Collections.unmodifiableList(this.parameters);
  }

  public ExpressionResultString getReturnTypes() {
    return this.returnTypes;
  }

  public ExpressionResultString getLoopTypes() {
    return this.loopTypes;
  }

  /**
   * Test if method definition is an actual {@code _method ... _endmethod}, or a shared
   * constant/variable/slot accessor.
   *
   * @return True if actual method, false otherwise.
   */
  public boolean isActualMethodDefinition() {
    final AstNode node = this.getNode();
    Objects.requireNonNull(node);
    return node.is(MagikGrammar.METHOD_DEFINITION);
  }

  /**
   * Get assignment parameter.
   *
   * @return Assignment parameter.
   */
  @CheckForNull
  public ParameterDefinition getAssignmentParameter() {
    return this.assignmentParameter;
  }

  /**
   * Get topics.
   *
   * @return Topics.
   */
  public Set<String> getTopics() {
    return Collections.unmodifiableSet(this.topics);
  }

  public Set<GlobalUsage> getUsedGlobals() {
    return Collections.unmodifiableSet(this.usedGlobals);
  }

  public Set<MethodUsage> getUsedMethods() {
    return Collections.unmodifiableSet(this.usedMethods);
  }

  public Set<SlotUsage> getUsedSlots() {
    return Collections.unmodifiableSet(this.usedSlots);
  }

  public Set<ConditionUsage> getUsedConditions() {
    return Collections.unmodifiableSet(this.usedConditions);
  }

  @Override
  public String getPackage() {
    return this.typeName.getPakkage();
  }

  @Override
  public MethodDefinition getWithoutNode() {
    return new MethodDefinition(
        this.getLocation(),
        this.getModuleName(),
        this.getDoc(),
        null,
        this.typeName,
        this.methodName,
        this.modifiers,
        this.parameters.stream().map(ParameterDefinition::getWithoutNode).toList(),
        this.assignmentParameter != null ? this.assignmentParameter.getWithoutNode() : null,
        this.topics,
        this.returnTypes,
        this.loopTypes,
        this.usedGlobals,
        this.usedMethods,
        this.usedSlots,
        this.usedConditions);
  }

  @Override
  public String toString() {
    return String.format(
        "%s@%s(%s, %s)",
        this.getClass().getName(),
        Integer.toHexString(this.hashCode()),
        this.getTypeName().getFullString(),
        this.getMethodName());
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        this.getLocation(),
        this.getModuleName(),
        this.getDoc(),
        this.modifiers,
        this.typeName,
        this.methodName,
        this.parameters,
        this.assignmentParameter,
        this.topics,
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

    final MethodDefinition other = (MethodDefinition) obj;
    return Objects.equals(this.getLocation(), other.getLocation())
        && Objects.equals(this.getModuleName(), other.getModuleName())
        && Objects.equals(this.getDoc(), other.getDoc())
        && Objects.equals(this.modifiers, other.modifiers)
        && Objects.equals(this.typeName, other.typeName)
        && Objects.equals(this.methodName, other.methodName)
        && Objects.equals(this.parameters, other.parameters)
        && Objects.equals(this.assignmentParameter, other.assignmentParameter)
        && Objects.equals(this.topics, other.topics)
        && Objects.equals(this.returnTypes, other.returnTypes)
        && Objects.equals(this.loopTypes, other.loopTypes);
  }
}
