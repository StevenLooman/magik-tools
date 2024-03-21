package nl.ramsolutions.sw.magik.analysis.typing.types;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.typing.GenericHelper;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;

/** Magik type: slotted exemplar, indexed exemplar, enumeration, or mixin. */
public class MagikType extends AbstractType {

  /** Sort of MagikType. */
  public enum Sort {

    /** Type has not been seen yet, but, e.g., referred to by a method definition. */
    UNDEFINED,

    /** {@code object} type. */
    OBJECT,

    /** Slotted exemplar type. */
    SLOTTED,

    /** Indexed exemplar type. */
    INDEXED,

    /** Intrinsic type. */
    INTRINSIC;
  }

  private final TypeString typeString;
  private final Set<Method> methods = ConcurrentHashMap.newKeySet();
  private final Set<TypeString> parents = ConcurrentHashMap.newKeySet();
  private final Map<String, Slot> slots = new ConcurrentHashMap<>();
  private final Set<String> topics = new HashSet<>();
  private final Set<GenericDefinition> genericDefinitions = ConcurrentHashMap.newKeySet();
  private final ITypeKeeper typeKeeper;
  private Sort sort;

  /**
   * Constructor.
   *
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
  }

  /**
   * Create a copy of {@link MagikType}, but set the new {@link TypeString}.
   *
   * <p>Used for
   *
   * @param magikType Type to copy from.
   * @param newTypeString New {@link TypeString} to use.
   */
  public MagikType(final MagikType magikType, final TypeString newTypeString) {
    super(magikType.getLocation(), magikType.getModuleName());

    this.typeKeeper = magikType.typeKeeper;
    this.sort = magikType.sort;
    this.typeString = newTypeString;

    // Copy existing parts.
    this.parents.addAll(magikType.parents);
    magikType
        .slots
        .values()
        .forEach(slot -> this.addSlot(slot.getLocation(), slot.getName(), slot.getType()));
    magikType.methods.forEach(
        method -> {
          final Method newMethod =
              this.addMethod(
                  method.getLocation(),
                  method.getModuleName(),
                  method.getModifiers(),
                  method.getName(),
                  method.getParameters(),
                  method.getAssignmentParameter(),
                  method.getDoc(),
                  method.getCallResult(),
                  method.getLoopbodyResult());
          method
              .getGlobalUsages()
              .forEach(
                  globalUsage ->
                      newMethod.addUsedGlobal(
                          new Method.GlobalUsage(
                              globalUsage.getGlobal(), globalUsage.getLocation())));
          method
              .getMethodUsages()
              .forEach(
                  methodUsage ->
                      newMethod.addCalledMethod(
                          new Method.MethodUsage(
                              methodUsage.getType(),
                              methodUsage.getMethodName(),
                              methodUsage.getLocation())));
          method
              .getUsedSlots()
              .forEach(
                  slotUsage ->
                      newMethod.addUsedSlot(
                          new Method.SlotUsage(slotUsage.getSlotName(), slotUsage.getLocation())));
          method
              .getConditionUsages()
              .forEach(
                  conditionUsage ->
                      newMethod.addUsedCondition(
                          new Method.ConditionUsage(
                              conditionUsage.getConditionName(), conditionUsage.getLocation())));
        });
    typeString
        .getGenerics()
        .forEach(genDefTypeStr -> this.addGenericDefinition(null, genDefTypeStr));
  }

  /**
   * Get the magik type.
   *
   * @return
   */
  public Sort getSort() {
    return this.sort;
  }

  /** Set the magik type. */
  public void setSort(final Sort sort) {
    this.sort = sort;
  }

  /** Clear parents. */
  public void clearParents() {
    this.parents.clear();
  }

  /**
   * Add a parent type.
   *
   * @param parentTypeString Reference to parent type.
   */
  @SuppressWarnings("java:S2583")
  public void addParent(final TypeString parentTypeString) {
    this.parents.add(parentTypeString);
  }

