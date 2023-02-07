package nl.ramsolutions.sw.magik.analysis.typing.types;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import nl.ramsolutions.sw.magik.analysis.Location;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;

/**
 * Magik type: slotted exemplar, indexed exemplar, enumeration, or mixin.
 */
public class MagikType extends AbstractType {

    /**
     * Sort of MagikType.
     */
    public enum Sort {

        /**
         * Type has not been seen yet, but, e.g., referred to by a method definition.
         */
        UNDEFINED,

        /**
         * {@code object} type.
         */
        OBJECT,

        /**
         * Slotted exemplar type.
         */
        SLOTTED,

        /**
         * Indexed exemplar type.
         */
        INDEXED,

        /**
         * Intrinsic type.
         */
        INTRINSIC;

    }

    private final TypeString typeString;
    private final Set<Method> methods = ConcurrentHashMap.newKeySet();
    private final Set<TypeString> parents = ConcurrentHashMap.newKeySet();
    private final Map<String, Slot> slots = new ConcurrentHashMap<>();
    private final Queue<GenericDeclaration> generics = new ConcurrentLinkedQueue<>();
    private Sort sort;
    private ITypeKeeper typeKeeper;

    /**
     * Constructor.
     * @param typeKeeper TypeKeeper.
     * @param magikTypeType Sort.
     * @param typeString Global reference.
     */
    public MagikType(
            final ITypeKeeper typeKeeper,
            final Sort magikTypeType,
            final TypeString typeString) {
        this.typeKeeper = typeKeeper;
        this.sort = magikTypeType;
        this.typeString = typeString;

        // Add self to TypeKeeper.
        this.typeKeeper.addType(this);
    }

    /**
     * Get the magik type.
     * @return
     */
    public Sort getSort() {
        return this.sort;
    }

    /**
     * Set the magik type.
     */
    public void setSort(final Sort sort) {
        this.sort = sort;
    }

    /**
     * Clear parents.
     */
    public void clearParents() {
        this.parents.clear();
    }

    /**
     * Add a parent type.
     * @param parentTypeString Reference to parent type.
     */
    @SuppressWarnings("java:S2583")
    public void addParent(final TypeString parentTypeString) {
        this.parents.add(parentTypeString);
    }

    @Override
    public Collection<AbstractType> getParents() {
        return this.parents.stream()
            .map(parentRef -> this.typeKeeper.getType(parentRef))
            .collect(Collectors.toList());
    }

    @Override
    public TypeString getTypeString() {
        return this.typeString;
    }

    @Override
    public String getFullName() {
        return this.typeString.getFullString();
    }

    @Override
    public String getName() {
        return this.typeString.getString();
    }

    /**
     * Add a {@link GenericDefinition}.
     * @param location Location GenericDefinition.
     * @param name Name of GenericDefinition.
     * @return
     */
    public GenericDeclaration addGeneric(final @Nullable Location location, final String name) {
        // TODO: Parameter `name` should be a TypeString?
        // TODO: Don't do this on the fly, but only at definition...
        final GenericDeclaration declaration = new GenericDeclaration(location, name);
        this.generics.add(declaration);
        return declaration;
    }

    @Override
    public List<GenericDeclaration> getGenerics() {
        return List.copyOf(this.generics);
    }

    /**
     * Add a slot with a given name and type.
     * @param location Location of slot.
     * @param name Name of slot.
     * @param slotTypeString Type of slot.
     * @return Added slot.
     */
    public Slot addSlot(final @Nullable Location location, final String name, final TypeString slotTypeString) {
        // TODO: Don't do this on the fly, but only at definition...
        final Slot slot = new Slot(location, name, slotTypeString);
        this.slots.put(name, slot);
        return slot;
    }

    /**
     * Get a slot by name.
     * @param name Name of slot
     * @return Type of slot.
     */
    @Override
    public Slot getSlot(final String name) {
        if (this.slots.containsKey(name)) {
            return this.slots.get(name);
        }

        for (final AbstractType parent : this.getParents()) {
            final Slot slot = parent.getSlot(name);
            if (slot != null) {
                return slot;
            }
        }

        return null;
    }

