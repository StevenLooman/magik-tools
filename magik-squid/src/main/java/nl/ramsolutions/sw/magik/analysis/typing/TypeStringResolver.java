package nl.ramsolutions.sw.magik.analysis.typing;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition.Sort;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ITypeStringDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.InheritanceDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.PackageDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ProcedureDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.SlotDefinition;

/** {@link TypeString} resolver tools. */
public class TypeStringResolver {

  private static final String ALL_METHODS = "_all_methods";
  private static final String ALL_PROCEDURES = "_all_procedures";

  private static final Map<ExemplarDefinition.Sort, TypeString> IMPLICIT_PARENTS =
      Map.of(
          ExemplarDefinition.Sort.INDEXED, TypeString.SW_INDEXED_FORMAT_MIXIN,
          ExemplarDefinition.Sort.SLOTTED, TypeString.SW_SLOTTED_FORMAT_MIXIN);

  private final IDefinitionKeeper definitionKeeper;
  private final Map<TypeString, Set<ITypeStringDefinition>> typeCache = new HashMap<>();
  private final Map<Map.Entry<TypeString, String>, Collection<MethodDefinition>> methodsCache =
      new HashMap<>();
  private final Map<Map.Entry<TypeString, String>, Collection<ProcedureDefinition>>
      proceduresCache = new HashMap<>();

  public TypeStringResolver(final IDefinitionKeeper definitionKeeper) {
    this.definitionKeeper = definitionKeeper;
  }

  private List<PackageDefinition> getPackageHierarchy(final TypeString typeString) {
    final Deque<String> packages = new ArrayDeque<>();
    final String startPackage = typeString.getPakkage();
    packages.push(startPackage);

    // Iterate through package structure.
    final List<PackageDefinition> seen = new ArrayList<>();
    while (!packages.isEmpty()) {
      final String packageName = packages.pop();
      this.definitionKeeper.getPackageDefinitions(packageName).stream()
          .filter(def -> !seen.contains(def))
          .map(
              def -> {
                seen.add(def);
                return def;
              })
          .flatMap(def -> def.getUses().stream())
          .forEach(packages::push);
    }

    return seen;
  }

  /**
   * Test if the {@link TypeString} is known.
   *
   * @param typeString Reference to look for.
   * @return True if known, false otherwise.
   */
  public boolean hasTypeDefinition(final TypeString typeString) {
    return !this.resolve(typeString).isEmpty();
  }

  /**
   * Get the {@link ITypeStringDefinition} for the given {@link TypeString}, following package uses.
   *
   * @param typeString Reference to look for.
   * @return A {@link ExemplarDefinition}/{@link ProcedureDefinition}/{@link GlobalDefinition}.
   */
  public synchronized Collection<ITypeStringDefinition> resolve(final TypeString typeString) {
    return this.typeCache.computeIfAbsent(typeString, this::resolveInPackageHierarchy);
  }

  private Set<ITypeStringDefinition> resolveInPackageHierarchy(final TypeString typeString) {
    // Only a single type reference can resolve to a definition; a combined/variadic type
    // has no identifier to look up.
    if (!typeString.isSingle()) {
      return Set.of();
    }

    // Walk the package-use hierarchy nearest-first. The first package that defines the
    // identifier shadows definitions of the same name in packages it uses, so a reference
    // resolves to a single type instead of a union of same-named types across the hierarchy.
    final String identifier = typeString.getIdentifier();
    for (final PackageDefinition packageDef : this.getPackageHierarchy(typeString)) {
      final TypeString pkgTypeString = TypeString.ofIdentifier(identifier, packageDef.getName());
      final Set<ITypeStringDefinition> definitions =
          Stream.of(
                  this.definitionKeeper.getExemplarDefinitions(pkgTypeString).stream(),
                  this.definitionKeeper.getProcedureDefinitions(pkgTypeString).stream(),
                  this.definitionKeeper.getGlobalDefinitions(pkgTypeString).stream())
              .flatMap(stream -> stream)
              .filter(Objects::nonNull)
              .collect(Collectors.toSet());
      if (!definitions.isEmpty()) {
        return definitions;
      }
    }

    return Set.of();
  }

