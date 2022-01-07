package nl.ramsolutions.sw.magik.analysis.typing.types;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import nl.ramsolutions.sw.magik.analysis.Location;

/**
 * Multiple magik types.
 */
public class CombinedType extends AbstractType {

    private static final String VALUE_COMBINATOR = "|";

    private final Set<AbstractType> types;

    public CombinedType(final AbstractType... types) {
        this(Set.of(types));
    }

    public CombinedType(final Collection<AbstractType> types) {
        this.types = Collections.unmodifiableSet(Set.copyOf(types));
    }

    public Collection<AbstractType> getTypes() {
        return this.types;
    }

    @Override
    public String getFullName() {
        return this.types.stream()
            .map(AbstractType::getFullName)
            .sorted()
            .collect(Collectors.joining(VALUE_COMBINATOR));
    }

    @Override
    public String getName() {
        return this.types.stream()
            .map(AbstractType::getName)
            .sorted()
            .collect(Collectors.joining(VALUE_COMBINATOR));
    }

    /**
     * Combine two {{MagikType}}s.
     * @param type1 First {{MagikType}} to combine.
     * @param type2 Second {{MagikType}} to combine.
     * @return {{CombinedMagikType}} representing both types.
     */
    public static AbstractType combine(final AbstractType type1, final @Nullable AbstractType type2) {
        if (type2 == null) {
            return type1;
        }

        // Cases: a. 1 = SingleMagikType,   2 = SingleMagikType
        //        b. 1 = SingleMagikType,   2 = CombinedMagikType
        //        c. 1 = CombinedMagikType, 2 = SingleMagikType
        //        d. 1 = CombinedMagikType, 2 = CombinedMagikType
        final Set<AbstractType> types = new HashSet<>();

        // 1.
        if (type1 instanceof CombinedType) {
            final CombinedType combinedType = (CombinedType) type1;
            types.addAll(combinedType.getTypes());
        } else {
            types.add(type1);
        }

        // 2.
        if (type2 instanceof CombinedType) {
            final CombinedType combinedType = (CombinedType) type2;
            types.addAll(combinedType.getTypes());
        } else {
            types.add(type2);
        }

        if (types.size() == 1) {
            return types.stream()
                .findAny()
                .orElseThrow();
        }

        return new CombinedType(types);
    }

    @Override
    public Collection<Slot> getSlots() {
        return this.types.stream()
            .flatMap(type -> type.getSlots().stream())
            .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Collection<Method> getMethods() {
        return this.types.stream()
            .flatMap(type -> type.getMethods().stream())
            .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Collection<Method> getLocalMethods() {
        return this.types.stream()
            .flatMap(type -> type.getLocalMethods().stream())
            .collect(Collectors.toUnmodifiableSet());
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
        return Objects.equals(this.types.size(), other.types.size())
            && Objects.equals(this.getFullName(), other.getFullName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.types.toArray());
    }

    @Override
    public Collection<AbstractType> getParents() {
        return this.types.stream()
            .flatMap(type -> type.getParents().stream())
            .collect(Collectors.toSet());
    }

    @Override
    public Collection<Method> getSuperMethods(final String methodName) {
        return this.types.stream()
            .flatMap(type -> type.getSuperMethods(methodName).stream())
            .collect(Collectors.toSet());
    }

    @Override
    public Collection<Method> getSuperMethods(final String methodName, final String superName) {
        return this.types.stream()
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

}
