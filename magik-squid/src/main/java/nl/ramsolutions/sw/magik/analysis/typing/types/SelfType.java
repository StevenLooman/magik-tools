package nl.ramsolutions.sw.magik.analysis.typing.types;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import nl.ramsolutions.sw.magik.Location;

/**
 * Type to represent self, to be evaluated later when the real type is known.
 */
public final class SelfType extends AbstractType {  // NOSONAR: Singleton.

    /**
     * Instance of {@code _self}/{@code _clone} to be used in all cases.
     */
    public static final SelfType INSTANCE = new SelfType();

    /**
     * Serialized name of {@code SelfType}.
     */
    public static final String SERIALIZED_NAME = "_self";

    /**
     * Private constructor.
     */
    private SelfType() {
        super(null, null);
    }

    @Override
    public TypeString getTypeString() {
        return TypeString.SELF;
    }

    @Override
    public String getFullName() {
        return SelfType.SERIALIZED_NAME;
    }

    @Override
    public String getName() {
        return SelfType.SERIALIZED_NAME;
    }

    @Override
    public Collection<Slot> getSlots() {
        return Collections.emptySet();
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
