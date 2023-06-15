package nl.ramsolutions.sw.magik.analysis.typing.types;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import nl.ramsolutions.sw.magik.analysis.Location;

/**
 * Procedure instance.
 */
public class ProcedureInstance extends AbstractType {

    /**
     * Serializer name for anonymouse procedure.
     */
    public static final String ANONYMOUS_PROCEDURE = "__anonymous_procedure";

    private final MagikType procedureType;
    private final String procedureName;
    private final Method invokeMethod;

    /**
     * Constructor, with loopbody types.
     * @param procedureName Name of method or procedure.
     */
    @SuppressWarnings("checkstyle:ParameterNumber")
    public ProcedureInstance(// NOSONAR
            final MagikType procedureType,
            final @Nullable Location location,
            final @Nullable String procedureName,
            final Set<Method.Modifier> modifiers,
            final List<Parameter> parameters,
            final @Nullable String methodDoc,
            final ExpressionResultString callResult,
            final ExpressionResultString loopbodyResult) {
        this.procedureType = procedureType;
        this.procedureName = procedureName != null
            ? procedureName
            : ANONYMOUS_PROCEDURE;
        this.invokeMethod = new Method(
            location,
            modifiers,
            this.procedureType,
            "invoke()",
            parameters,
            null,
            methodDoc,
            callResult,
            loopbodyResult);

        if (methodDoc != null) {
            this.setDoc(methodDoc);
        }
    }

    public String getProcedureName() {
        return this.procedureName;
    }

    @Override
    public Collection<Slot> getSlots() {
        return this.procedureType.getSlots();
    }

    @Override
    public List<GenericDeclaration> getGenerics() {
        return Collections.emptyList();
    }

    @Override
    public TypeString getTypeString() {
        return this.procedureType.getTypeString();
    }

    @Override
    public String getFullName() {
        return this.procedureName;
    }

    @Override
    public String getName() {
        return this.procedureName;
    }

    @Override
    public Collection<Method> getMethods() {
        final Collection<Method> methods = new HashSet<>(this.procedureType.getMethods());
        methods.add(this.invokeMethod);
        return methods;
    }

    @Override
    public Collection<Method> getLocalMethods() {
        final Collection<Method> methods = new HashSet<>(this.procedureType.getLocalMethods());
        methods.add(this.invokeMethod);
        return methods;
    }

    @Override
    public Collection<AbstractType> getParents() {
        return Set.of(this.procedureType);
    }

    @Override
    public Collection<Method> getSuperMethods(final String methodName) {
        return this.procedureType.getSuperMethods(methodName);
    }

    @Override
    public Collection<Method> getSuperMethods(final String methodName, final String superName) {
        return this.procedureType.getSuperMethods(methodName, superName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getFullName());
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

        final ProcedureInstance other = (ProcedureInstance) obj;
        return this == other;  // Test on identity.
    }

    @Override
    public String toString() {
        return String.format(
            "%s@%s(%s)",
            this.getClass().getName(), Integer.toHexString(this.hashCode()),
            this.getFullName());
    }

}
