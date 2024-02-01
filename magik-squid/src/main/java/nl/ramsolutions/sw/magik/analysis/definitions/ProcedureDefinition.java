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

/**
 * Procedure definition.
 */
@Immutable
public class ProcedureDefinition extends TypeStringDefinition {

    /**
     * Procedure definition modifier.
     */
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
            final @Nullable String moduleName,
            final @Nullable String doc,
            final @Nullable AstNode node,
            final Set<Modifier> modifiers,
            final TypeString typeName,
            final @Nullable String procedureName,
            final List<ParameterDefinition> parameters,
            final ExpressionResultString returnTypes,
            final ExpressionResultString loopTypes) {
        super(location, moduleName, doc, node);
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
        super(location, moduleName, doc, node);
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

    public List<ParameterDefinition> getParameters() {
        return this.parameters;
    }

    public ExpressionResultString getReturnTypes() {
        return this.returnTypes;
    }

    public ExpressionResultString getLoopTypes() {
        return this.loopTypes;
    }

    @Override
    public String getName() {
        return this.procedureName;
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
            this.getModuleName(),
            this.getDoc(),
            null,
            this.modifiers,
            this.typeName,
            this.procedureName,
            this.parameters.stream()
                .map(ParameterDefinition::getWithoutNode)
                .collect(Collectors.toList()),
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
