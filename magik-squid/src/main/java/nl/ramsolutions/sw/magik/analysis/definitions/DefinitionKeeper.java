package nl.ramsolutions.sw.magik.analysis.definitions;

import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.IDefinition;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.moduledef.ModuleDefinition;
import nl.ramsolutions.sw.productdef.ProductDefinition;

/** In memory Definition keeper. */
public class DefinitionKeeper implements IDefinitionKeeper {

  private final Map<String, Set<ProductDefinition>> productDefinitions = new ConcurrentHashMap<>();
  private final Map<String, Set<ModuleDefinition>> moduleDefinitions = new ConcurrentHashMap<>();
  private final Map<URI, Set<MagikFileDefinition>> magikFileDefinitions = new ConcurrentHashMap<>();
  private final Map<String, Set<PackageDefinition>> packageDefinitions = new ConcurrentHashMap<>();
  private final Map<String, Set<BinaryOperatorDefinition>> binaryOperatorDefinitions =
      new ConcurrentHashMap<>();
  private final Map<String, Set<ConditionDefinition>> conditionDefinitions =
      new ConcurrentHashMap<>();
  private final Map<TypeString, Set<ExemplarDefinition>> exemplarDefinitions =
      new ConcurrentHashMap<>();
  private final Map<TypeString, Set<MethodDefinition>> methodDefinitions =
      new ConcurrentHashMap<>();
  private final Map<TypeString, Set<GlobalDefinition>> globalDefinitions =
      new ConcurrentHashMap<>();
  private final Map<TypeString, Set<ProcedureDefinition>> procedureDefinitions =
      new ConcurrentHashMap<>();
  private final SortedMap<URI, Set<IDefinition>> uriDefinitions =
      Collections.synchronizedSortedMap(new TreeMap<>());

  /** Constructor. */
  public DefinitionKeeper() {
    this(true);
  }

  /**
   * Constructor to allow adding default types or not.
   *
   * @param addDefaultTypes Do add default types?
   */
  public DefinitionKeeper(final boolean addDefaultTypes) {
    this.clear();

    DefaultDefinitionsAdder.addBaseDefinitions(this);
    if (addDefaultTypes) {
      DefaultDefinitionsAdder.addDefaultDefinitions(this);
    }
  }

  @Override
  public void add(final ProductDefinition definition) {
    final String name = definition.getName();
    final Set<ProductDefinition> definitions =
        this.productDefinitions.computeIfAbsent(name, k -> ConcurrentHashMap.newKeySet());
    definitions.add(definition);

    this.addToPathIndex(definition);
  }

  @Override
  public void add(final ModuleDefinition definition) {
    final String name = definition.getName();
    final Set<ModuleDefinition> definitions =
        this.moduleDefinitions.computeIfAbsent(name, k -> ConcurrentHashMap.newKeySet());
    definitions.add(definition);

    this.addToPathIndex(definition);
  }

  @Override
  public void add(final MagikFileDefinition definition) {
    final URI uri = definition.getUri();
    final Set<MagikFileDefinition> definitions =
        this.magikFileDefinitions.computeIfAbsent(uri, k -> ConcurrentHashMap.newKeySet());
    definitions.add(definition);

    this.addToPathIndex(definition);
  }

  @Override
  public void add(final PackageDefinition definition) {
    final String name = definition.getName();
    final Set<PackageDefinition> definitions =
        this.packageDefinitions.computeIfAbsent(name, k -> ConcurrentHashMap.newKeySet());
    definitions.add(definition);

    this.addToPathIndex(definition);
  }

  @Override
  public void add(final ExemplarDefinition definition) {
    // Store without generics.
    final TypeString bareTypeString = definition.getTypeString().getWithoutGenerics();
    final Set<ExemplarDefinition> definitions =
        this.exemplarDefinitions.computeIfAbsent(
            bareTypeString, k -> ConcurrentHashMap.newKeySet());
    definitions.add(definition);

    this.addToPathIndex(definition);
  }

