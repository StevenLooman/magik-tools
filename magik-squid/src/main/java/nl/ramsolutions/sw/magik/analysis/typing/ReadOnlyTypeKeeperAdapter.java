package nl.ramsolutions.sw.magik.analysis.typing;

import java.util.Collection;
import java.util.Set;
import javax.annotation.CheckForNull;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.Condition;
import nl.ramsolutions.sw.magik.analysis.typing.types.Package;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;

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
    public boolean hasType(final TypeString typeString) {
        return this.typeKeeper.hasType(typeString);
    }

    @Override
    public void addType(final AbstractType type) {
        // Do nothing.
    }

    @Override
    public AbstractType getType(final TypeString typeString) {
        return this.typeKeeper.getType(typeString);
    }

    @Override
    public void removeType(final AbstractType type) {
        // Do nothing.
    }

    @Override
    public Collection<AbstractType> getTypes() {
        return this.typeKeeper.getTypes();
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

    @Override
    public void addCondition(final Condition condition) {
        // Do nothing.
    }

    @Override
    public Condition getCondition(final String name) {
        return this.typeKeeper.getCondition(name);
    }

    public Collection<Condition> getConditions() {
        return this.typeKeeper.getConditions();
    }

    @Override
    public void removeCondition(final Condition condition) {
        // Do nothing.
    }

    @Override
    public Collection<BinaryOperator> getBinaryOperators() {
        return this.typeKeeper.getBinaryOperators();
    }

}
