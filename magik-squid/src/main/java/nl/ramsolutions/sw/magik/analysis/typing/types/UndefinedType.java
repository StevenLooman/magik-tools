package nl.ramsolutions.sw.magik.analysis.typing.types;

import java.util.Collection;
import java.util.Collections;
import nl.ramsolutions.sw.magik.analysis.Location;

/**
 * Special type used when the {{TypeReasoner}} cannot determine type.
 */
public final class UndefinedType extends AbstractType {

    /**
     * Instance of {{UndefinedType}} to be used in all cases.
     */
    public static final UndefinedType INSTANCE = new UndefinedType();

    /**
     * Serialized name of {{UndefinedType}}.
     */
    public static final String SERIALIZED_NAME = "_undefined";

    /**
     * Private constructor.
     */
    private UndefinedType() {
    }

    @Override
    public String getFullName() {
        return SERIALIZED_NAME;
    }

    @Override
    public String getName() {
        return SERIALIZED_NAME;
    }

    @Override
    public Collection<Slot> getSlots() {
        return Collections.emptySet();
    }

    @Override
    public Slot getSlot(String name) {
        return null;
    }

    @Override
    public Collection<Method> getMethods(String methodName) {
        return Collections.emptySet();
    }

    @Override
    public Collection<Method> getMethods() {
        return Collections.emptySet();
    }

    @Override
    public boolean hasLocalMethod(String methodName) {
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
    public Collection<Method> getSuperMethods(String methodName) {
        return Collections.emptySet();
    }

    @Override
    public Collection<Method> getSuperMethods(String methodName, String superName) {
        return Collections.emptySet();
    }

    @Override
    public Location getLocation() {
        return null;
    }

    @Override
    public void setLocation(Location location) {
        throw new IllegalStateException();
    }

    @Override
    public String getDoc() {
        return null;
    }

    @Override
    public void setDoc(String comment) {
        throw new IllegalStateException();
    }

}
