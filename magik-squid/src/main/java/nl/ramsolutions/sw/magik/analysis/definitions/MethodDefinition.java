package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
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
    private final TypeString exemplarName;
    private final String methodName;
    private final List<ParameterDefinition> parameters;
    private final ParameterDefinition assignmentParameter;

    /**
     * Constructor.
     * @param moduleName Name of module this method is defined in.
     * @param node Node for definition.
     * @param exemplarName Name of exemplar.
     * @param methodName Name of method.
     * @param modifiers Modifiers for method.
     * @param parameters Parameters for method.
     * @param assignmentParameter Assignment parameter.
     */
    public MethodDefinition(
            final @Nullable String moduleName,
            final AstNode node,
            final TypeString exemplarName,
            final String methodName,
            final Set<Modifier> modifiers,
            final List<ParameterDefinition> parameters,
            final @Nullable ParameterDefinition assignmentParameter) {
        super(moduleName, node, exemplarName);
        this.exemplarName = exemplarName;
        this.methodName = methodName;
        this.modifiers = Set.copyOf(modifiers);
        this.parameters = List.copyOf(parameters);
        this.assignmentParameter = assignmentParameter;
    }

    /**
     * Get exemplar name.
     * @return Name of exemplar.
     */
    public TypeString getExemplarName() {
        return this.exemplarName;
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
            ? exemplarName.getIdentifier() + methodName
            : exemplarName.getIdentifier() + "." + methodName;
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

}
