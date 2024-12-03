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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ITypeStringDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.PackageDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ProcedureDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.SlotDefinition;

/** {@link TypeString} resolver tools. */
public class TypeStringResolver {

  private static final String ALL_METHODS = "_all_methods";

  private final IDefinitionKeeper definitionKeeper;
  private final Map<TypeString, Set<ITypeStringDefinition>> typeCache = new HashMap<>();
  private final Map<Map.Entry<TypeString, String>, Collection<MethodDefinition>> methodsCache =
      new HashMap<>();

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

  private Collection<ExemplarDefinition> findExemplarDefinitions(final TypeString typeString) {
    return this.getPackageHierarchy(typeString).stream()
        .sequential()
        .flatMap(
            def -> {
              final String packageName = def.getName();
              final TypeString pkgTypeString =
                  TypeString.ofIdentifier(typeString.getIdentifier(), packageName);
              return this.definitionKeeper.getExemplarDefinitions(pkgTypeString).stream();
            })
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  private Collection<ProcedureDefinition> findProcedureDefinitions(final TypeString typeString) {
    return this.getPackageHierarchy(typeString).stream()
        .sequential()
        .flatMap(
            def -> {
              final String packageName = def.getName();
              final TypeString pkgTypeString =
                  TypeString.ofIdentifier(typeString.getIdentifier(), packageName);
              return this.definitionKeeper.getProcedureDefinitions(pkgTypeString).stream();
            })
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  private Collection<GlobalDefinition> findGlobalDefinitions(final TypeString typeString) {
    return this.getPackageHierarchy(typeString).stream()
        .sequential()
        .flatMap(
            def -> {
              final String packageName = def.getName();
              final TypeString pkgTypeString =
                  TypeString.ofIdentifier(typeString.getIdentifier(), packageName);
              return this.definitionKeeper.getGlobalDefinitions(pkgTypeString).stream();
            })
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
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
    return this.typeCache.computeIfAbsent(
        typeString,
        typeStr -> {
          final Collection<ExemplarDefinition> exemplarDefinitions =
              this.findExemplarDefinitions(typeStr);
          final Collection<ProcedureDefinition> procedureDefinitions =
              this.findProcedureDefinitions(typeStr);
          final Collection<GlobalDefinition> globalDefinitions =
              this.findGlobalDefinitions(typeStr);
          return Stream.of(
                  exemplarDefinitions.stream(),
                  procedureDefinitions.stream(),
                  globalDefinitions.stream())
              .flatMap(stream -> stream)
              .collect(Collectors.toSet());
        });
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
        .map(
            typeStr -> {
              // Resolve typeString.
              final Collection<ITypeStringDefinition> resolvedTypes = this.resolve(typeStr);
              final TypeString actualTypeStr =
                  resolvedTypes.isEmpty()
                      ? typeStr
                      : resolvedTypes.iterator().next().getTypeString();

              // Find first method to respond.
              final Collection<MethodDefinition> methodDefinitions =
                  this.definitionKeeper.getMethodDefinitions(actualTypeStr).stream()
                      .filter(def -> def.getMethodName().equals(methodName))
                      .collect(Collectors.toSet());
              if (!methodDefinitions.isEmpty()) {
                return methodDefinitions;
              }

              // Iterate through parents, breadth first search.
              for (final TypeString parentTypeString : this.getParents(typeStr)) {
                final Collection<MethodDefinition> parentDefinitions =
                    this.getRespondingMethodDefinitions(parentTypeString, methodName);
                if (!parentDefinitions.isEmpty()) {
                  return parentDefinitions;
                }
              }

              return methodDefinitions;
            })
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
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

  /**
   * Get all the {@link SlotDefinition}s for the given {@link TypeString}.
   *
   * @param typeString {@link TypeString} to resolve.
   * @return All {@link SlotDefinition}s for the given type.
   */
  public Collection<SlotDefinition> getSlotDefinitions(final TypeString typeString) {
    return this.findExemplarDefinitions(typeString).stream()
        .flatMap(exemplarDefinition -> exemplarDefinition.getSlots().stream())
        .collect(Collectors.toSet());
  }

  private Collection<TypeString> getParents(final ITypeStringDefinition definition) {
    if (definition instanceof ExemplarDefinition exemplarDefinition) {
      return exemplarDefinition.getParents();
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
   * @param typeString {@link TypeString} to get parents from.
   * @return Parents of the given type.
   */
  public Collection<TypeString> getParents(final TypeString typeString) {
    // TODO: This can be multiple.
    final ExemplarDefinition exemplarDefinition = this.getExemplarDefinition(typeString);
    if (exemplarDefinition == null) {
      return Collections.emptyList();
    }

    final List<TypeString> parents = exemplarDefinition.getParents();
    final Set<TypeString> implicitParents = new HashSet<>();
    if (parents.isEmpty()) {
      if (exemplarDefinition.getSort() == ExemplarDefinition.Sort.INDEXED) {
        implicitParents.add(TypeString.SW_INDEXED_FORMAT_MIXIN);
      } else if (exemplarDefinition.getSort() == ExemplarDefinition.Sort.SLOTTED) {
        implicitParents.add(TypeString.SW_SLOTTED_FORMAT_MIXIN);
      }
    }

    final TypeString[] thisGenDefs = typeString.getGenerics().toArray(TypeString[]::new);
    return Stream.concat(parents.stream(), implicitParents.stream())
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
    this.getAllAncestors(typeString, ancestors);
    return ancestors;
  }

  private void getAllAncestors(final TypeString typeString, final List<TypeString> ancestors) {
    // TODO: Does this skip one level at a time?
    final Collection<TypeString> typeStringParents = this.getParents(typeString);
    ancestors.addAll(typeStringParents);

    // Recurse.
    this.resolve(typeString).stream()
        .filter(ExemplarDefinition.class::isInstance)
        .map(ExemplarDefinition.class::cast)
        .flatMap(def -> def.getParents().stream())
        .forEach(parentTypeStr -> this.getAllAncestors(parentTypeStr, ancestors));
  }

  public Collection<TypeString> getSelfAndAncestors(final TypeString typeString) {
    return Stream.concat(Stream.of(typeString), this.getAllAncestors(typeString).stream())
        .collect(Collectors.toUnmodifiableSet());
  }
}