  @Override
  public void add(final MethodDefinition definition) {
    final TypeString bareTypeString = definition.getTypeName().getWithoutGenerics();
    final Set<MethodDefinition> definitions =
        this.methodDefinitions.computeIfAbsent(bareTypeString, k -> ConcurrentHashMap.newKeySet());
    definitions.add(definition);

    this.addToPathIndex(definition);
  }

  @Override
  public void add(final GlobalDefinition definition) {
    final TypeString bareTypeString = definition.getTypeString().getWithoutGenerics();
    final Set<GlobalDefinition> definitions =
        this.globalDefinitions.computeIfAbsent(bareTypeString, k -> ConcurrentHashMap.newKeySet());
    definitions.add(definition);

    this.addToPathIndex(definition);
  }

  @Override
  public void add(final BinaryOperatorDefinition definition) {
    final String key =
        this.getKey(
            definition.getOperator(), definition.getLhsTypeName(), definition.getRhsTypeName());
    final Set<BinaryOperatorDefinition> definitions =
        this.binaryOperatorDefinitions.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet());
    definitions.add(definition);

    this.addToPathIndex(definition);
  }

  @Override
  public void add(final ConditionDefinition definition) {
    final String name = definition.getName();
    final Set<ConditionDefinition> definitions =
        this.conditionDefinitions.computeIfAbsent(name, k -> ConcurrentHashMap.newKeySet());
    definitions.add(definition);

    this.addToPathIndex(definition);
  }

  @Override
  public void add(final ProcedureDefinition definition) {
    // TODO: Should these always be aliases via a GlobalDefinition? Probably so!
    final TypeString bareTypeString = definition.getTypeString().getWithoutGenerics();
    final Set<ProcedureDefinition> definitions =
        this.procedureDefinitions.computeIfAbsent(
            bareTypeString, k -> ConcurrentHashMap.newKeySet());
    definitions.add(definition);

    this.addToPathIndex(definition);
  }

  @Override
  public void add(final IDefinition definition) {
    if (definition instanceof final ProductDefinition productDefinition) {
      this.add(productDefinition);
    } else if (definition instanceof final ModuleDefinition moduleDefinition) {
      this.add(moduleDefinition);
    } else if (definition instanceof final MagikFileDefinition magikFileDefinition) {
      this.add(magikFileDefinition);
    } else if (definition instanceof final PackageDefinition packageDefinition) {
      this.add(packageDefinition);
    } else if (definition instanceof final ExemplarDefinition exemplarDefinition) {
      this.add(exemplarDefinition);
    } else if (definition instanceof final MethodDefinition methodDefinition) {
      this.add(methodDefinition);
    } else if (definition instanceof final GlobalDefinition globalDefinition) {
      this.add(globalDefinition);
    } else if (definition instanceof final BinaryOperatorDefinition binaryOperatorDefinition) {
      this.add(binaryOperatorDefinition);
    } else if (definition instanceof final ConditionDefinition conditionDefinition) {
      this.add(conditionDefinition);
    } else if (definition instanceof final ProcedureDefinition procedureDefinition) {
      this.add(procedureDefinition);
    } else {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public void remove(final ProductDefinition definition) {
    final String name = definition.getName();
    final Set<ProductDefinition> definitions =
        this.productDefinitions.computeIfAbsent(name, k -> ConcurrentHashMap.newKeySet());
    definitions.remove(definition);

    this.removeFromPathIndex(definition);
  }

  @Override
  public void remove(final ModuleDefinition definition) {
    final String name = definition.getName();
    final Set<ModuleDefinition> definitions =
        this.moduleDefinitions.computeIfAbsent(name, k -> ConcurrentHashMap.newKeySet());
    definitions.remove(definition);

    this.removeFromPathIndex(definition);
  }

  @Override
  public void remove(final MagikFileDefinition definition) {
    final URI uri = definition.getUri();
    final Set<MagikFileDefinition> definitions =
        this.magikFileDefinitions.computeIfAbsent(uri, k -> ConcurrentHashMap.newKeySet());
    definitions.remove(definition);

    this.removeFromPathIndex(definition);
  }

  @Override
  public void remove(final PackageDefinition definition) {
    final String name = definition.getName();
    final Set<PackageDefinition> definitions =
        this.packageDefinitions.computeIfAbsent(name, k -> ConcurrentHashMap.newKeySet());
    definitions.remove(definition);

    this.removeFromPathIndex(definition);
  }

  @Override
  public void remove(ExemplarDefinition definition) {
    final TypeString bareTypeString = definition.getTypeString().getWithoutGenerics();
    final Set<ExemplarDefinition> definitions =
        this.exemplarDefinitions.computeIfAbsent(
            bareTypeString, k -> ConcurrentHashMap.newKeySet());
    definitions.remove(definition);

    this.removeFromPathIndex(definition);
  }

  @Override
  public void remove(final MethodDefinition definition) {
    final TypeString bareTypeString = definition.getTypeName().getWithoutGenerics();
    final Set<MethodDefinition> definitions =
        this.methodDefinitions.computeIfAbsent(bareTypeString, k -> ConcurrentHashMap.newKeySet());
    definitions.remove(definition);

    this.removeFromPathIndex(definition);
  }

  @Override
  public void remove(final GlobalDefinition definition) {
    final TypeString bareTypeString = definition.getTypeString().getWithoutGenerics();
    final Set<GlobalDefinition> definitions =
        this.globalDefinitions.computeIfAbsent(bareTypeString, k -> ConcurrentHashMap.newKeySet());
    definitions.remove(definition);

    this.removeFromPathIndex(definition);
  }

  @Override
  public void remove(final BinaryOperatorDefinition definition) {
    final String key =
        this.getKey(
            definition.getOperator(), definition.getLhsTypeName(), definition.getRhsTypeName());
    final Set<BinaryOperatorDefinition> definitions =
        this.binaryOperatorDefinitions.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet());
    definitions.remove(definition);

    this.removeFromPathIndex(definition);
  }

  @Override
  public void remove(final ConditionDefinition definition) {
    final String name = definition.getName();
    final Set<ConditionDefinition> definitions =
        this.conditionDefinitions.computeIfAbsent(name, k -> ConcurrentHashMap.newKeySet());
    definitions.remove(definition);

    this.removeFromPathIndex(definition);
  }

  @Override
  public void remove(final ProcedureDefinition definition) {
    final TypeString bareTypeString = definition.getTypeString().getWithoutGenerics();
    final Set<ProcedureDefinition> definitions =
        this.procedureDefinitions.computeIfAbsent(
            bareTypeString, k -> ConcurrentHashMap.newKeySet());
    definitions.remove(definition);

    this.removeFromPathIndex(definition);
  }

  @Override
  public void remove(final IDefinition definition) {
    if (definition instanceof final ProductDefinition productDefinition) {
      this.remove(productDefinition);
    } else if (definition instanceof final ModuleDefinition moduleDefinition) {
      this.remove(moduleDefinition);
    } else if (definition instanceof final MagikFileDefinition magikFileDefinition) {
      this.remove(magikFileDefinition);
    } else if (definition instanceof final PackageDefinition packageDefinition) {
      this.remove(packageDefinition);
    } else if (definition instanceof final ExemplarDefinition exemplarDefinition) {
      this.remove(exemplarDefinition);
    } else if (definition instanceof final MethodDefinition methodDefinition) {
      this.remove(methodDefinition);
    } else if (definition instanceof final GlobalDefinition globalDefinition) {
      this.remove(globalDefinition);
    } else if (definition instanceof final BinaryOperatorDefinition binaryOperatorDefinition) {
      this.remove(binaryOperatorDefinition);
    } else if (definition instanceof final ConditionDefinition conditionDefinition) {
      this.remove(conditionDefinition);
    } else if (definition instanceof final ProcedureDefinition procedureDefinition) {
      this.remove(procedureDefinition);
    } else {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public Collection<ProductDefinition> getProductDefinitions(final String name) {
    final Collection<ProductDefinition> definitions =
        this.productDefinitions.getOrDefault(name, Collections.emptySet());
    return Collections.unmodifiableCollection(definitions);
  }

  @Override
  public Collection<ProductDefinition> getProductDefinitions() {
    return this.productDefinitions.values().stream()
        .flatMap(Set::stream)
        .collect(Collectors.toSet());
  }

  @Override
  public Collection<ModuleDefinition> getModuleDefinitions(final String name) {
    final Collection<ModuleDefinition> definitions =
        this.moduleDefinitions.getOrDefault(name, Collections.emptySet());
    return Collections.unmodifiableCollection(definitions);
  }

  @Override
  public Collection<ModuleDefinition> getModuleDefinitions() {
    return this.moduleDefinitions.values().stream()
        .flatMap(Set::stream)
        .collect(Collectors.toSet());
  }

  @Override
  public Collection<MagikFileDefinition> getMagikFileDefinitions(final URI uri) {
    final Collection<MagikFileDefinition> definitions =
        this.magikFileDefinitions.getOrDefault(uri, Collections.emptySet());
    return Collections.unmodifiableCollection(definitions);
  }

  @Override
  public Collection<MagikFileDefinition> getMagikFileDefinitions() {
    return this.magikFileDefinitions.values().stream()
        .flatMap(Set::stream)
        .collect(Collectors.toSet());
  }

  @Override
  public Collection<PackageDefinition> getPackageDefinitions(final String name) {
    final Collection<PackageDefinition> definitions =
        this.packageDefinitions.getOrDefault(name, Collections.emptySet());
    return Collections.unmodifiableCollection(definitions);
  }

  @Override
  public Collection<PackageDefinition> getPackageDefinitions() {
    return this.packageDefinitions.values().stream()
        .flatMap(Set::stream)
        .collect(Collectors.toSet());
  }

  @Override
  public Collection<ExemplarDefinition> getExemplarDefinitions(final TypeString typeString) {
    // Get without generics.
    final TypeString bareTypeString = typeString.getWithoutGenerics();
    final Collection<ExemplarDefinition> definitions =
        this.exemplarDefinitions.getOrDefault(bareTypeString, Collections.emptySet());
    return Collections.unmodifiableCollection(definitions);
  }

  @Override
  public Collection<ExemplarDefinition> getExemplarDefinitions() {
    return this.exemplarDefinitions.values().stream()
        .flatMap(Set::stream)
        .collect(Collectors.toSet());
  }

  @Override
  public Collection<MethodDefinition> getMethodDefinitions(final TypeString typeString) {
    final TypeString bareTypeString = typeString.getWithoutGenerics();
    final Collection<MethodDefinition> definitions =
        this.methodDefinitions.getOrDefault(bareTypeString, Collections.emptySet());
    return Collections.unmodifiableCollection(definitions);
  }

  @Override
  public Collection<MethodDefinition> getMethodDefinitions() {
    return this.methodDefinitions.values().stream()
        .flatMap(Set::stream)
        .collect(Collectors.toSet());
  }

  @Override
  public Collection<GlobalDefinition> getGlobalDefinitions(final TypeString typeString) {
    final Collection<GlobalDefinition> definitions =
        this.globalDefinitions.getOrDefault(typeString, Collections.emptySet());
    return Collections.unmodifiableCollection(definitions);
  }

  @Override
  public Collection<GlobalDefinition> getGlobalDefinitions() {
    return this.globalDefinitions.values().stream()
        .flatMap(Set::stream)
        .collect(Collectors.toSet());
  }

  private String getKey(final String operator, final TypeString lhs, final TypeString rhs) {
    return operator
        + "_"
        + lhs.getWithoutGenerics().getFullString()
        + "_"
        + rhs.getWithoutGenerics().getFullString();
  }

  @Override
  public Collection<BinaryOperatorDefinition> getBinaryOperatorDefinitions(
      final String operator, final TypeString lhs, final TypeString rhs) {
    final String key = this.getKey(operator, lhs, rhs);
    final Collection<BinaryOperatorDefinition> definitions =
        this.binaryOperatorDefinitions.getOrDefault(key, Collections.emptySet());
    return Collections.unmodifiableCollection(definitions);
  }

  @Override
  public Collection<BinaryOperatorDefinition> getBinaryOperatorDefinitions() {
    return this.binaryOperatorDefinitions.values().stream()
        .flatMap(Set::stream)
        .collect(Collectors.toSet());
  }

  @Override
  public Collection<ConditionDefinition> getConditionDefinitions(final String name) {
    final Collection<ConditionDefinition> definitions =
        this.conditionDefinitions.getOrDefault(name, Collections.emptySet());
    return Collections.unmodifiableCollection(definitions);
  }

  @Override
  public Collection<ConditionDefinition> getConditionDefinitions() {
    return this.conditionDefinitions.values().stream()
        .flatMap(Set::stream)
        .collect(Collectors.toSet());
  }

  @Override
  public Collection<ProcedureDefinition> getProcedureDefinitions(final TypeString typeString) {
    final TypeString bareTypeString = typeString.getWithoutGenerics();
    final Collection<ProcedureDefinition> definitions =
        this.procedureDefinitions.getOrDefault(bareTypeString, Collections.emptySet());
    return Collections.unmodifiableCollection(definitions);
  }

  @Override
  public Collection<ProcedureDefinition> getProcedureDefinitions() {
    return this.procedureDefinitions.values().stream()
        .flatMap(Set::stream)
        .collect(Collectors.toSet());
  }

  @Override
  public Collection<IDefinition> getDefinitionsByPath(final Path path) {
    final URI uri = path.toUri();
    final String uriStr = uri.toString();
    final Set<IDefinition> allDefinitions = new HashSet<>();
    for (final Map.Entry<URI, Set<IDefinition>> entry :
        this.uriDefinitions.tailMap(uri).entrySet()) {
      final URI entryUri = entry.getKey();
      if (!entryUri.toString().startsWith(uriStr)) {
        break;
      }

      final Set<IDefinition> entryDefinitions = entry.getValue();
      allDefinitions.addAll(entryDefinitions);
    }
    return Collections.unmodifiableCollection(allDefinitions);
  }

  private void addToPathIndex(final IDefinition definition) {
    final Location location = definition.getLocation();
    if (location == null) {
      return;
    }

    final URI uri = location.getUri();
    final Set<IDefinition> definitions =
        this.uriDefinitions.computeIfAbsent(uri, k -> new HashSet<>());
    definitions.add(definition);
  }

  private void removeFromPathIndex(final IDefinition definition) {
    final Location location = definition.getLocation();
    if (location == null) {
      return;
    }

    final URI uri = location.getUri();
    if (!this.uriDefinitions.containsKey(uri)) {
      return;
    }

    final Set<IDefinition> definitions = this.uriDefinitions.get(uri);
    definitions.remove(definition);

    if (definitions.isEmpty()) {
      this.uriDefinitions.remove(uri);
    }
  }

  /** Clear any contained {@link MagikDefinition}s. */
  @Override
  public void clear() {
    this.productDefinitions.clear();
    this.moduleDefinitions.clear();
    this.packageDefinitions.clear();
    this.binaryOperatorDefinitions.clear();
    this.conditionDefinitions.clear();
    this.exemplarDefinitions.clear();
    this.methodDefinitions.clear();
    this.globalDefinitions.clear();
    this.procedureDefinitions.clear();
  }
}
