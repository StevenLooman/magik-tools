package nl.ramsolutions.sw.magik.analysis.typing;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.GlobalReference;
import nl.ramsolutions.sw.magik.analysis.typing.types.IndexedType;
import nl.ramsolutions.sw.magik.analysis.typing.types.IntrinsicType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ObjectType;
import nl.ramsolutions.sw.magik.analysis.typing.types.Package;
import nl.ramsolutions.sw.magik.analysis.typing.types.SlottedType;
import nl.ramsolutions.sw.magik.analysis.typing.types.UndefinedType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Type keeper.
 */
public class TypeKeeper implements ITypeKeeper {

    private static final Logger LOGGER = LoggerFactory.getLogger(TypeKeeper.class);

    private final Map<String, Package> packages = new HashMap<>();
    private final Set<UnaryOperator> unaryOperators = new HashSet<>();
    private final Set<BinaryOperator> binaryOperators = new HashSet<>();

    /**
     * Constructor.
     */
    public TypeKeeper() {
        this.clear();
    }

    @Override
    public void clear() {
        this.packages.clear();
        this.unaryOperators.clear();
        this.binaryOperators.clear();

        this.registerRequiredPackages();
        this.registerRequiredTypes();
    }

    /**
     * Register required packages (sw and user).
     */
    public void registerRequiredPackages() {
        final Package swPackage = new Package("sw");
        this.addPackage(swPackage);

        final Package userPackage = new Package("user");
        userPackage.addUse(swPackage);
        this.addPackage(userPackage);
    }

    private void registerRequiredTypes() {
        final Package swPakkage = this.getPackage("sw");

        swPakkage.put("object", new ObjectType(GlobalReference.of("sw:object")));
        swPakkage.put("unset", new IntrinsicType(GlobalReference.of("sw:unset")));
        swPakkage.put("false", new IntrinsicType(GlobalReference.of("sw:false")));
        swPakkage.put("maybe", new IntrinsicType(GlobalReference.of("sw:maybe")));
        swPakkage.put("integer", new IntrinsicType(GlobalReference.of("sw:integer")));
        swPakkage.put("bignum", new IntrinsicType(GlobalReference.of("sw:bignum")));
        swPakkage.put("float", new IntrinsicType(GlobalReference.of("sw:float")));
        swPakkage.put("symbol", new IndexedType(GlobalReference.of("sw:symbol")));
        swPakkage.put("character", new IntrinsicType(GlobalReference.of("sw:character")));
        swPakkage.put("sw_regexp", new IntrinsicType(GlobalReference.of("sw:sw_regexp")));
        swPakkage.put("char16_vector", new IndexedType(GlobalReference.of("sw:char16_vector")));
        swPakkage.put("simple_vector", new IndexedType(GlobalReference.of("sw:simple_vector")));
        swPakkage.put("heavy_thread", new IntrinsicType(GlobalReference.of("sw:heavy_thread")));
        swPakkage.put("light_thread", new IntrinsicType(GlobalReference.of("sw:light_thread")));
        swPakkage.put("condition", new SlottedType(GlobalReference.of("sw:condition")));
        swPakkage.put("enumeration_value", new SlottedType(GlobalReference.of("sw:enumeration_value")));
        swPakkage.put("indexed_format_mixin", new IntrinsicType(GlobalReference.of("sw:indexed_format_mixin")));
        swPakkage.put("slotted_format_mixin", new IntrinsicType(GlobalReference.of("sw:slotted_format_mixin")));
    }

    @Override
    public void addPackage(final Package pakkage) {
        this.packages.put(pakkage.getName(), pakkage);
    }

    public boolean hasPackage(final String pakkageName) {
        return this.packages.containsKey(pakkageName);
    }

    @Override
    public Package getPackage(final String pakkageName) {
        if (!this.packages.containsKey(pakkageName)) {
            LOGGER.debug("Undefined package: {}", pakkageName);
        }
        return this.packages.computeIfAbsent(pakkageName, Package::new);
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
    public boolean hasType(final GlobalReference globalReference) {
        final String pakkageName = globalReference.getPakkage();
        final Package pakkage = this.getPackage(pakkageName);
        final String identifier = globalReference.getIdentifier();
        return pakkage != null && pakkage.containsKey(identifier);
    }

    @Override
    public void addType(final AbstractType type) {
        final GlobalReference globalRef = GlobalReference.of(type.getFullName());
        final String pakkageName = globalRef.getPakkage();
        final Package pakkage = this.getPackage(pakkageName);
        final String identifier = globalRef.getIdentifier();
        pakkage.put(identifier, type);
    }

    @Override
    public AbstractType getType(final GlobalReference globalReference) {
        final String pakkageName = globalReference.getPakkage();
        final Package pakkage = this.getPackage(pakkageName);
        final String identifier = globalReference.getIdentifier();
        if (!pakkage.containsKey(identifier)) {
            return UndefinedType.INSTANCE;
        }

        return pakkage.get(identifier);
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

    // region: Operators
    // region: Unary operators
    @Override
    public void addUnaryOperator(final UnaryOperator unaryOperator) {
        this.unaryOperators.add(unaryOperator);
    }

    @Override
    public UnaryOperator getUnaryOperator(final UnaryOperator.Operator operator, final AbstractType type) {
        return this.unaryOperators.stream()
            .filter(unaryOperator ->
                unaryOperator.getOperator() == operator
                && unaryOperator.getType().equals(type))
            .findAny()
            .orElse(null);
    }

    @Override
    public void removeUnaryOperator(final UnaryOperator unaryOperator) {
        this.unaryOperators.remove(unaryOperator);
    }
    // endregion

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
    // endregion

}
