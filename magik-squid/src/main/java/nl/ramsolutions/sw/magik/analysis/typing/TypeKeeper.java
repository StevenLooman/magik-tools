package nl.ramsolutions.sw.magik.analysis.typing;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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

    private final Map<String, Package> packages = new HashMap<>();
    private final Set<BinaryOperator> binaryOperators = new HashSet<>();
    private final Map<String, Condition> conditions = new HashMap<>();

    /**
     * Constructor.
     */
    public TypeKeeper() {
        this.clear();
    }

    @Override
    public void clear() {
        this.packages.clear();
        this.binaryOperators.clear();

        this.registerRequiredPackages();
        this.registerRequiredTypes();
    }

    /**
     * Register required packages (sw and user).
     */
    public void registerRequiredPackages() {
        new Package(this, "sw");

        final Package userPackage = new Package(this, "user");
        userPackage.addUse("sw");
    }

    private void registerRequiredTypes() {
        final Package swPakkage = this.getPackage("sw");

        swPakkage.put("object", new MagikType(this, Sort.OBJECT, TypeString.of("sw:object")));
        swPakkage.put("unset", new MagikType(this, Sort.INTRINSIC, TypeString.of("sw:unset")));
        swPakkage.put("false", new MagikType(this, Sort.INTRINSIC, TypeString.of("sw:false")));
        swPakkage.put("maybe", new MagikType(this, Sort.INTRINSIC, TypeString.of("sw:maybe")));
        swPakkage.put("integer", new MagikType(this, Sort.INTRINSIC, TypeString.of("sw:integer")));
        swPakkage.put("bignum", new MagikType(this, Sort.INTRINSIC, TypeString.of("sw:bignum")));
        swPakkage.put("float", new MagikType(this, Sort.INTRINSIC, TypeString.of("sw:float")));
        swPakkage.put("symbol", new MagikType(this, Sort.INDEXED, TypeString.of("sw:symbol")));
        swPakkage.put("character", new MagikType(this, Sort.INTRINSIC, TypeString.of("sw:character")));
        swPakkage.put("sw_regexp", new MagikType(this, Sort.INTRINSIC, TypeString.of("sw:sw_regexp")));
        swPakkage.put("procedure", new MagikType(this, Sort.INTRINSIC, TypeString.of("sw:procedure")));
        swPakkage.put(
            "char16_vector", new MagikType(this, Sort.INDEXED, TypeString.of("sw:char16_vector")));
        swPakkage.put(
            "simple_vector", new MagikType(this, Sort.INDEXED, TypeString.of("sw:simple_vector")));
        swPakkage.put(
            "heavy_thread",
            new MagikType(this, Sort.INTRINSIC, TypeString.of("sw:heavy_thread")));
        swPakkage.put(
            "light_thread",
            new MagikType(this, Sort.INTRINSIC, TypeString.of("sw:light_thread")));
        swPakkage.put("condition", new MagikType(this, Sort.SLOTTED, TypeString.of("sw:condition")));
        swPakkage.put(
            "enumeration_value",
            new MagikType(this, Sort.SLOTTED, TypeString.of("sw:enumeration_value")));
        swPakkage.put(
            "indexed_format_mixin",
            new MagikType(this, Sort.INTRINSIC, TypeString.of("sw:indexed_format_mixin")));
        swPakkage.put(
            "slotted_format_mixin",
            new MagikType(this, Sort.INTRINSIC, TypeString.of("sw:slotted_format_mixin")));
    }

    @Override
    public void addPackage(final Package pakkage) {
        final String pakkageName = pakkage.getName();
        if (this.hasPackage(pakkageName)) {
            throw new IllegalStateException();
        }

        this.packages.put(pakkageName, pakkage);
    }

    public boolean hasPackage(final String pakkageName) {
        return this.packages.containsKey(pakkageName);
    }

    @Override
    public Package getPackage(final String pakkageName) {
        if (!this.packages.containsKey(pakkageName)) {
            LOGGER.debug("Undefined package: {}", pakkageName);
        }

        return this.packages.get(pakkageName);
    }

    @Override
    public void removePackage(final Package pakkage) {
        final String pakkageName = pakkage.getName();
        this.packages.remove(pakkageName);
    }

    @Override
    public Set<Package> getPackages() {
        return this.packages.values().stream()
            .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public boolean hasType(final TypeString typeString) {
        if (!typeString.isSingle()) {
            throw new IllegalStateException();
        }

        final String pakkageName = typeString.getPakkage();
        final Package pakkage = this.getPackage(pakkageName);
        final String identifier = typeString.getIdentifier();
        return pakkage != null && pakkage.containsKey(identifier);
    }

    @Override
    public void addType(final AbstractType type) {
        if (type instanceof ProcedureInstance) {
            // A procedure instance is never added/globally referrable,
            // unless it is assigned to a global through a AliasType.
            return;
        }

        final TypeString typeString = type.getTypeString();
        final String pakkageName = typeString.getPakkage();
        if (!this.hasPackage(pakkageName)) {
            // Not allowed to add packages.
            throw new IllegalStateException("Unknown package: " + pakkageName);
        }

        final Package pakkage = this.getPackage(pakkageName);
        final String identifier = typeString.getIdentifier();
        if (pakkage.containsKey(identifier)) {
            // Not allowed to overwrite types.
            throw new IllegalStateException("Type already defined: " + typeString.getFullString());
        }

        pakkage.put(identifier, type);
    }

    @Override
    public AbstractType getType(final TypeString typeString) {
        final String pakkageName = typeString.getPakkage();
        if (!this.hasPackage(pakkageName)) {
            return UndefinedType.INSTANCE;
        }

        if (!typeString.isSingle()) {
            throw new IllegalStateException();
        }

        final Package pakkage = this.getPackage(pakkageName);
        final String identifier = typeString.getIdentifier();
        final AbstractType type = pakkage.get(identifier);
        if (type == null) {
            return UndefinedType.INSTANCE;
        }

        return type;
    }

    @Override
    public void removeType(final AbstractType type) {
        for (final Package pakkage : this.packages.values()) {
            if (pakkage.containsTypeLocal(type)) {
                pakkage.remove(type);
            }
        }
    }

    @Override
    public Collection<AbstractType> getTypes() {
        return this.packages.values().stream()
            .flatMap(pakkage -> pakkage.getTypes().entrySet().stream())
            .map(Entry::getValue)
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
            final AbstractType leftType,
            final AbstractType rightType) {
        return this.binaryOperators.stream()
            .filter(binaryOperator ->
                binaryOperator.getOperator() == operator
                && binaryOperator.getLeftType().equals(leftType.getTypeString())
                && binaryOperator.getRightType().equals(rightType.getTypeString()))
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

}
