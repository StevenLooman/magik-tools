package nl.ramsolutions.sw.magik.analysis.typing.types;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.Location;

/**
 * Multiple magik types.
 */
public class CombinedType extends AbstractType {

    private static final String VALUE_COMBINATOR = "|";

    private final Set<AbstractType> types;

    /**
     * Constructor.
     * @param types Combined types.
     */
    public CombinedType(final AbstractType... types) {
        this(Arrays.asList(types));
    }

    /**
     * Constructor.
     * @param types Combined types.
     */
    public CombinedType(final Collection<AbstractType> types) {
        super(null, null);
        this.types = types.stream()
            .flatMap(type ->
                type instanceof CombinedType
                ? ((CombinedType) type).getTypes().stream()
                : Stream.of(type))
            .collect(Collectors.toUnmodifiableSet());
    }

    public Collection<AbstractType> getTypes() {
        return this.types;
    }

    @Override
    public String getFullName() {
        return this.getTypes().stream()
            .map(AbstractType::getFullName)
            .sorted()
            .collect(Collectors.joining(VALUE_COMBINATOR));
    }

    @Override
    public String getName() {
        return this.getTypes().stream()
            .map(AbstractType::getName)
            .sorted()
            .collect(Collectors.joining(VALUE_COMBINATOR));
    }

    /**
     * Combine {@link AbstractType}s.
     * @param types Types {@link AbstractType} to combine.
     * @return {@link CombinedType} representing both types, or {@link AbstractType} if singular.
     */
    public static AbstractType combine(final AbstractType... types) {
        if (types.length == 0) {
            return null;
        }

        final Set<AbstractType> combinedTypes = Stream.of(types)
            .flatMap(type -> {
                if (type instanceof CombinedType) {
                    final CombinedType combinedType = (CombinedType) type;
                    return combinedType.getTypes().stream();
                }

                return Stream.of(type);
            })
            .collect(Collectors.toUnmodifiableSet());
        if (combinedTypes.size() == 1) {
            return combinedTypes.stream()
                .findFirst()
                .orElseThrow();
        }

        return new CombinedType(combinedTypes);
    }

    @Override
    public List<GenericDefinition> getGenericDefinitions() {
        return this.getTypes().stream()
            .flatMap(type -> type.getGenericDefinitions().stream())
            .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public Collection<Slot> getSlots() {
        return this.getTypes().stream()
            .flatMap(type -> type.getSlots().stream())
            .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Collection<Method> getMethods() {
        return this.getTypes().stream()
            .flatMap(type -> type.getMethods().stream())
            .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Collection<Method> getLocalMethods() {
        return this.getTypes().stream()
            .flatMap(type -> type.getLocalMethods().stream())
            .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Collection<AbstractType> getParents() {
        return this.getTypes().stream()
            .flatMap(type -> type.getParents().stream())
            .collect(Collectors.toSet());
    }

    @Override
    public Collection<Method> getSuperMethods(final String methodName) {
        return this.getTypes().stream()
            .flatMap(type -> type.getSuperMethods(methodName).stream())
            .collect(Collectors.toSet());
    }

    @Override
    public Collection<Method> getSuperMethods(final String methodName, final String superName) {
        return this.getTypes().stream()
            .filter(type -> type.getFullName().equals(superName))
            .flatMap(type -> type.getMethods(methodName).stream())
            .collect(Collectors.toSet());
    }

    @Override
    public Location getLocation() {
        return null;
    }

    @Override
    public void setLocation(final Location location) {
        throw new IllegalStateException();
    }

    @Override
    public String getDoc() {
        return null;
    }

    @Override
    public void setDoc(final String comment) {
        throw new IllegalStateException();
    }

    @Override
    public TypeString getTypeString() {
        final TypeString[] typeStringsArr = this.types.stream()
            .map(AbstractType::getTypeString)
            .collect(Collectors.toList())
            .toArray(TypeString[]::new);
        return TypeString.ofCombination(TypeString.DEFAULT_PACKAGE, typeStringsArr);
    }

    @Override
    public AbstractType substituteType(final AbstractType from, final AbstractType to) {
        final Set<AbstractType> substitutedTypes = this.types.stream()
            .map(type -> type.substituteType(from, to))
            .collect(Collectors.toUnmodifiableSet());
        if (substitutedTypes.equals(this.types)) {
            return this;
        }

        return new CombinedType(substitutedTypes);
    }

    @Override
    public String toString() {
        return String.format(
            "%s@%s(%s)",
            this.getClass().getName(), Integer.toHexString(this.hashCode()),
            this.getFullName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.types.toArray());
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (this.getClass() != obj.getClass()) {
            return false;
        }

        final CombinedType other = (CombinedType) obj;
        return Objects.equals(this.getFullName(), other.getFullName());
    }

}
