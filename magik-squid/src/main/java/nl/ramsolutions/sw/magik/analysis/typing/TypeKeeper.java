package nl.ramsolutions.sw.magik.analysis.typing;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.Condition;
import nl.ramsolutions.sw.magik.analysis.typing.types.MagikType;
import nl.ramsolutions.sw.magik.analysis.typing.types.MagikType.Sort;
import nl.ramsolutions.sw.magik.analysis.typing.types.Package;
import nl.ramsolutions.sw.magik.analysis.typing.types.ProcedureInstance;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.types.UndefinedType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Type keeper.
 */
public class TypeKeeper implements ITypeKeeper {

    private static final Logger LOGGER = LoggerFactory.getLogger(TypeKeeper.class);

    private final Map<Package, Map<TypeString, AbstractType>> types = new ConcurrentHashMap<>();
    private final Set<BinaryOperator> binaryOperators = ConcurrentHashMap.newKeySet();
    private final Map<String, Condition> conditions = new ConcurrentHashMap<>();

    /**
     * Constructor.
     */
    public TypeKeeper() {
        this.clear();
    }

    @Override
    public void clear() {
        this.types.clear();
        this.binaryOperators.clear();

        this.registerBaseTypes();
    }

    private void registerBaseTypes() {
        this.addPackage(new Package(this, null, null, "sw"));
        this.addType(new MagikType(this, null, null, Sort.OBJECT, TypeString.ofIdentifier("object", "sw")));
        this.addType(new MagikType(this, null, null, Sort.INTRINSIC, TypeString.ofIdentifier("unset", "sw")));
        this.addType(new MagikType(this, null, null, Sort.INTRINSIC, TypeString.ofIdentifier("false", "sw")));
        this.addType(new MagikType(this, null, null, Sort.INTRINSIC, TypeString.ofIdentifier("maybe", "sw")));
        this.addType(new MagikType(this, null, null, Sort.INTRINSIC, TypeString.ofIdentifier("integer", "sw")));
        this.addType(new MagikType(this, null, null, Sort.INTRINSIC, TypeString.ofIdentifier("bignum", "sw")));
        this.addType(new MagikType(this, null, null, Sort.INTRINSIC, TypeString.ofIdentifier("float", "sw")));
        this.addType(new MagikType(this, null, null, Sort.INDEXED, TypeString.ofIdentifier("symbol", "sw")));
        this.addType(new MagikType(this, null, null, Sort.INTRINSIC, TypeString.ofIdentifier("character", "sw")));
        this.addType(new MagikType(this, null, null, Sort.INTRINSIC, TypeString.ofIdentifier("sw_regexp", "sw")));
        this.addType(new MagikType(this, null, null, Sort.INTRINSIC, TypeString.ofIdentifier("procedure", "sw")));
        this.addType(new MagikType(this, null, null, Sort.INDEXED, TypeString.ofIdentifier("char16_vector", "sw")));
        this.addType(new MagikType(this, null, null, Sort.INDEXED, TypeString.ofIdentifier("simple_vector", "sw")));
        this.addType(new MagikType(this, null, null, Sort.INTRINSIC, TypeString.ofIdentifier("heavy_thread", "sw")));
        this.addType(new MagikType(this, null, null, Sort.INTRINSIC, TypeString.ofIdentifier("light_thread", "sw")));
        this.addType(new MagikType(this, null, null, Sort.SLOTTED, TypeString.ofIdentifier("condition", "sw")));
        this.addType(new MagikType(this, null, null, Sort.SLOTTED, TypeString.ofIdentifier("enumeration_value", "sw")));
        this.addType(
            new MagikType(this, null, null, Sort.INTRINSIC, TypeString.ofIdentifier("indexed_format_mixin", "sw")));
        this.addType(
            new MagikType(this, null, null, Sort.INTRINSIC, TypeString.ofIdentifier("slotted_format_mixin", "sw")));

        final Package userPackage = new Package(this, null, null, "user");
        this.addPackage(userPackage);
        userPackage.addUse("sw");
    }

    @Override
    public void addPackage(final Package pakkage) {
        final String pakkageName = pakkage.getName();
        if (this.hasPackage(pakkageName)) {
            throw new IllegalStateException();
        }

        this.types.put(pakkage, new HashMap<>());
    }

    public boolean hasPackage(final String pakkageName) {
        return this.types.keySet().stream()
            .anyMatch(pakkage -> pakkage.getName().equals(pakkageName));
    }

    @Override
    public Package getPackage(final String pakkageName) {
        if (!this.hasPackage(pakkageName)) {
            LOGGER.debug("Undefined package: {}", pakkageName);
        }

        return this.types.keySet().stream()
            .filter(pkg -> pkg.getName().equals(pakkageName))
            .findAny()
            .orElse(null);
    }

    @Override
    public void removePackage(final Package pakkage) {
        this.types.remove(pakkage);
    }

    @Override
    public Set<Package> getPackages() {
        return Collections.unmodifiableSet(this.types.keySet());
    }

    @Override
    public boolean hasType(final TypeString typeString) {
        if (!typeString.isSingle()) {
            throw new IllegalStateException();
        }

        return this.getType(typeString) != UndefinedType.INSTANCE;
    }

