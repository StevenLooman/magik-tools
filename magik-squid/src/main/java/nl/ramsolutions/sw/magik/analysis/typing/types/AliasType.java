package nl.ramsolutions.sw.magik.analysis.typing.types;

import java.util.Collection;

/**
 * Alias type, refering to another type.
 */
public class AliasType extends AbstractType {

    private final GlobalReference globalReference;
    private final AbstractType aliasedType;

    /**
     * Constructor.
     * @param globalReference Reference to type.
     * @param aliasedType Aliased type.
     */
    public AliasType(final GlobalReference globalReference, final AbstractType aliasedType) {
        this.globalReference = globalReference;
        this.aliasedType = aliasedType;
    }

    public AbstractType getAliasedType() {
        return this.aliasedType;
    }

    @Override
    public String getFullName() {
        return this.globalReference.getFullName();
    }

    @Override
    public String getName() {
        return this.globalReference.getIdentifier();
    }

    @Override
    public Collection<Method> getMethods() {
        return this.aliasedType.getMethods();
    }

    @Override
    public Collection<Method> getLocalMethods() {
        return this.aliasedType.getLocalMethods();
    }

    @Override
    public Collection<AbstractType> getParents() {
        return this.aliasedType.getParents();
    }

    @Override
    public Collection<Method> getSuperMethods(String methodName) {
        return this.aliasedType.getSuperMethods(methodName);
    }

    @Override
    public Collection<Method> getSuperMethods(String methodName, String superName) {
        return this.aliasedType.getSuperMethods(methodName, superName);
    }

    @Override
    public Collection<Slot> getSlots() {
        return this.aliasedType.getSlots();
    }

    @Override
    public String toString() {
        return String.format(
            "%s@%s(%s, %s)",
            this.getClass().getName(), Integer.toHexString(this.hashCode()),
            this.getFullName(), this.getAliasedType());
    }

}