  /**
   * Get parent {@link TypeString}s.
   *
   * @return Parents.
   */
  public Set<TypeString> getParentsTypeRefs() {
    // TODO: This does not work properly. Types don't provide any hints whether
    //       it inherits `sw:slotted_format_mixin`, such as `sw:rope_mixin`,
    //       or not, such as as `sw:ace_access_mixin`. `ExemplarDefinition.Sort`
    //       does not provide any usable information.
    final Set<TypeString> implicitParents = new HashSet<>();
    if (this.parents.isEmpty()) {
      if (this.getSort() == MagikType.Sort.INDEXED) {
        implicitParents.add(TypeString.SW_INDEXED_FORMAT_MIXIN);
      } else if (this.getSort() == MagikType.Sort.SLOTTED) {
        implicitParents.add(TypeString.SW_SLOTTED_FORMAT_MIXIN);
      }
    }

    final TypeString[] thisGenDefs = this.typeString.getGenerics().toArray(TypeString[]::new);
    return Stream.concat(this.parents.stream(), implicitParents.stream())
        .map(
            typeStr ->
                // Let all parents inherit generic definitions.
                // TODO: Keep existing genDefs, but overwrite any found.
                TypeString.ofIdentifier(typeStr.getIdentifier(), typeStr.getPakkage(), thisGenDefs))
        .collect(Collectors.toUnmodifiableSet());
  }

