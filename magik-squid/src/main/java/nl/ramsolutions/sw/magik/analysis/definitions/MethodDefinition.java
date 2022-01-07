package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import nl.ramsolutions.sw.magik.analysis.typing.types.GlobalReference;

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
    private final String exemplarName;
    private final String methodName;
    private final List<ParameterDefinition> parameters;
    private final ParameterDefinition assignmentParameter;

    /**
     * Constructor.
     * @param node Node for definition.
     * @param pakkage Package defined in.
     * @param exemplarName Name of exemplar.
     * @param name Name of method.
     * @param modifiers Modifiers for method.
     * @param parameters Parameters for method.
     * @param assignmentParameter Assignment parameter.
     */
    public MethodDefinition(
            final AstNode node,
            final String pakkage,
            final String exemplarName,
            final String name,
            final Set<Modifier> modifiers,
            final List<ParameterDefinition> parameters,
            final @Nullable ParameterDefinition assignmentParameter) {
        super(node, pakkage, name.startsWith("[") ? exemplarName + name : exemplarName + "." + name);
        this.exemplarName = exemplarName;
        this.methodName = name;
        this.modifiers = Set.copyOf(modifiers);
        this.parameters = List.copyOf(parameters);
        this.assignmentParameter = assignmentParameter;
    }

    /**
     * Get exemplar name.
     * @return Name of exemplar.
     */
    public String getExemplarName() {
        return this.exemplarName;
    }

    /**
     * Get method name.
     * @return Name of method.
     */
    public String getMethodName() {
        return methodName;
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
     * Get assignment parameter.
     * @return Assignment parameter.
     */
    @CheckForNull
    public ParameterDefinition getAssignmentParameter() {
        return this.assignmentParameter;
    }

    public GlobalReference getTypeGlobalReference() {
        return GlobalReference.of(this.getPackage(), this.getExemplarName());
    }

}