  /**
   * Get every {@link ExemplarDefinition} a {@link TypeString} can stand for.
   *
   * <p>An exemplar stands for itself, a procedure for {@code procedure}, and a global for what its
   * aliased type stands for. Colliding definitions give the union, as which one wins at runtime is
   * unknown. An untyped global adds nothing.
   *
   * @param typeString {@link TypeString} to resolve.
   * @return The exemplars, empty when there are none.
   */
  public Collection<ExemplarDefinition> getExemplarDefinitions(final TypeString typeString) {
    final Set<ExemplarDefinition> exemplarDefinitions =
        this.collectExemplarDefinitions(typeString, new HashSet<>());
    return Collections.unmodifiableSet(exemplarDefinitions);
  }

  private Set<ExemplarDefinition> collectExemplarDefinitions(
      final TypeString typeString, final Set<TypeString> visited) {
    // An alias cycle (e.g. a global aliased to itself) is unresolvable.
    if (!visited.add(typeString)) {
      return Collections.emptySet();
    }

    final Set<ExemplarDefinition> exemplarDefinitions = new HashSet<>();
    for (final ITypeStringDefinition definition : this.resolve(typeString)) {
      if (definition instanceof final ExemplarDefinition exemplarDefinition) {
        exemplarDefinitions.add(exemplarDefinition);
        continue;
      }

      // Follow a global's alias; treat a procedure definition as the exemplar `procedure`.
      final TypeString nextTypeString =
          definition instanceof final GlobalDefinition globalDefinition
              ? globalDefinition.getAliasedTypeName()
              : TypeString.SW_PROCEDURE;
      final Set<TypeString> branchVisited = new HashSet<>(visited);
      final Set<ExemplarDefinition> branchDefinitions =
          this.collectExemplarDefinitions(nextTypeString, branchVisited);
      exemplarDefinitions.addAll(branchDefinitions);
    }
    return exemplarDefinitions;
  }

  /**
   * The type string of the exemplars a {@link TypeString} stands for, e.g. to key a keeper lookup
   * by the defining package.
   *
   * <p>Exemplars sharing a name give that name, bare when their generics differ. Exemplars with
   * different names give {@code typeString}.
   *
   * @param typeString {@link TypeString} to resolve.
   * @return The shared exemplar type string, or {@code typeString} when there is none.
   */
  public TypeString getExemplarTypeString(final TypeString typeString) {
    final Collection<ExemplarDefinition> exemplarDefinitions =
        this.getExemplarDefinitions(typeString);
    final Set<TypeString> typeStrings =
        exemplarDefinitions.stream()
            .map(ExemplarDefinition::getTypeString)
            .collect(Collectors.toSet());
    return TypeStringResolver.getSharedTypeString(typeString, typeStrings);
  }

  /**
   * Whether a {@link TypeString} stands for at least one {@link ExemplarDefinition}.
   *
   * @param typeString {@link TypeString} to resolve.
   * @return True when it does.
   */
  public boolean hasExemplarDefinition(final TypeString typeString) {
    final Collection<ExemplarDefinition> exemplarDefinitions =
        this.getExemplarDefinitions(typeString);
    return !exemplarDefinitions.isEmpty();
  }

  /**
   * The type string all resolved definitions share, or their bare name when they differ only in
   * generics.
   *
   * @param typeString The reference.
   * @param resolvedTypes What it resolved to.
   * @return The shared type string, or {@code typeString} when nothing resolved.
   */
  private static TypeString getSharedTypeString(
      final TypeString typeString, final Collection<ITypeStringDefinition> resolvedTypes) {
    final Set<TypeString> typeStrings =
        resolvedTypes.stream()
            .map(ITypeStringDefinition::getTypeString)
            .collect(Collectors.toSet());
    return TypeStringResolver.getSharedTypeString(typeString, typeStrings);
  }

  /** See {@link #getSharedTypeString(TypeString, Collection)}; over the type strings themselves. */
  private static TypeString getSharedTypeString(
      final TypeString typeString, final Set<TypeString> typeStrings) {
    if (typeStrings.size() == 1) {
      final Iterator<TypeString> iterator = typeStrings.iterator();
      return iterator.next();
    }

    // Same name, differing only in generics.
    final Set<TypeString> bareTypeStrings =
        typeStrings.stream().map(TypeString::getWithoutGenerics).collect(Collectors.toSet());
    if (bareTypeStrings.size() == 1) {
      final Iterator<TypeString> iterator = bareTypeStrings.iterator();
      return iterator.next();
    }

    return typeString;
  }

