package nl.ramsolutions.sw.magik.analysis.typing;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.util.Collection;
import java.util.Set;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.Condition;
import nl.ramsolutions.sw.magik.analysis.typing.types.Package;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;

/**
 * Type keeper to hold types.
 */
public interface ITypeKeeper {

    /**
     * Add a new package.
     * If the package already exists, then:
     * - the types are copied.
     * - the location is updated.
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
    @CheckForNull
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
     * Test if the type reference can be resolved.
     * @param typeString Reference.
     * @return True if reference points to something.
     */
    boolean hasType(TypeString typeString);

    /**
     * Test if the type reference can be resolved in the given package.
     * No used packages are tested.
     * @param typeString Reference.
     * @return True if reference points to something in the package.
     */
    boolean hasTypeInPackage(TypeString typeString);

    /**
     * Add a global type, such as an exemplar.
     * @param type Type to add.
     */
    void addType(AbstractType type);

    /**
     * Get a global type, such as an exemplar.
     * @param typeString Reference.
     * @return Type, or UndefinedType if not found.
     */
    AbstractType getType(TypeString typeString);

    @CheckForNull
    AbstractType getTypeInPackage(TypeString typeString);

    /**
     * Remove a type. Searches all packages.
     * @param type Type to remove.
     */
    void removeType(AbstractType type);

    /**
     * Get all {@link AbstractType}s that we know of.
     * @return All {@link AbstractType}s.
     */
    Collection<AbstractType> getTypes();

    /**
     * Add a condition.
     * @param condition Condition to add.
     */
    void addCondition(Condition condition);

    /**
     * Get condition by name.
     * @param name Name of condition.
     * @return Condition, if found.
     */
    @CheckForNull
    Condition getCondition(String name);

    /**
     * Get all {@link Condition}s that we know of.
     * @return All {@link Condition}s.
     */
    Collection<Condition> getConditions();

    /**
     * Remove a condition.
     * @param condition Condition to remove.
     */
    void removeCondition(Condition condition);

    // region: Binary operators.
    /**
     * Add a binary operator.
     * @param binaryOperator Operator.
     */
    void addBinaryOperator(BinaryOperator binaryOperator);

    /**
     * Get the resulting {@link MagikType} for a binary operator.
     * @param operator Operator name.
     * @param leftType Left type.
     * @param rightType Right type.
     * @return Resulting type.
     */
    @CheckForNull
    BinaryOperator getBinaryOperator(BinaryOperator.Operator operator, TypeString leftType, TypeString rightType);

    /**
     * Remove a binary operator.
     * @param binaryOperator Operator.
     */
    void removeBinaryOperator(BinaryOperator binaryOperator);

    /**
     * Get all binary operators.
     * @return All binary operators.
     */
    Collection<BinaryOperator> getBinaryOperators();
    // endregion

    /**
     * Clear all packages/types/operators.
     */
    void clear();

}
