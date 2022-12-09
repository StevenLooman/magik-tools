package nl.ramsolutions.sw.magik.analysis.typing.types;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * Reference to the type of a parameter.
 * TODO: Should this be an AbstractType? Probably otherwise it cannot be used with the reasoner etc.
 *       Maybe the hierarchy should change a bit, such as:
 *       - AbstractType
 *         - ConcreteType
 *           - MagikType
 *           - SelfType
 *           - ...
 *         - ParameterRefType
 *         - GenericedType
 *         - ...
 */
public class ParameterReferenceType extends AbstractType {

    private final String parameterName;

    public ParameterReferenceType(final String parameterName) {
        this.parameterName = parameterName;
    }

    public String getParameterName() {
        return this.parameterName;
    }

    @Override
    public TypeString getTypeString() {
        throw new IllegalStateException();
    }

    @Override
    public String getFullName() {
        return "_parameter(" + this.parameterName + ")";
    }

    @Override
    public String getName() {
        return "_parameter(" + this.parameterName + ")";
    }

    @Override
    public Collection<Method> getMethods() {
        return Collections.emptySet();
    }

    @Override
    public Collection<Method> getLocalMethods() {
        return Collections.emptySet();
    }

    @Override
    public Collection<AbstractType> getParents() {
        return Collections.emptySet();
    }

    @Override
    public Collection<Method> getSuperMethods(final String methodName) {
        return Collections.emptySet();
    }

    @Override
    public Collection<Method> getSuperMethods(final String methodName, final String superName) {
        return Collections.emptySet();
    }

    @Override
    public Collection<Slot> getSlots() {
        return Collections.emptySet();
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

        final ParameterReferenceType other = (ParameterReferenceType) obj;
        return Objects.equals(this.getParameterName(), other.getParameterName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.parameterName);
    }

    @Override
    public String toString() {
        return String.format(
            "%s@%s(%s)",
            this.getClass().getName(), Integer.toHexString(this.hashCode()),
            this.getParameterName());
    }

}
