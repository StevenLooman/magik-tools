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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.analysis.definitions.Definition;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.PackageDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ProcedureDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.SlotDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.TypeStringDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;

/** {@link TypeString} resolver tools. */
public class TypeStringResolver {

  private final IDefinitionKeeper definitionKeeper;

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
   * Get the {@link TypeStringDefinition} for the given {@link TypeString}, following package uses.
   *
   * @param typeString Reference to look for.
   * @return A {@link ExemplarDefinition}/{@link ProcedureDefinition}/{@link GlobalDefinition}.
   */
  public Collection<TypeStringDefinition> resolve(final TypeString typeString) {
    final Collection<ExemplarDefinition> exemplarDefinitions =
        this.findExemplarDefinitions(typeString);
    final Collection<ProcedureDefinition> procedureDefinitions =
        this.findProcedureDefinitions(typeString);
    final Collection<GlobalDefinition> globalDefinitions = this.findGlobalDefinitions(typeString);
    return Stream.of(
            exemplarDefinitions.stream(), procedureDefinitions.stream(), globalDefinitions.stream())
        .flatMap(definition -> definition)
        .collect(Collectors.toSet());
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
    Collection<TypeStringDefinition> definitions = this.resolve(typeString);
    if (definitions.isEmpty()) {
      return null;
    }

    // Resolve global first.
    TypeStringDefinition definition = definitions.iterator().next();
    while (definition instanceof GlobalDefinition globalDefinition0) {
      final TypeString aliasedTypeString = globalDefinition0.getAliasedTypeName();
      definitions = this.resolve(aliasedTypeString);
      if (definitions.isEmpty()) {
        return null;
      }

      definition = definitions.iterator().next();
    }

    // Treat a procedure definition as the exemplar `procedure`.
    if (definition instanceof ProcedureDefinition) {
      definitions = this.resolve(TypeString.SW_PROCEDURE);
      if (definitions.isEmpty()) {
        return null;
      }

      definition = definitions.iterator().next();
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
    final TypeStringDefinition definition1 =
        this.resolve(typeString1).stream().findAny().orElse(null);
    if (definition1 == null) {
      return false;
    }

    final TypeStringDefinition definition2 =
        this.resolve(typeString2).stream().findAny().orElse(null);
    if (definition2 == null) {
      return false;
    }

    return this.isKindOf(definition1, definition2);
  }

  /**
   * Test if {@link definition1} is kind of {@link definition2}.
   *
   * @param definition1 The thing to test.
   * @param definition2 The kind to test for.
   * @return True if is kind of, false otherwise.
   */
  public boolean isKindOf(
      final TypeStringDefinition definition1, final TypeStringDefinition definition2) {
    final TypeString typeString1 = definition1.getTypeString();
    final TypeString typeString2 = definition2.getTypeString();
    if (typeString1.equals(typeString2)) {
      return true;
    }

    return this.getParents(definition1).stream()
        .anyMatch(parentTypeString1 -> this.isKindOf(parentTypeString1, typeString2));
  }

  private Collection<TypeString> getParents(final Definition definition) {
    if (definition instanceof ExemplarDefinition exemplarDefinition) {
      return exemplarDefinition.getParents();
    } else if (definition instanceof ProcedureDefinition) {
      return Set.of(TypeString.SW_PROCEDURE);
    } else if (definition instanceof GlobalDefinition globalDefinition) {
      final TypeString typeString = globalDefinition.getAliasedTypeName();
      final Definition aliasedDefinition = this.resolve(typeString).stream().findAny().orElse(null);
      if (aliasedDefinition == null) {
        return Collections.emptySet();
      }

      return this.getParents(aliasedDefinition);
    }

    throw new IllegalStateException();
  }

  /**
   * Get the {@link MethodDefinition}s the {@link TypeString} responds to, including from its super
   * types.
   *
   * @param typeString {@link TypeString} to resolve.
   * @return {@link MethodDefinition}s the {@link TypeString} responds to.
   */
  public Collection<MethodDefinition> getMethodDefinitions(final TypeString typeString) {
    // Try to resolve the typeString to an actual type.
    Collection<TypeStringDefinition> resolvedTypes = this.resolve(typeString);
    final TypeString actualTypeStr =
        resolvedTypes.isEmpty() ? typeString : resolvedTypes.iterator().next().getTypeString();

    final Map<String, Set<MethodDefinition>> methodDefinitions = new HashMap<>();
    this.getMethodDefinitions(actualTypeStr, methodDefinitions);
    return methodDefinitions.values().stream().flatMap(Set::stream).collect(Collectors.toSet());
  }

  private void getMethodDefinitions(
      final TypeString typeString, final Map<String, Set<MethodDefinition>> methodDefinitions) {
    Stream.concat(Stream.of(typeString), this.getAllParents(typeString).stream())
        .forEach(
            typeStr -> {
              this.definitionKeeper
                  .getMethodDefinitions(typeString)
                  .forEach(
                      methodDefinition -> {
                        final String methodName = methodDefinition.getMethodName();
                        // TODO: If already present, then skip? Filter duplicates with the same
                        // name, we're trying to emulate responding to specific methods.
                        final Set<MethodDefinition> methodsForName =
                            methodDefinitions.computeIfAbsent(methodName, key -> new HashSet<>());
                        methodsForName.add(methodDefinition);
                      });
            });
  }

  public Collection<MethodDefinition> getMethodDefinitions(
      final TypeString typeString, final String methodName) {
    return this.getMethodDefinitions(typeString).stream()
        .filter(methodDef -> methodDef.getMethodName().equalsIgnoreCase(methodName))
        .toList();
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

  /**
   * Get the parents of a {@link TypeString}.
   *
   * <p>This add the implicit parents, where {@link ExemplarDefinition} only returns its explicitly
   * defined parents.
   *
   * @param typeString
   * @return
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
                TypeString.ofIdentifier(typeStr.getIdentifier(), typeStr.getPakkage(), thisGenDefs))
        .collect(Collectors.toUnmodifiableSet());
  }

  /**
   * Find all parents (recursively) for a given {@link TypeString}.
   *
   * @param typeString {@link TypeString} to get parents from.
   * @return All parents this the given type.
   */
  public Collection<TypeString> getAllParents(final TypeString typeString) {
    final List<TypeString> parents = new ArrayList<>();
    this.getAllParents(typeString, parents);
    return parents;
  }

  private void getAllParents(final TypeString typeString, final List<TypeString> parents) {
    final Collection<TypeString> typeStringParents = this.getParents(typeString);
    parents.addAll(typeStringParents);

    // Recurse.
    // TODO: This can be multiple.
    final ExemplarDefinition exemplarDefinition = this.getExemplarDefinition(typeString);
    if (exemplarDefinition == null) {
      return;
    }

    exemplarDefinition
        .getParents()
        .forEach(parentTypeStr -> this.getAllParents(parentTypeStr, parents));
  }
}
