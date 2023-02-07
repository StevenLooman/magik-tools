package nl.ramsolutions.sw.magik.analysis.typing.types;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import nl.ramsolutions.sw.magik.analysis.Location;

/**
 * Abstract magik type.
 */
public abstract class AbstractType {

    private Location location;
    private String doc;

    /**
     * Get the global reference to this type.
     * @return Global reference to this type.
     */
    public abstract TypeString getTypeString();

    /**
     * Get the full name of this type, including package.
     * @return Name of this type, including package.
     */
    public abstract String getFullName();  // TODO: Remove this, get via TypeString.

    /**
     * Get the name of this type.
     * @return Name of this type.
     */
    public abstract String getName();

    /**
     * Get all methods for this type responds to.
     *
     * <p>
     * I.e., if this type overrides a method from a super type,
     * the method from this type will be returned. The method from
     * the super type will be omitted.
     * </p>
     * @return Collection with all local and inherited methods.
     */
    public abstract Collection<Method> getMethods();

    /**
     * Get all {@link Method}s for this type responds to by name.
     * @param methodName Name of method(s).
     * @return Collection of methods for this type/these types with this name.
     */
    public Collection<Method> getMethods(final String methodName) {
        return this.getMethods().stream()
            .filter(method -> method.getName().equals(methodName))
            .collect(Collectors.toSet());
    }

    /**
     * Get all local methods for this type.
     * @return Collection with all local and methods.
     */
    public abstract Collection<Method> getLocalMethods();

    /**
     * Get all local methods for this type.
     * @param methodName Name of method(s).
     * @return Collection with all local and methods.
     */
    public Collection<Method> getLocalMethods(final String methodName) {
        return this.getLocalMethods().stream()
            .filter(method -> method.getName().equals(methodName))
            .collect(Collectors.toSet());
    }

    public boolean hasLocalMethod(final String methodName) {
        return !this.getLocalMethods(methodName).isEmpty();
    }

    /**
     * Get parent types.
     * @return Collection with parent types.
     */
    public abstract Collection<AbstractType> getParents();

    /**
     * Get all ancestor types.
     * @return Collection with parent and ancestor types.
     */
    public Collection<AbstractType> getAncestors() {
        return Stream.concat(
            this.getParents().stream(),
            this.getParents().stream()
                .flatMap(parentType -> parentType.getAncestors().stream()))
            .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Test if this type is kind of {@code otherType}.
     *
     * <p>
     * Note that this does not work when testing against a {@link CombinedType}.
     * </p>
     * @param otherType Type to test against.
     * @return True if kind of {@code otherType}, false otherwise.
     */
    public boolean isKindOf(final AbstractType otherType) {
        return this.equals(otherType) || this.getAncestors().contains(otherType);
    }

    /**
     * Get method from super type.
     * @param methodName Name of method.
     * @return Methods with methodName.
     */
    public abstract Collection<Method> getSuperMethods(String methodName);

    /**
     * Get method from specific super type.
     * @param methodName Name of method.
     * @return Methods with methodName.
     */
    public abstract Collection<Method> getSuperMethods(String methodName, String superName);

    /**
     * Get slots for this type.
     * @return Slots for this type.
     */
    public abstract Collection<Slot> getSlots();

    /**
     * Get slot by type.
     * @param name Name of slot.
     * @return Slot with name.
     */
    @CheckForNull
    public Slot getSlot(final String name) {
        return this.getSlots().stream()
            .filter(slot -> slot.getName().equals(name))
            .findFirst()
            .orElse(null);
    }

    /**
     * Get the {@link GenericDefinition}s.
     * @return List of {@link GenericDefinition}s.
     */
    public abstract List<GenericDeclaration> getGenerics();

    /**
     * Get the {@link GenericDefinition} by name.
     * @param name Name of GenericDefinition.
     * @return
     */
    @CheckForNull
    public GenericDeclaration getGeneric(final String name) {
        return this.getGenerics().stream()
            .filter(genericDefinition -> genericDefinition.getName().equals(name))
            .findAny()
            .orElse(null);
    }

    /**
     * Get {@link Location} for exemplar.
     * @return {@link Location} where exemplar is defined.
     */
    @CheckForNull
    public Location getLocation() {
        return this.location;
    }

    /**
     * Set {@link Location} for exemplar.
     * @param location Set {@link Location} where exemplar is defined.
     */
    public void setLocation(final Location location) {
        this.location = location;
    }

    /**
     * Set type documentation.
     * @param doc Type doc.
     */
    public void setDoc(final String doc) {
        this.doc = doc;
    }

    /**
     * Get type documentation.
     * @return Type doc.
     */
    @CheckForNull
    public String getDoc() {
        return this.doc;
    }

    /**
     * Substitute {@code from} type for {@code to} type, if {@code this} is equal to {@code from}.
     * @param from From type.
     * @param to To type.
     * @return To type or self.
     */
    public AbstractType substituteType(final AbstractType from, final AbstractType to) {
        if (from.equals(this)) {
            return to;
        }

        return this;
    }

}
