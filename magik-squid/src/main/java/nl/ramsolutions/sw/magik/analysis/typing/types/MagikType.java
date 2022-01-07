package nl.ramsolutions.sw.magik.analysis.typing.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import nl.ramsolutions.sw.magik.analysis.Location;

/**
 * Magik type: slotted exemplar, indexed exemplar, enumeration, or mixin.
 */
public abstract class MagikType extends AbstractType {

    private final GlobalReference globalReference;
    private final Set<Method> methods;
    private final List<AbstractType> parents;
    private final Map<String, Slot> slots;

    /**
     * Constructor.
     * @param globalReference Global reference.
     */
    protected MagikType(final GlobalReference globalReference) {
        this.globalReference = globalReference;

        this.methods = new HashSet<>();
        this.parents = new ArrayList<>();
        this.slots = new HashMap<>();
    }

    /**
     * Clear parents.
     */
    public void clearParents() {
        this.parents.clear();
    }

    /**
     * Add a parent type.
     * @param parentType Type to inherit.
     */
    @SuppressWarnings("java:S2583")
    public void addParent(final AbstractType parentType) {
        if (parentType == SelfType.INSTANCE
            || parentType == UndefinedType.INSTANCE) {
            final String message = "Trying to inherit invalid type: " + parentType;
            throw new IllegalArgumentException(message);
        } else if (parentType == this) {
            final String message = "Type: " + parentType + " cannot be inherited by ourselves";
            throw new IllegalArgumentException(message);
        }
        this.parents.add(parentType);
    }

    @Override
    public Collection<AbstractType> getParents() {
        return Collections.unmodifiableList(this.parents);
    }

    @Override
    public String getFullName() {
        return this.globalReference.getFullName();
    }

    @Override
    public String getName() {
        return this.globalReference.getIdentifier();
    }

    /**
     * Add a slot with a given name and type.
     * @param slotLocation Location of slot.
     * @param slotName Name of slot.
     * @return Added slot.
     */
    public Slot addSlot(final @Nullable Location slotLocation, final String slotName) {
        final Slot slot = new Slot(slotLocation, this, slotName);
        this.slots.put(slot.getName(), slot);
        return slot;
    }

    /**
     * Get a slot by name.
     * @param slotName Name of slot
     * @return Type of slot.
     */
    @Override
    public Slot getSlot(final String slotName) {
        if (this.slots.containsKey(slotName)) {
            return this.slots.get(slotName);
        }

        for (final AbstractType parent : this.getParents()) {
            final Slot slot = parent.getSlot(slotName);
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

    /**
     * Clear current slots, for this type.
     */
    public void clearSlots() {
        this.slots.clear();
    }

    /**
     * Add the resulting types of a method.
     * @param methodLocation Location of method.
     * @param methodName Name of method.
     * @param parameters Parameters of method.
     * @param assignmentParameter Assignment parameter for method.
     * @param callResult Results the method returns.
     */
    @SuppressWarnings("java:S1319")
    public Method addMethod(
            final EnumSet<Method.Modifier> modifiers,
            final @Nullable Location methodLocation,
            final String methodName,
            final List<Parameter> parameters,
            final @Nullable Parameter assignmentParameter,
            final ExpressionResult callResult) {
        final ExpressionResult loopbodyResult = new ExpressionResult();
        return this.addMethod(
            modifiers, methodLocation, methodName, parameters, assignmentParameter, callResult, loopbodyResult);
    }

    /**
     * Add the resulting types of a method and loopbody, overwrites existing methods.
     * @param methodLocation Location of method.
     * @param methodName Name of method.
     * @param parameters Parameters for method.
     * @param assignmentParameter Assignment parameter for method.
     * @param callResult {{MagikType}}s the method returns.
     * @param loopbodyResult {{MagikType}}s the method iterates.
     */
    @SuppressWarnings("java:S1319")
    public Method addMethod(
            final EnumSet<Method.Modifier> modifiers,
            final @Nullable Location methodLocation,
            final String methodName,
            final List<Parameter> parameters,
            final @Nullable Parameter assignmentParameter,
            final ExpressionResult callResult,
            final ExpressionResult loopbodyResult) {
        final Method method = new Method(
            modifiers, methodLocation, this, methodName, parameters, assignmentParameter, callResult, loopbodyResult);
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
        for (final AbstractType parent : this.parents) {
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
        return this.parents.stream()
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
        final Optional<AbstractType> superType = this.parents.stream()
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

}
