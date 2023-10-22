package nl.ramsolutions.sw.magik.analysis.typing.types;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;

/**
 * Generic definition.
 */
public class GenericDefinition extends AbstractType {

    private final ITypeKeeper typeKeeper;
    private final String name;
    private final TypeString typeString;

    /**
     * Constructor.
     * @param typeKeeper TypeKeeper.
     * @param name Name of generic.
     * @param typeString Defined type of generic.
     */
    public GenericDefinition(
            final ITypeKeeper typeKeeper,
            final String name,
            final TypeString typeString) {
        super(null);
        this.typeKeeper = typeKeeper;
        this.name = name;
        this.typeString = typeString;
    }

    public String getName() {
        return this.name;
    }

    public TypeString getNameAsTypeString() {
        return TypeString.ofGeneric(this.name);
    }

    public TypeString getTypeString() {
        return this.typeString;
    }

    public AbstractType getType() {
        return this.typeKeeper.getType(this.typeString);
    }

    @Override
    public String getFullName() {
        return this.getType().getFullName();
    }

    @Override
    public Collection<Method> getMethods() {
        return this.getType().getMethods();
    }

    @Override
    public Collection<Method> getLocalMethods() {
        return this.getType().getLocalMethods();
    }

    @Override
    public Collection<AbstractType> getParents() {
        return this.getType().getParents();
    }

    @Override
    public Collection<Method> getSuperMethods(final String methodName) {
        return this.getType().getSuperMethods(methodName);
    }

    @Override
    public Collection<Method> getSuperMethods(final String methodName, final String superName) {
        return this.getType().getSuperMethods(methodName, superName);
    }

    @Override
    public Collection<Slot> getSlots() {
        return this.getType().getSlots();
    }

    @Override
    public List<GenericDeclaration> getGenerics() {
        return Collections.emptyList();
    }

}
