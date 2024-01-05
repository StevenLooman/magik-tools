package nl.ramsolutions.sw.magik.analysis.typing.types;

import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;

/**
 * Alias type, refering to another type.
 */
public class AliasType extends AbstractType {

    private final ITypeKeeper typeKeeper;
    private final TypeString typeString;
    private final TypeString aliasedTypeRef;
    private final AbstractType aliasedType;

    /**
     * Constructor.
     * @param typeString Reference to type.
     * @param aliasedTypeString Aliased type.
     */
    public AliasType(
            final ITypeKeeper typeKeeper,
            final @Nullable Location location,
            final @Nullable String moduleName,
            final TypeString typeString,
            final TypeString aliasedTypeString) {
        super(location, moduleName);
        this.typeKeeper = typeKeeper;
        this.typeString = typeString;
        this.aliasedTypeRef = aliasedTypeString;
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
            final @Nullable Location location,
            final @Nullable String moduleName,
            final TypeString typeString,
            final AbstractType aliasedType) {
        super(location, moduleName);
        this.typeKeeper = typeKeeper;
        this.typeString = typeString;
        this.aliasedTypeRef = null;
        this.aliasedType = aliasedType;
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
        if (this.aliasedTypeRef != null) {
            return this.typeKeeper.getType(this.aliasedTypeRef);
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
    public Collection<Method> getSuperMethods(final String methodName) {
        return this.getAliasedType().getSuperMethods(methodName);
    }

    @Override
    public Collection<Method> getSuperMethods(final String methodName, final String superName) {
        return this.getAliasedType().getSuperMethods(methodName, superName);
    }

    @Override
    public List<GenericDefinition> getGenericDefinitions() {
        return this.getAliasedType().getGenericDefinitions();
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
            this.getFullName(), this.getAliasedType().getFullName());
    }

}