    /**
     * Get all slots from this type, including sub-types.
     * @return Slots.
     */
    @Override
    public Collection<Slot> getSlots() {
        final Collection<Slot> allSlots = new HashSet<>();

        allSlots.addAll(this.slots.values());
        for (final AbstractType parentType : this.getParents()) {
            allSlots.addAll(parentType.getSlots());
        }

        return allSlots;
    }

    public Collection<Slot> getLocalSlots() {
        return Collections.unmodifiableCollection(this.slots.values());
    }

    /**
     * Clear current slots, for this type.
     */
    public void clearSlots() {
        this.slots.clear();
    }

    /**
     * Add the resulting types of a method and loopbody, overwrites existing methods.
     * @param location Location of method.
     * @param methodName Name of method.
     * @param parameters Parameters for method.
     * @param assignmentParameter Assignment parameter for method.
     * @param methodDoc Method doc.
     * @param callResult {@link MagikType}s the method returns.
     * @param loopbodyResult {@link MagikType}s the method iterates.
     */
    @SuppressWarnings({"java:S1319", "checkstyle:ParameterNumber"})
    public Method addMethod(
            final @Nullable Location location,
            final EnumSet<Method.Modifier> modifiers,
            final String methodName,
            final List<Parameter> parameters,
            final @Nullable Parameter assignmentParameter,
            final @Nullable String methodDoc,
            final ExpressionResultString callResult,
            final ExpressionResultString loopbodyResult) {
        final Method method = new Method(
            location,
            modifiers,
            this,
            methodName,
            parameters,
            assignmentParameter,
            methodDoc,
            callResult,
            loopbodyResult);
        this.methods.add(method);
        return method;
    }

    /**
     * Get all methods this type has, including sub-types.
     * @return Methods for this type.
     */
    @Override
    public Collection<Method> getMethods() {
        final Map<String, Set<Method>> allMethods = new HashMap<>();

        // Add local methods.
        this.methods.forEach(method -> {
            final String methodName = method.getName();
            final Set<Method> methodsForName = allMethods.computeIfAbsent(methodName, key -> new HashSet<>());
            methodsForName.add(method);
        });

        // Add methods from parent types, if not overridden.
        for (final AbstractType parent : this.getParents()) {
            parent.getMethods().forEach(method -> {
                final String methodName = method.getName();
                if (allMethods.containsKey(methodName)) {
                    // This type already responds to the method, no need to add method from parent.
                    return;
                }

                final Set<Method> methodsForName = new HashSet<>();
                allMethods.put(methodName, methodsForName);

                methodsForName.add(method);
            });
        }

        return allMethods.values().stream()
            .flatMap(typeMethods -> typeMethods.stream())
            .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Collection<Method> getLocalMethods() {
        return Collections.unmodifiableSet(this.methods);
    }

    /**
     * Get any super method.
     * @param methodName Name of method.
     * @return Method return types.
     */
    @Override
    public Collection<Method> getSuperMethods(final String methodName) {
        // Try to get the wanted method from all super types, first one wins.
        return this.getParents().stream()
            .flatMap(parent -> parent.getMethods(methodName).stream())
            .collect(Collectors.toSet());
    }

    /**
     * Get any super method from a specific parent.
     * @param methodName Name of method.
     * @return Method return types.
     */
    @Override
    public Collection<Method> getSuperMethods(final String methodName, final String superName) {
        // If super-type was specified, specifically search that one.
        final Optional<AbstractType> superType = this.getParents().stream()
            .filter(type -> type.getFullName().equals(superName))
            .findAny();
        if (!superType.isPresent()) {
            return Collections.emptySet();
        }

        return superType.get().getMethods(methodName);
    }

    /**
     * Remove a method.
     * @param method Method to remove.
     */
    public void removeMethod(final Method method) {
        this.methods.remove(method);
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
        return Objects.hash(this.getFullName());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (this.getClass() != obj.getClass()) {
            return false;
        }

        final MagikType other = (MagikType) obj;
        return Objects.equals(this.getTypeString(), other.getTypeString());
    }

}