  /**
   * The type string {@code typeString} resolves to; see {@link #getSharedTypeString}.
   *
   * @param typeString The reference.
   * @return The resolved type string, or {@code typeString} when it resolves to nothing.
   */
  public TypeString getResolvedTypeString(final TypeString typeString) {
    final Collection<ITypeStringDefinition> resolvedTypes = this.resolve(typeString);
    return TypeStringResolver.getSharedTypeString(typeString, resolvedTypes);
  }

  /**
   * Test if any member of {@link typeString1}, through any of its definitions, is kind of any
   * member of {@link typeString2}.
   *
   * @param typeString1 The thing to test.
   * @param typeString2 The kind to test for.
   * @return True if is kind of, false otherwise.
   */
  public boolean isKindOf(final TypeString typeString1, final TypeString typeString2) {
    // Any member or colliding definition may be the runtime value.
    final TypeString combinedTypeString2 = TypeString.combine(typeString2);
    Objects.requireNonNull(combinedTypeString2);
    final List<ITypeStringDefinition> definitions2 =
        combinedTypeString2.getCombinedTypes().stream()
            .flatMap(typeStr2 -> this.resolve(typeStr2).stream())
            .toList();
    return TypeString.combine(typeString1).getCombinedTypes().stream()
        .flatMap(typeStr1 -> this.resolve(typeStr1).stream())
        .anyMatch(
            definition1 ->
                definitions2.stream()
                    .anyMatch(definition2 -> this.isKindOf(definition1, definition2)));
  }

  /**
   * Test if {@link definition1} is kind of {@link definition2}.
   *
   * @param definition1 The thing to test.
   * @param definition2 The kind to test for.
   * @return True if is kind of, false otherwise.
   */
  public boolean isKindOf(
      final ITypeStringDefinition definition1, final ITypeStringDefinition definition2) {
    final TypeString typeString1 = definition1.getTypeString();
    final TypeString typeString2 = definition2.getTypeString();
    if (typeString1.equals(typeString2)) {
      return true;
    }

    return this.getParents(definition1).stream()
        .anyMatch(parentTypeString1 -> this.isKindOf(parentTypeString1, typeString2));
  }

  /**
   * Get the {@link MethodDefinition}s the {@link TypeString} responds to, including from its super
   * types.
   *
   * @param typeString {@link TypeString} to resolve.
   * @return {@link MethodDefinition}s the {@link TypeString} responds to.
   */
  public synchronized Collection<MethodDefinition> getRespondingMethodDefinitions(
      final TypeString typeString) {
    return typeString.getCombinedTypes().stream()
        .map(
            typeStr -> {
              final Entry<TypeString, String> cacheKey = Map.entry(typeStr, ALL_METHODS);
              return this.methodsCache.computeIfAbsent(
                  cacheKey,
                  entry -> {
                    // Try to resolve the typeString to an actual type.
                    final Collection<ITypeStringDefinition> resolvedTypes = this.resolve(typeStr);
                    final TypeString actualTypeStr =
                        TypeStringResolver.getSharedTypeString(typeStr, resolvedTypes);

                    return this.collectRespondingMethodDefinitions(actualTypeStr);
                  });
            })
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
  }

  /**
   * Get the {@link ProcedureDefinition}s the {@link TypeString} responds to, including from its
   * super types.
   *
   * @param typeString {@link TypeString} to resolve.
   * @return {@link ProcedureDefinition}s the {@link TypeString} responds to.
   */
  public synchronized Collection<ProcedureDefinition> getRespondingProcedureDefinitions(
      final TypeString typeString) {
    return typeString.getCombinedTypes().stream()
        .map(
            typeStr -> {
              final Entry<TypeString, String> cacheKey = Map.entry(typeStr, ALL_PROCEDURES);
              return this.proceduresCache.computeIfAbsent(
                  cacheKey,
                  entry -> {
                    // Try to resolve the typeString to an actual type.
                    final Collection<ITypeStringDefinition> resolvedTypes = this.resolve(typeStr);
                    final TypeString actualTypeStr =
                        TypeStringResolver.getSharedTypeString(typeStr, resolvedTypes);

                    return this.collectRespondingProcedureDefinitions(actualTypeStr);
                  });
            })
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
  }

