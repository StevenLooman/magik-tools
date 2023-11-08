package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/**
 * Method definition.
 */
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
    private final ParameterDefinition assignmentParameter;
    private final ExpressionResultString returnTypes;
    private final ExpressionResultString loopTypes;
    private final Set<GlobalUsage> usedGlobals = new HashSet<>();
    private final Set<MethodUsage> usedMethods = new HashSet<>();
    private final Set<SlotUsage> usedSlots = new HashSet<>();
    private final Set<ConditionUsage> usedConditions = new HashSet<>();

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
    @SuppressWarnings("checkstyle:ParameterNumber")
    public MethodDefinition(
            final @Nullable Location location,
            final @Nullable String moduleName,
            final @Nullable AstNode node,
            final TypeString typeName,
            final String methodName,
            final Set<Modifier> modifiers,
            final List<ParameterDefinition> parameters,
            final @Nullable ParameterDefinition assignmentParameter,
            final String doc,
            final ExpressionResultString returnTypes,
            final ExpressionResultString loopTypes) {
        super(location, moduleName, node, doc);
        this.typeName = typeName;
        this.methodName = methodName;
        this.modifiers = Set.copyOf(modifiers);
        this.parameters = List.copyOf(parameters);
        this.assignmentParameter = assignmentParameter;
        this.returnTypes = returnTypes;
        this.loopTypes = loopTypes;
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
            ? typeName.getIdentifier() + methodName
            : typeName.getIdentifier() + "." + methodName;
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
        return this.getNode().is(MagikGrammar.METHOD_DEFINITION);
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

}
