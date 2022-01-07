package nl.ramsolutions.sw.magik.analysis.typing;

import java.util.Collection;
import java.util.Set;
import javax.annotation.CheckForNull;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.GlobalReference;
import nl.ramsolutions.sw.magik.analysis.typing.types.Package;

/**
 * Type keeper to hold types.
 */
public interface ITypeKeeper {

    /**
     * Add a new package.
     * @param pakkage New package.
     */
    void addPackage(Package pakkage);

    /**
     * Test if we have a package with name.
     *
     * @param pakkageName Name of package.
     * @return True if package is registered, false if not.
     */
    boolean hasPackage(String pakkageName);

    /**
     * Get a package by its name.
     * @param pakkageName Name of package.
     * @return Package with name.
     */
    Package getPackage(String pakkageName);

    /**
     * Remove a package.
     * @param pakkage Package to remove.
     */
    void removePackage(Package pakkage);

    /**
     * Get all known packages.
     */
    Set<Package> getPackages();

    /**
     * Test if we have a type with name.
     * @param globalReference Reference.
     * @return True if reference points to something.
     */
    boolean hasType(GlobalReference globalReference);

    /**
     * Add a global type, such as an exemplar.
     * @param type Type to add.
     */
    void addType(AbstractType type);

    /**
     * Get a global type, such as an exemplar.
     * @param globalReference Reference.
     * @return Type, or UndefinedType if not found.
     */
    AbstractType getType(GlobalReference globalReference);

    /**
     * Remove a type. Searches all packages.
     * @param type Type to remove.
     */
    void removeType(AbstractType type);

    /**
     * Get all {{AbstractType}}s that we know of.
     * @return All {{AbstractTypes}}
     */
    Collection<AbstractType> getTypes();

    // region: Operators.
    // region: Unary operators.
    /**
     * Add a unary operator.
     * @param unaryOperator Unary operator.
     */
    void addUnaryOperator(UnaryOperator unaryOperator);

    /**
     * Get the resulting {{MagikTYpe}} for a unary operator.
     * @param operator Operator name.
     * @param type Type operator is applied to.
     */
    @CheckForNull
    UnaryOperator getUnaryOperator(UnaryOperator.Operator operator, AbstractType type);

    /**
     * Remove a unary operator.
     * @param unaryOperator Unary operator.
     */
    void removeUnaryOperator(UnaryOperator unaryOperator);
    // endregion

    // region: Binary operators.
    /**
     * Add a binary operator.
     * @param binaryOpeartor Operator.
     */
    void addBinaryOperator(BinaryOperator binaryOpeartor);

    /**
     * Get the resulting {{MagikType}} for a binary operator.
     * @param operator Operator name.
     * @param leftType Left type.
     * @param rightType Right type.
     * @return Resulting type.
     */
    @CheckForNull
    BinaryOperator getBinaryOperator(BinaryOperator.Operator operator, AbstractType leftType, AbstractType rightType);

    /**
     * Remove a binary operator.
     * @param binaryOperator Operator.
     */
    void removeBinaryOperator(BinaryOperator binaryOperator);
    // endregion
    // endregion

    /**
     * Clear all packages/types/operators.
     */
    void clear();

}