  /**
   * Get the {@link MethodDefinition} that responds to the given {@link TypeString} and {@link
   * methodName}.
   *
   * @param typeString {@link TypeString}(s) to resolve.
   * @param methodName Method name to resolve.
   * @return {@link MethodDefinition} that are responding to the given type and method name.
   */
  public synchronized Collection<MethodDefinition> getRespondingMethodDefinitions(
      final TypeString typeString, final String methodName) {
    return typeString.getCombinedTypes().stream()
        .map(typeStr -> this.getRespondingMethodDefinitionsForType(typeStr, methodName))
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
  }

  /** Resolve {@link methodName} against a single (non-combined) {@link TypeString}. */
  private Collection<MethodDefinition> getRespondingMethodDefinitionsForType(
      final TypeString typeStr, final String methodName) {
    // Resolve typeString.
    final Collection<ITypeStringDefinition> resolvedTypes = this.resolve(typeStr);
    final TypeString actualTypeStr = TypeStringResolver.getSharedTypeString(typeStr, resolvedTypes);

    // The type's own methods shadow every parent.
    final Collection<MethodDefinition> ownDefinitions =
        this.definitionKeeper.getMethodDefinitions(actualTypeStr).stream()
            .filter(def -> def.getMethodName().equals(methodName))
            .collect(Collectors.toSet());
    if (!ownDefinitions.isEmpty()) {
      return ownDefinitions;
    }

    // Gather every responding branch: the parent set is unordered, so first-wins was a coin flip.
    final Collection<MethodDefinition> parentDefinitions =
        this.getParents(typeStr).stream()
            .map(parentTypeStr -> this.getRespondingMethodDefinitions(parentTypeStr, methodName))
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
    if (!parentDefinitions.isEmpty()) {
      return this.preferConcreteMethodDefinitions(parentDefinitions);
    }

    return ownDefinitions;
  }

  /** Prefer concrete implementations over {@code _abstract} ones, unless none are concrete. */
  private Collection<MethodDefinition> preferConcreteMethodDefinitions(
      final Collection<MethodDefinition> methodDefinitions) {
    final Collection<MethodDefinition> concreteDefinitions =
        methodDefinitions.stream()
            .filter(
                methodDefinition -> {
                  final Set<MethodDefinition.Modifier> modifiers = methodDefinition.getModifiers();
                  return !modifiers.contains(MethodDefinition.Modifier.ABSTRACT);
                })
            .collect(Collectors.toSet());
    return concreteDefinitions.isEmpty() ? methodDefinitions : concreteDefinitions;
  }

  /**
   * Every method a type responds to, walking the hierarchy nearest level first. A nearer level
   * shadows a farther one; same-named methods at one level all respond.
   */
  private Collection<MethodDefinition> collectRespondingMethodDefinitions(
      final TypeString typeString) {
    final Map<String, Set<MethodDefinition>> byName = new HashMap<>();
    final Set<TypeString> seen = new HashSet<>();
    Set<TypeString> level = Set.of(typeString);
    while (!level.isEmpty()) {
      level.stream()
          .flatMap(typeStr -> this.definitionKeeper.getMethodDefinitions(typeStr).stream())
          .collect(Collectors.groupingBy(MethodDefinition::getMethodName, Collectors.toSet()))
          .forEach(byName::putIfAbsent);
      seen.addAll(level);
      level =
          level.stream()
              .flatMap(typeStr -> this.getParents(typeStr).stream())
              .filter(typeStr -> !seen.contains(typeStr))
              .collect(Collectors.toSet());
    }
    return byName.values().stream().flatMap(Set::stream).collect(Collectors.toSet());
  }