    @Override
    public boolean hasTypeInPackage(final TypeString typeString) {
        if (!typeString.isSingle()) {
            throw new IllegalStateException();
        }

        final String pakkageName = typeString.getPakkage();
        final Package pakkage = this.getPackage(pakkageName);
        if (pakkage == null) {
            return false;
        }

        final Map<TypeString, AbstractType> pakkageTypes = this.types.get(pakkage);
        final TypeString bareTypeString = typeString.getWithoutGenerics();
        return pakkageTypes.containsKey(bareTypeString);
    }

    @Override
    public void addType(final AbstractType type) {
        if (type instanceof ProcedureInstance) {
            // A procedure instance is never added/globally referrable,
            // it is assigned to a global through a AliasType.
            return;
        }

        final TypeString typeString = type.getTypeString();
        final String pakkageName = typeString.getPakkage();
        if (!this.hasPackage(pakkageName)) {
            // Not allowed to add packages.
            throw new IllegalStateException("Unknown package: " + pakkageName);
        }

        final Package pakkage = this.getPackage(pakkageName);
        Objects.requireNonNull(pakkage);
        final Map<TypeString, AbstractType> pakkageTypes = this.types.get(pakkage);
        final TypeString bareTypeString = typeString.getWithoutGenerics();
        if (pakkageTypes.containsKey(bareTypeString)) {
            // Not allowed to overwrite types.
            throw new IllegalStateException("Type already defined: " + bareTypeString.getFullString());
        }

        pakkageTypes.put(bareTypeString, type);
    }

    @Override
    public AbstractType getType(final TypeString typeString) {
        if (!typeString.isSingle()) {
            throw new IllegalStateException();
        }

        final String pakkageName = typeString.getPakkage();
        final Package pakkage = this.getPackage(pakkageName);
        if (pakkage == null) {
            return UndefinedType.INSTANCE;
        }

        final Stack<Package> pakkages = new Stack<>();
        pakkages.push(pakkage);
        while (!pakkages.isEmpty()) {
            final Package pkg = pakkages.pop();
            final TypeString pkgTypeString = TypeString.ofIdentifier(typeString.getIdentifier(), pkg.getName());
            final Map<TypeString, AbstractType> pakkageTypes = this.types.get(pkg);
            if (pakkageTypes.containsKey(pkgTypeString)) {
                return pakkageTypes.get(pkgTypeString);
            }

            pkg.getUses().stream()
                .map(this::getPackage)
                .filter(Objects::nonNull)
                .forEach(pakkages::push);
        }

        return UndefinedType.INSTANCE;
    }

    /**
     * Get the type explicitly from the package.
     */
    public AbstractType getTypeInPackage(final TypeString typeString) {
        if (!typeString.isSingle()) {
            throw new IllegalStateException();
        }

        final String pakkageName = typeString.getPakkage();
        final Package pakkage = this.getPackage(pakkageName);
        if (pakkage == null) {
            return UndefinedType.INSTANCE;
        }

        final Map<TypeString, AbstractType> pakkageTypes = this.types.get(pakkage);
        final TypeString bareTypeString = typeString.getWithoutGenerics();
        return pakkageTypes.getOrDefault(bareTypeString, UndefinedType.INSTANCE);
    }

    @Override
    public void removeType(final AbstractType type) {
        final TypeString typeString = type.getTypeString();
        for (final Package pakkage : this.types.keySet()) {
            final Map<TypeString, AbstractType> pakkageTypes = this.types.get(pakkage);
            if (pakkageTypes.containsKey(typeString)) {
                pakkageTypes.remove(typeString);
            }
        }
    }

    @Override
    public Collection<AbstractType> getTypes() {
        return this.types.values().stream()
            .flatMap(pakkageTypes -> pakkageTypes.values().stream())
            .collect(Collectors.toUnmodifiableSet());
    }

    // region: Binary operators
    @Override
    public void addBinaryOperator(final BinaryOperator binaryOperator) {
        this.binaryOperators.add(binaryOperator);
    }

    @Override
    public BinaryOperator getBinaryOperator(
            final BinaryOperator.Operator operator,
            final TypeString leftType,
            final TypeString rightType) {
        return this.binaryOperators.stream()
            .filter(binaryOperator ->
                binaryOperator.getOperator() == operator
                && binaryOperator.getLeftType().equals(leftType)
                && binaryOperator.getRightType().equals(rightType))
            .findAny()
            .orElse(null);
    }

    @Override
    public void removeBinaryOperator(final BinaryOperator binaryOperator) {
        this.binaryOperators.remove(binaryOperator);
    }
    // endregion

    @Override
    public void addCondition(final Condition condition) {
        final String name = condition.getName();
        this.conditions.put(name, condition);
    }

    @Override
    public Condition getCondition(final String name) {
        return this.conditions.get(name);
    }

    public Collection<Condition> getConditions() {
        return Collections.unmodifiableCollection(this.conditions.values());
    }

    @Override
    public void removeCondition(final Condition condition) {
        final String name = condition.getName();
        this.conditions.remove(name);
    }

    @Override
    public Collection<BinaryOperator> getBinaryOperators() {
        return Collections.unmodifiableCollection(this.binaryOperators);
    }

}
