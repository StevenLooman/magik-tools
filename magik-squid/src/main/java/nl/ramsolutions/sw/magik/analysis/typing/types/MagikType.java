package nl.ramsolutions.sw.magik.analysis.typing.types;

import java.util.Collection;
import java.util.Collections;
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
import java.util.stream.Stream;
import javax.annotation.Nullable;
import nl.ramsolutions.sw.magik.Location;
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

    private static final TypeString DEFAULT_PARENT_INDEXED_EXEMPLAR =
        TypeString.ofIdentifier("indexed_format_mixin", "sw");
    private static final TypeString DEFAULT_PARENT_SLOTTED_EXEMPLAR =
        TypeString.ofIdentifier("slotted_format_mixin", "sw");

    private final TypeString typeString;
    private final Set<Method> methods = ConcurrentHashMap.newKeySet();
    private final Set<TypeString> parents = ConcurrentHashMap.newKeySet();
    private final Map<String, Slot> slots = new ConcurrentHashMap<>();
    private final Queue<GenericDeclaration> generics = new ConcurrentLinkedQueue<>();
    private final ITypeKeeper typeKeeper;
    private Sort sort;

    /**
     * Constructor.
     * @param typeKeeper TypeKeeper.
     * @param moduleName Module name.
     * @param sort Sort.
     * @param typeString Global reference.
     */
    public MagikType(
            final ITypeKeeper typeKeeper,
            final @Nullable Location location,
            final @Nullable String moduleName,
            final Sort sort,
            final TypeString typeString) {
        super(location, moduleName);
        this.typeKeeper = typeKeeper;
        this.sort = sort;
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

    /**
     * Get parent {@link TypeString}s.
     * @return Parents.
     */
    public Set<TypeString> getParentsTypeRefs() {
        final Set<TypeString> defaultParents;
        if (this.getSort() == MagikType.Sort.INDEXED) {
            defaultParents = Set.of(DEFAULT_PARENT_INDEXED_EXEMPLAR);
        } else if (this.getSort() == MagikType.Sort.SLOTTED) {
            defaultParents = Set.of(DEFAULT_PARENT_SLOTTED_EXEMPLAR);
        } else {
            defaultParents = Collections.emptySet();
        }

        if (this.getSort() == MagikType.Sort.INDEXED
            || this.getSort() == MagikType.Sort.SLOTTED) {
            // Ensure correct parents. As we only know the actual parents during this evaluation,
            // we need to do this at runtime.
            final boolean parentNonIntrinsicType = this.parents.stream()
                .map(parentTypeRef -> this.typeKeeper.getType(parentTypeRef))
                .filter(MagikType.class::isInstance)
                .map(MagikType.class::cast)
                .anyMatch(parentType ->
                    parentType.getSort() == Sort.SLOTTED
                    || parentType.getSort() == Sort.INDEXED);
            if (!parentNonIntrinsicType) {
                return Stream.concat(
                    this.parents.stream(),
                    defaultParents.stream())
                    .collect(Collectors.toSet());
            }
        }

        return Collections.unmodifiableSet(this.parents);
    }

    @Override
    public Collection<AbstractType> getParents() {
        return this.getParentsTypeRefs().stream()
            .map(this.typeKeeper::getType)
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
     * @param moduleName Module name.
     * @param location Location of method.
     * @param methodName Name of method.
     * @param parameters Parameters for method.
     * @param assignmentParameter Assignment parameter for method.
     * @param methodDoc Method doc.
     * @param callResult {@link MagikType}s the method returns.
     * @param loopbodyResult {@link MagikType}s the method iterates.
     */
    @SuppressWarnings({"java:S1319", "checkstyle:ParameterNumber"})
    public Method addMethod(// NOSONAR
            final @Nullable Location location,
            final @Nullable String moduleName,
            final Set<Method.Modifier> modifiers,
            final String methodName,
            final List<Parameter> parameters,
            final @Nullable Parameter assignmentParameter,
            final @Nullable String methodDoc,
            final ExpressionResultString callResult,
            final ExpressionResultString loopbodyResult) {
        final Method method = new Method(
            location,
            moduleName,
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
            .flatMap(Collection::stream)
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