  /** Every procedure of a type and its ancestors; colliding definitions all respond. */
  private Collection<ProcedureDefinition> collectRespondingProcedureDefinitions(
      final TypeString typeString) {
    return this.getSelfAndAncestors(typeString).stream()
        .flatMap(typeStr -> this.definitionKeeper.getProcedureDefinitions(typeStr).stream())
        .collect(Collectors.toSet());
  }

  /**
   * Get the {@link SlotDefinition}s defined directly on the given {@link TypeString}, following
   * package uses.
   *
   * @param typeString {@link TypeString} to get the slots for.
   * @return Own {@link SlotDefinition}s for the given type.
   */
  private Collection<SlotDefinition> getOwnSlotDefinitions(final TypeString typeString) {
    return this.getPackageHierarchy(typeString).stream()
        .sequential()
        .flatMap(
            def -> {
              final String packageName = def.getName();
              final TypeString pkgTypeString =
                  TypeString.ofIdentifier(typeString.getIdentifier(), packageName);
              return this.definitionKeeper.getSlotDefinitions(pkgTypeString).stream();
            })
        .collect(Collectors.toSet());
  }

  /**
   * Get all the {@link SlotDefinition}s for the given {@link TypeString}, including inherited
   * slots.
   *
   * <p>Walks the hierarchy level by level, nearest-first: a slot name found at a shallower level
   * shadows a same-named slot from an ancestor. Slots sharing a name at the same level (e.g., two
   * files contributing the same slot name to one type) all survive, so that conflict surfaces
   * instead of being silently resolved.
   *
   * @param typeString {@link TypeString} to get the slots for.
   * @return All {@link SlotDefinition}s for the given type, own and inherited.
   */
  public Collection<SlotDefinition> getSlotDefinitions(final TypeString typeString) {
    final Map<String, Set<SlotDefinition>> byName = new HashMap<>();
    final Set<TypeString> seen = new HashSet<>();
    Set<TypeString> level = Set.of(typeString);
    while (!level.isEmpty()) {
      level.stream()
          .flatMap(typeStr -> this.getOwnSlotDefinitions(typeStr).stream())
          .collect(Collectors.groupingBy(SlotDefinition::getName, Collectors.toSet()))
          .forEach(byName::putIfAbsent);
      seen.addAll(level);
      level =
          level.stream()
              .flatMap(typeStr -> this.getParents(typeStr).stream())
              .filter(typeStr -> !seen.contains(typeStr))
              .collect(Collectors.toSet());
    }
    return byName.values().stream().flatMap(Set::stream).collect(Collectors.toSet());
  }

  /**
   * Get the {@link SlotDefinition}s with the given name for the given {@link TypeString}.
   *
   * @param typeString {@link TypeString} to get the slots for.
   * @param name Name of the slot.
   * @return Matching {@link SlotDefinition}s.
   */
  public Collection<SlotDefinition> getSlotDefinitions(
      final TypeString typeString, final String name) {
    return this.getSlotDefinitions(typeString).stream()
        .filter(definition -> definition.getName().equals(name))
        .collect(Collectors.toSet());
  }

  private Collection<TypeString> getParents(final ITypeStringDefinition definition) {
    return this.getParents(definition, new HashSet<>());
  }

  private Collection<TypeString> getParents(
      final ITypeStringDefinition definition, final Set<TypeString> visited) {
    if (definition instanceof ExemplarDefinition exemplarDefinition) {
      return this.definitionKeeper
          .getInheritanceDefinitions(exemplarDefinition.getTypeString())
          .stream()
          .map(InheritanceDefinition::getParentTypeName)
          .collect(Collectors.toSet());
    } else if (definition instanceof ProcedureDefinition) {
      // TODO: Is this right?
      return Set.of(TypeString.SW_PROCEDURE);
    } else if (definition instanceof GlobalDefinition globalDefinition) {
      // An alias cycle (e.g. a global aliased to itself) has no parents.
      final TypeString globalTypeString = globalDefinition.getTypeString();
      if (!visited.add(globalTypeString)) {
        return Collections.emptySet();
      }

      final TypeString typeString = globalDefinition.getAliasedTypeName();
      return this.resolve(typeString).stream()
          .map(aliasedDefinition -> this.getParents(aliasedDefinition, new HashSet<>(visited)))
          .flatMap(Collection::stream)
          .collect(Collectors.toSet());
    }

    throw new IllegalStateException();
  }

