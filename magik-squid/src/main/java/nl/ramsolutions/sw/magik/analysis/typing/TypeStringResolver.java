package nl.ramsolutions.sw.magik.analysis.typing;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
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
   * Get the {@link ExemplarDefinition} from a {@link TypeString}.
   *
   * <p>Note that this gives only a singular {@link ExemplarDefinition}, even though there might be
   * multiple known.
   *
   * @param typeString {@link TypeString} to resolve.
   * @return Found {@link ExemplarDefinition}, or null.
   */
  @CheckForNull
  public ExemplarDefinition getExemplarDefinition(final TypeString typeString) {
    // TODO: Return type should be Collection<ExemplarDefinition>
    final Collection<ITypeStringDefinition> definitions = this.resolve(typeString);
    if (definitions.isEmpty()) {
      return null;
    }

    // Prefer ExemplarDefinitions.
    final ITypeStringDefinition exemplarDefefinition =
        definitions.stream().filter(ExemplarDefinition.class::isInstance).findAny().orElse(null);
    final ITypeStringDefinition definition =
        exemplarDefefinition != null ? exemplarDefefinition : definitions.iterator().next();

    // Resolve global first.
    if (definition instanceof GlobalDefinition globalDefinition0) {
      final TypeString aliasedTypeString = globalDefinition0.getAliasedTypeName();
      return this.getExemplarDefinition(aliasedTypeString);
    }

    // Treat a procedure definition as the exemplar `procedure`.
    if (definition instanceof ProcedureDefinition) {
      return this.getExemplarDefinition(TypeString.SW_PROCEDURE);
    }

    return definition instanceof ExemplarDefinition exemplarDefinition ? exemplarDefinition : null;
  }

  /**
   * Test if {@link typeString1} is kind of {@link typeString2}.
   *
   * @param typeString1 The thing to test.
   * @param typeString2 The kind to test for.
   * @return True if is kind of, false otherwise.
   */
  public boolean isKindOf(final TypeString typeString1, final TypeString typeString2) {
    for (final TypeString typeStr1 : TypeString.combine(typeString1).getCombinedTypes()) {
      final ITypeStringDefinition definition1 =
          this.resolve(typeStr1).stream().findAny().orElse(null);
      if (definition1 == null) {
        continue;
      }

      final TypeString combinedTypeString2 = TypeString.combine(typeString2);
      Objects.requireNonNull(combinedTypeString2);
      for (final TypeString typeStr2 : combinedTypeString2.getCombinedTypes()) {
        final ITypeStringDefinition definition2 =
            this.resolve(typeStr2).stream().findAny().orElse(null);
        if (definition2 == null) {
          continue;
        }

        if (this.isKindOf(definition1, definition2)) {
          return true;
        }
      }
    }

    return false;
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
                        resolvedTypes.isEmpty()
                            ? typeStr
                            : resolvedTypes.iterator().next().getTypeString();

                    final Map<String, MethodDefinition> methodDefinitionsByName = new HashMap<>();
                    this.fillRespondingMethodDefinitions(actualTypeStr, methodDefinitionsByName);
                    return methodDefinitionsByName.values().stream().collect(Collectors.toSet());
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
                        resolvedTypes.isEmpty()
                            ? typeStr
                            : resolvedTypes.iterator().next().getTypeString();

                    final Map<TypeString, ProcedureDefinition> procedureDefinitionsByType =
                        new HashMap<>();
                    this.fillRespondingProcedureDefinitions(
                        actualTypeStr, procedureDefinitionsByType);
                    return procedureDefinitionsByType.values().stream().collect(Collectors.toSet());
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
    final TypeString actualTypeStr =
        resolvedTypes.isEmpty() ? typeStr : resolvedTypes.iterator().next().getTypeString();

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

  private void fillRespondingMethodDefinitions(
      final TypeString typeString, final Map<String, MethodDefinition> methodDefinitions) {
    // TODO: This doesn't handle any conflicts.
    this.getSelfAndAncestors(typeString)
        .forEach(
            typeStr ->
                this.definitionKeeper
                    .getMethodDefinitions(typeStr)
                    .forEach(
                        methodDefinition -> {
                          final String methodName = methodDefinition.getMethodName();
                          if (methodDefinitions.containsKey(methodName)) {
                            // Don't overwrite.
                            return;
                          }

                          methodDefinitions.put(methodName, methodDefinition);
                        }));
  }

  private void fillRespondingProcedureDefinitions(
      final TypeString typeString,
      final Map<TypeString, ProcedureDefinition> procedureDefinitions) {
    // TODO: This doesn't handle any conflicts.
    this.getSelfAndAncestors(typeString)
        .forEach(
            typeStr ->
                this.definitionKeeper
                    .getProcedureDefinitions(typeStr)
                    .forEach(
                        procedureDefinition -> {
                          final TypeString procedureType = procedureDefinition.getTypeString();
                          if (procedureDefinitions.containsKey(procedureType)) {
                            // Don't overwrite.
                            return;
                          }

                          procedureDefinitions.put(procedureType, procedureDefinition);
                        }));
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
      final TypeString typeString = globalDefinition.getAliasedTypeName();
      final ITypeStringDefinition aliasedDefinition =
          this.resolve(typeString).stream().findAny().orElse(null);
      if (aliasedDefinition == null) {
        return Collections.emptySet();
      }

      return this.getParents(aliasedDefinition);
    }

    throw new IllegalStateException();
  }

  /**
   * Get the parents of a {@link TypeString}.
   *
   * <p>This adds the implicit parents, where {@link ExemplarDefinition} only returns its explicitly
   * defined parents.
   *
   * <p>The returned parents are UNORDERED: this returns a {@link Set}, and the order in which
   * multiple parents are visited during resolution (e.g., which parent wins in {@code
   * fillRespondingMethodDefinitions}'s first-wins lookup) is unspecified. This predates the move to
   * first-class {@link InheritanceDefinition} edges -- the resolver already returned an unordered
   * set of parents before that change, so method-resolution order among competing parents was
   * already nondeterministic and remains so.
   *
   * @param typeString {@link TypeString} to get parents from.
   * @return Parents of the given type.
   */
  public Collection<TypeString> getParents(final TypeString typeString) {
    // TODO: This can be multiple.
    final ExemplarDefinition exemplarDefinition = this.getExemplarDefinition(typeString);
    if (exemplarDefinition == null) {
      return Collections.emptyList();
    }

    // A parent mixin *can* provide a default mixin, but does not have to be the case. I.e.,
    // the sw:rope_mixin does inherit from sw:slotted_format_mixin, but
    // sw:serial_structure_indexed_mixin does not do so. Most mixins do not inherit from
    // sw:slotted_format_mixin. As such, we assume that a parent mixin does not provide
    // a default mixin.
    final List<TypeString> parents =
        this.definitionKeeper.getInheritanceDefinitions(exemplarDefinition.getTypeString()).stream()
            .map(InheritanceDefinition::getParentTypeName)
            .toList();
    final Collection<TypeString> nonMixinParents =
        parents.stream()
            .filter(
                parentTypeString -> {
                  final ExemplarDefinition parentExemplarDefinition =
                      this.getExemplarDefinition(parentTypeString);
                  final ExemplarDefinition.Sort parentExemplarSort =
                      parentExemplarDefinition != null
                          ? parentExemplarDefinition.getSort()
                          : ExemplarDefinition.Sort.UNDEFINED;
                  return parentExemplarSort == ExemplarDefinition.Sort.SLOTTED
                      || parentExemplarSort == ExemplarDefinition.Sort.INDEXED;
                })
            .toList();
    final Sort sort = exemplarDefinition.getSort();
    final TypeString implicitParentTypeStr =
        nonMixinParents.isEmpty() ? IMPLICIT_PARENTS.get(sort) : null;

    final TypeString[] thisGenDefs = typeString.getGenerics().toArray(TypeString[]::new);
    return Stream.concat(parents.stream(), Optional.ofNullable(implicitParentTypeStr).stream())
        .map(
            typeStr ->
                // Let all parents inherit generic definitions.
                typeStr.withGenerics(thisGenDefs))
        .collect(Collectors.toUnmodifiableSet());
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
