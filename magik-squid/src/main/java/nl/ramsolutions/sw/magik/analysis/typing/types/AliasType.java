package nl.ramsolutions.sw.magik.analysis.typing.types;

import java.util.Collection;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;

/**
 * Alias type, refering to another type.
 */
public class AliasType extends AbstractType {

    private final ITypeKeeper typeKeeper;
    private final TypeString typeString;
    private final TypeString aliasedTypeString;
    private final AbstractType aliasedType;

    /**
     * Constructor.
     * @param typeString Reference to type.
     * @param aliasedTypeString Aliased type.
     */
    public AliasType(
            final ITypeKeeper typeKeeper,
            final TypeString typeString,
            final TypeString aliasedTypeString) {
        this.typeKeeper = typeKeeper;
        this.typeString = typeString;
        this.aliasedTypeString = aliasedTypeString;
        this.aliasedType = null;
    }

    /**
     * Constructor.
     * @param typeKeeper Type keeper.
     * @param typeString Reference to type.
     * @param aliasedType Aliased type.
     */
    public AliasType(
            final ITypeKeeper typeKeeper,
            final TypeString typeString,
            final AbstractType aliasedType) {
        this.typeKeeper = typeKeeper;
        this.typeString = typeString;
        this.aliasedTypeString = null;
        this.aliasedType = aliasedType;

        this.typeKeeper.addType(this);
    }

    @Override
    public TypeString getTypeString() {
        return this.typeString;
    }

    /**
     * Get aliased type.
     * @return Aliased type.
     */
    public AbstractType getAliasedType() {
        if (this.aliasedTypeString != null) {
            return this.typeKeeper.getType(this.aliasedTypeString);
        }

        return this.aliasedType;
    }

    @Override
    public String getFullName() {
        return this.typeString.getFullString();
    }

    @Override
    public String getName() {
        return this.typeString.getString();
    }

    @Override
    public Collection<Method> getMethods() {
        return this.getAliasedType().getMethods();
    }

    @Override
    public Collection<Method> getLocalMethods() {
        return this.getAliasedType().getLocalMethods();
    }

    @Override
    public Collection<AbstractType> getParents() {
        return this.getAliasedType().getParents();
    }

    @Override
    public Collection<Method> getSuperMethods(String methodName) {
        return this.getAliasedType().getSuperMethods(methodName);
    }

    @Override
    public Collection<Method> getSuperMethods(String methodName, String superName) {
        return this.getAliasedType().getSuperMethods(methodName, superName);
    }

    @Override
    public Collection<Slot> getSlots() {
        return this.getAliasedType().getSlots();
    }

    @Override
    public String getDoc() {
        return this.getAliasedType().getDoc();
    }

    @Override
    public String toString() {
        return String.format(
            "%s@%s(%s, %s)",
            this.getClass().getName(), Integer.toHexString(this.hashCode()),
            this.getFullName(), this.getAliasedType());
    }

}