  /**
   * Get the parents of a {@link TypeString}.
   *
   * <p>This adds the implicit parents, where {@link ExemplarDefinition} only returns its explicitly
   * defined parents.
   *
   * <p>A type defined more than once gives the union of each definition's parents. The returned
   * parents are unordered; callers that resolve methods over them consider every parent branch.
   *
   * @param typeString {@link TypeString} to get parents from.
   * @return Parents of the given type.
   */
  public Collection<TypeString> getParents(final TypeString typeString) {
    // Union over every exemplar the type stands for.
    final TypeString[] thisGenDefs = typeString.getGenerics().toArray(TypeString[]::new);
    return this.getExemplarDefinitions(typeString).stream()
        .flatMap(exemplarDefinition -> this.getParents(exemplarDefinition).stream())
        .map(
            typeStr ->
                // Let all parents inherit generic definitions.
                typeStr.withGenerics(thisGenDefs))
        .collect(Collectors.toUnmodifiableSet());
  }

  /** The explicit parents of one exemplar, plus its implicit parent when it has no other. */
  private Collection<TypeString> getParents(final ExemplarDefinition exemplarDefinition) {
    // A parent mixin *can* provide a default mixin, but does not have to be the case. I.e.,
    // the sw:rope_mixin does inherit from sw:slotted_format_mixin, but
    // sw:serial_structure_indexed_mixin does not do so. Most mixins do not inherit from
    // sw:slotted_format_mixin. As such, we assume that a parent mixin does not provide
    // a default mixin.
    final List<TypeString> parents =
        this.definitionKeeper.getInheritanceDefinitions(exemplarDefinition.getTypeString()).stream()
            .map(InheritanceDefinition::getParentTypeName)
            .toList();
    final boolean hasNonMixinParent = parents.stream().anyMatch(this::isSlottedOrIndexed);
    final Sort sort = exemplarDefinition.getSort();
    final TypeString implicitParentTypeStr = hasNonMixinParent ? null : IMPLICIT_PARENTS.get(sort);
    return Stream.concat(parents.stream(), Optional.ofNullable(implicitParentTypeStr).stream())
        .toList();
  }

  private boolean isSlottedOrIndexed(final TypeString typeString) {
    return this.getExemplarDefinitions(typeString).stream()
        .map(ExemplarDefinition::getSort)
        .anyMatch(sort -> sort == Sort.SLOTTED || sort == Sort.INDEXED);
  }

  /**
   * Find all ancestors for a given {@link TypeString}.
   *
   * @param typeString {@link TypeString} to get ancestors from.
   * @return All ancestors this the given type.
   */
  public Collection<TypeString> getAllAncestors(final TypeString typeString) {
    final List<TypeString> ancestors = new ArrayList<>();
    this.getAllAncestors(typeString, ancestors, new HashSet<>());
    return ancestors;
  }

  private void getAllAncestors(
      final TypeString typeString, final List<TypeString> ancestors, final Set<TypeString> seen) {
    if (!seen.add(typeString)) {
      return;
    }
    final Collection<TypeString> typeStringParents = this.getParents(typeString);
    ancestors.addAll(typeStringParents);

    // Recurse over the resolved exemplar's edges. NB: this intentionally recurses over the raw
    // edge parents (no implicit parents), preserving pre-refactor behaviour. See the
    // getallancestors prompt under docs/superpowers/prompts/ for the deliberately-untouched quirk.
    this.resolve(typeString).stream()
        .filter(ExemplarDefinition.class::isInstance)
        .map(ExemplarDefinition.class::cast)
        .flatMap(
            def ->
                this.definitionKeeper.getInheritanceDefinitions(def.getTypeString()).stream()
                    .map(InheritanceDefinition::getParentTypeName))
        .forEach(parentTypeStr -> this.getAllAncestors(parentTypeStr, ancestors, seen));
  }

  public Collection<TypeString> getSelfAndAncestors(final TypeString typeString) {
    return Stream.concat(Stream.of(typeString), this.getAllAncestors(typeString).stream())
        .collect(Collectors.toUnmodifiableSet());
  }
}