  @Override
  public Collection<AbstractType> getParents() {
    return this.getParentsTypeRefs().stream().map(this.typeKeeper::getType).toList();
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
   *
   * @param location {@link Location} of {@link GenericDefinition}.
   * @param genericTypeString {@link TypeString} of GenericDefinition.
   * @return
   */
  public GenericDefinition addGenericDefinition(
      final @Nullable Location location, final TypeString genericTypeString) {
    final GenericDefinition definition = new GenericDefinition(this.typeKeeper, genericTypeString);
    this.genericDefinitions.add(definition);
    return definition;
  }

  @Override
  public List<GenericDefinition> getGenericDefinitions() {
    return List.copyOf(this.genericDefinitions);
  }

  /**
   * Add a slot with a given name and type.
   *
   * @param location Location of slot.
   * @param name Name of slot.
   * @param slotTypeString Type of slot.
   * @return Added slot.
   */
  public Slot addSlot(
      final @Nullable Location location, final String name, final TypeString slotTypeString) {
    final Slot slot = new Slot(location, name, slotTypeString);
    this.slots.put(name, slot);
    return slot;
  }

  /**
   * Get a slot by name.
   *
   * @param name Name of slot
   * @return Type of slot.
   */
  @Override
  public Slot getSlot(final String name) {
    if (this.slots.containsKey(name)) {
      final GenericHelper genericHelper = new GenericHelper(typeKeeper, this);
      final Slot slot = this.slots.get(name);
      return genericHelper.substituteGenerics(slot);
    }

    for (final AbstractType parent : this.getParents()) {
      final Slot slot = parent.getSlot(name);
      if (slot != null) {
        final GenericHelper genericHelper = new GenericHelper(typeKeeper, this);
        return genericHelper.substituteGenerics(slot);
      }
    }

    return null;
  }

  /**
   * Get all slots from this type, including super-types.
   *
   * @return Slots.
   */
  @Override
  public Collection<Slot> getSlots() {
    final GenericHelper genericHelper = new GenericHelper(typeKeeper, this);
    final Collection<Slot> allSlots = new HashSet<>();

    allSlots.addAll(this.slots.values());
    for (final AbstractType parentType : this.getParents()) {
      allSlots.addAll(parentType.getSlots());
    }

    return allSlots.stream()
        .map(genericHelper::substituteGenerics)
        .collect(Collectors.toUnmodifiableSet());
  }

  /**
   * Get slots defined for this type, excluding super-types.
   *
   * @return
   */
  public Collection<Slot> getLocalSlots() {
    final GenericHelper genericHelper = new GenericHelper(typeKeeper, this);
    return this.slots.values().stream()
        .map(genericHelper::substituteGenerics)
        .collect(Collectors.toUnmodifiableSet());
  }

  /** Clear current slots, for this type. */
  public void clearSlots() {
    this.slots.clear();
  }

  /**
   * Add the resulting types of a method and loopbody, overwrites existing methods.
   *
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
  public Method addMethod( // NOSONAR
      final @Nullable Location location,
      final @Nullable String moduleName,
      final Set<Method.Modifier> modifiers,
      final String methodName,
      final List<Parameter> parameters,
      final @Nullable Parameter assignmentParameter,
      final @Nullable String methodDoc,
      final ExpressionResultString callResult,
      final ExpressionResultString loopbodyResult) {
    final Method method =
        new Method(
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
   * Get all methods this type has, including super-types.
   *
   * @return Methods for this type.
   */
  @Override
  public Collection<Method> getMethods() {
    final Map<String, Set<Method>> allMethods = new HashMap<>();

    // Add local methods.
    this.methods.forEach(
        method -> {
          final String methodName = method.getName();
          final Set<Method> methodsForName =
              allMethods.computeIfAbsent(methodName, key -> new HashSet<>());
          methodsForName.add(method);
        });

    // Add methods from parent types, if not overridden.
    for (final AbstractType parent : this.getParents()) {
      parent
          .getMethods()
          .forEach(
              method -> {
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

    final GenericHelper genericHelper = new GenericHelper(typeKeeper, this);
    return allMethods.values().stream()
        .flatMap(Collection::stream)
        .map(genericHelper::substituteGenerics)
        .collect(Collectors.toUnmodifiableSet());
  }

  @Override
  public Collection<Method> getLocalMethods() {
    final GenericHelper genericHelper = new GenericHelper(typeKeeper, this);
    return this.methods.stream()
        .map(genericHelper::substituteGenerics)
        .collect(Collectors.toUnmodifiableSet());
  }

  /**
   * Get any super method.
   *
   * @param methodName Name of method.
   * @return Method return types.
   */
  @Override
  public Collection<Method> getSuperMethods(final String methodName) {
    // Try to get the wanted method from all super types, first one wins.
    final GenericHelper genericHelper = new GenericHelper(typeKeeper, this);
    return this.getParents().stream()
        .flatMap(parent -> parent.getMethods(methodName).stream())
        .map(genericHelper::substituteGenerics)
        .collect(Collectors.toUnmodifiableSet());
  }

  /**
   * Get any super method from a specific parent.
   *
   * @param methodName Name of method.
   * @return Method return types.
   */
  @Override
  public Collection<Method> getSuperMethods(final String methodName, final String superName) {
    // If super-type was specified, specifically search that one.
    final Optional<AbstractType> superType =
        this.getParents().stream().filter(type -> type.getFullName().equals(superName)).findAny();
    if (!superType.isPresent()) {
      return Collections.emptySet();
    }

    final GenericHelper genericHelper = new GenericHelper(typeKeeper, this);
    return superType.get().getMethods(methodName).stream()
        .map(genericHelper::substituteGenerics)
        .collect(Collectors.toUnmodifiableSet());
  }

  /**
   * Remove a method.
   *
   * @param method Method to remove.
   */
  public void removeMethod(final Method method) {
    this.methods.remove(method);
  }

  @Override
  public Set<String> getTopics() {
    return Collections.unmodifiableSet(this.topics);
  }

  public void addTopic(final String topic) {
    this.topics.add(topic);
  }

  @Override
  public String toString() {
    return String.format(
        "%s@%s(%s)",
        this.getClass().getName(), Integer.toHexString(this.hashCode()), this.getFullName());
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
    return Objects.equals(this.typeString, other.typeString);
  }
}
