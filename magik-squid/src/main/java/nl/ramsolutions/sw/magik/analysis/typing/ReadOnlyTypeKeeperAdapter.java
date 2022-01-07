package nl.ramsolutions.sw.magik.analysis.typing;

import java.util.Collection;
import java.util.Set;
import javax.annotation.CheckForNull;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.GlobalReference;
import nl.ramsolutions.sw.magik.analysis.typing.types.Package;

/**
 * Readonly wrapper for TypeKeeper.
 */
public class ReadOnlyTypeKeeperAdapter implements ITypeKeeper {

    private final ITypeKeeper typeKeeper;

    public ReadOnlyTypeKeeperAdapter(final ITypeKeeper typeKeeper) {
        this.typeKeeper = typeKeeper;
    }

    @Override
    public void addPackage(final Package pakkage) {
        // Do nothing.
    }

    @Override
    public boolean hasPackage(final String pakkageName) {
        return this.typeKeeper.hasPackage(pakkageName);
    }

    @Override
    public Package getPackage(final String pakkageName) {
        return this.typeKeeper.getPackage(pakkageName);
    }

    @Override
    public void removePackage(final Package pakkage) {
        // Do nothing.
    }

    @Override
    public Set<Package> getPackages() {
        return this.typeKeeper.getPackages();
    }

    @Override
    public boolean hasType(final GlobalReference globalReference) {
        return this.typeKeeper.hasType(globalReference);
    }

    @Override
    public void addType(final AbstractType type) {
        // Do nothing.
    }

    @Override
    public AbstractType getType(final GlobalReference globalReference) {
        return this.typeKeeper.getType(globalReference);
    }

    @Override
    public void removeType(final AbstractType type) {
        // Do nothing.
    }

    @Override
    public Collection<AbstractType> getTypes() {
        return this.typeKeeper.getTypes();
    }

    @CheckForNull
    @Override
    public void addUnaryOperator(final UnaryOperator unaryOperator) {
        // Do nothing.
    }

    @Override
    public UnaryOperator getUnaryOperator(final UnaryOperator.Operator operator, final AbstractType type) {
        return this.typeKeeper.getUnaryOperator(operator, type);
    }

    @Override
    public void removeUnaryOperator(final UnaryOperator unaryOperator) {
        // Do nothing.
    }

    @Override
    public void addBinaryOperator(final BinaryOperator binaryOperator) {
        // Do nothing.
    }

    @CheckForNull
    @Override
    public BinaryOperator getBinaryOperator(
            final BinaryOperator.Operator operator,
            final AbstractType leftType,
            final AbstractType rightType) {
        return this.typeKeeper.getBinaryOperator(operator, leftType, rightType);
    }

    @Override
    public void removeBinaryOperator(final BinaryOperator binaryOperator) {
        // Do nothing.
    }

    @Override
    public void clear() {
        // Do nothing.
    }

}
