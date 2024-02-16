package nl.ramsolutions.sw.magik.analysis.typing.types;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.magik.Location;

/**
 * Special type used when the {@link TypeReasoner} cannot determine type.
 */
public final class UndefinedType extends AbstractType {  // NOSONAR: Singleton.

    /**
     * Instance of {@code UndefinedType} to be used in all cases.
     */
    public static final UndefinedType INSTANCE = new UndefinedType();

    /**
     * Serialized name of {@code UndefinedType}.
     */
    public static final String SERIALIZED_NAME = "_undefined";

    /**
     * Private constructor.
     */
    private UndefinedType() {
        super(null, null);
    }

    @Override
    public TypeString getTypeString() {
        return TypeString.UNDEFINED;
    }

    @Override
    public String getFullName() {
        return UndefinedType.SERIALIZED_NAME;
    }

    @Override
    public String getName() {
        return UndefinedType.SERIALIZED_NAME;
    }

    @Override
    public Collection<Slot> getSlots() {
        return Collections.emptySet();
    }

    @Override
    public Slot getSlot(final String name) {
        return null;
    }

    @Override
    public Collection<Method> getMethods(final String methodName) {
        return Collections.emptySet();
    }

    @Override
    public Collection<Method> getMethods() {
        return Collections.emptySet();
    }

    @Override
    public boolean hasLocalMethod(final String methodName) {
        return false;
    }

    @Override
    public Collection<Method> getLocalMethods() {
        return Collections.emptySet();
    }

    @Override
    public String toString() {
        return String.format(
            "%s@%s",
            this.getClass().getName(), Integer.toHexString(this.hashCode()));
    }

    @Override
    public Collection<AbstractType> getParents() {
        return Collections.emptyList();
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
    public Location getLocation() {
        return null;
    }

    @Override
    public void setLocation(final @Nullable Location location) {
        throw new IllegalStateException();
    }

    @Override
    public String getDoc() {
        return null;
    }

    @Override
    public void setDoc(final @Nullable String doc) {
        throw new IllegalStateException();
    }

    @Override
    public List<GenericDefinition> getGenericDefinitions() {
        return Collections.emptyList();
    }

}
