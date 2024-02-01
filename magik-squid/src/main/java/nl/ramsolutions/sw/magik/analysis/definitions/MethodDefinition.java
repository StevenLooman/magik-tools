package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/**
 * Method definition.
 */
@Immutable
public class MethodDefinition extends Definition {

    /**
     * Method definition modifier.
     */
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
    private final ExpressionResultString returnTypes;
    private final ExpressionResultString loopTypes;
    private final Set<GlobalUsage> usedGlobals;
    private final Set<MethodUsage> usedMethods;
    private final Set<SlotUsage> usedSlots;
    private final Set<ConditionUsage> usedConditions;

    /**
     * Constructor.
     * @param moduleName Name of module this method is defined in.
     * @param node Node for definition.
     * @param typeName Name of exemplar.
     * @param methodName Name of method.
     * @param modifiers Modifiers for method.
     * @param parameters Parameters for method.
     * @param assignmentParameter Assignment parameter.
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
            final ExpressionResultString returnTypes,
            final ExpressionResultString loopTypes) {
        super(location, moduleName, doc, node);
        this.typeName = typeName;
        this.methodName = methodName;
        this.modifiers = Set.copyOf(modifiers);
        this.parameters = List.copyOf(parameters);
        this.assignmentParameter = assignmentParameter;
        this.returnTypes = returnTypes;
        this.loopTypes = loopTypes;
        this.usedGlobals = Collections.emptySet();
        this.usedMethods = Collections.emptySet();
        this.usedSlots = Collections.emptySet();
        this.usedConditions = Collections.emptySet();
    }

    /**
     * Constructor.
     * @param moduleName Name of module this method is defined in.
     * @param node Node for definition.
     * @param typeName Name of exemplar.
     * @param methodName Name of method.
     * @param modifiers Modifiers for method.
     * @param parameters Parameters for method.
     * @param assignmentParameter Assignment parameter.
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
        this.returnTypes = returnTypes;
        this.loopTypes = loopTypes;
        this.usedGlobals = Collections.unmodifiableSet(usedGlobals);
        this.usedMethods = Collections.unmodifiableSet(usedMethods);
        this.usedSlots = Collections.unmodifiableSet(usedSlots);
        this.usedConditions = Collections.unmodifiableSet(usedConditions);
    }

    /**
     * Get exemplar name.
     * @return Name of exemplar.
     */
    public TypeString getTypeName() {
        return this.typeName;
    }

    /**
     * Get method name.
     * @return Name of method.
     */
    public String getMethodName() {
        return this.methodName;
    }

    @Override
    public String getName() {
        return this.methodName.startsWith("[")
            ? typeName.getFullString() + methodName
            : typeName.getFullString() + "." + methodName;
    }

    /**
     * Get modifiers.
     * @return Modifiers.
     */
    public Set<Modifier> getModifiers() {
        return Collections.unmodifiableSet(this.modifiers);
    }

    /**
     * Get parameters.
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
     * Test if method definition is an actual {@code _method ... _endmethod}, or a
     * shared constant/variable/slot accessor.
     * @return True if actual method, false otherwise.
     */
    public boolean isActualMethodDefinition() {
        final AstNode node = this.getNode();
        Objects.requireNonNull(node);
        return node.is(MagikGrammar.METHOD_DEFINITION);
    }

    /**
     * Get assignment parameter.
     * @return Assignment parameter.
     */
    @CheckForNull
    public ParameterDefinition getAssignmentParameter() {
        return this.assignmentParameter;
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
            this.parameters.stream()
                .map(ParameterDefinition::getWithoutNode)
                .collect(Collectors.toList()),
            this.assignmentParameter != null
                ? this.assignmentParameter.getWithoutNode()
                : null,
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
            this.getClass().getName(), Integer.toHexString(this.hashCode()),
            this.getTypeName().getFullString(), this.getMethodName());
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
            && Objects.equals(this.returnTypes, other.returnTypes)
            && Objects.equals(this.loopTypes, other.loopTypes);
    }

}
