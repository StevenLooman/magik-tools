package nl.ramsolutions.sw.magik.analysis.definitions;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.definitions.ModuleDefinition;
import nl.ramsolutions.sw.definitions.ProductDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;

/** In memory Definition keeper. */
public class DefinitionKeeper implements IDefinitionKeeper {

  private final Map<String, Set<ProductDefinition>> productDefinitions = new ConcurrentHashMap<>();
  private final Map<String, Set<ModuleDefinition>> moduleDefinitions = new ConcurrentHashMap<>();
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
  }

  @Override
  public void add(final ModuleDefinition definition) {
    final String name = definition.getName();
    final Set<ModuleDefinition> definitions =
        this.moduleDefinitions.computeIfAbsent(name, k -> ConcurrentHashMap.newKeySet());
    definitions.add(definition);
  }

  @Override
  public void add(final PackageDefinition definition) {
    final String name = definition.getName();
    final Set<PackageDefinition> definitions =
        this.packageDefinitions.computeIfAbsent(name, k -> ConcurrentHashMap.newKeySet());
    definitions.add(definition);
  }

  @Override
  public void add(final ExemplarDefinition definition) {
    // Store without generics.
    final TypeString bareTypeString = definition.getTypeString().getWithoutGenerics();
    final Set<ExemplarDefinition> definitions =
        this.exemplarDefinitions.computeIfAbsent(
            bareTypeString, k -> ConcurrentHashMap.newKeySet());
    definitions.add(definition);
  }

  @Override
  public void add(final MethodDefinition definition) {
    final TypeString bareTypeString = definition.getTypeName().getWithoutGenerics();
    final Set<MethodDefinition> definitions =
        this.methodDefinitions.computeIfAbsent(bareTypeString, k -> ConcurrentHashMap.newKeySet());
    definitions.add(definition);
  }

  @Override
  public void add(final GlobalDefinition definition) {
    final TypeString bareTypeString = definition.getTypeString().getWithoutGenerics();
    final Set<GlobalDefinition> definitions =
        this.globalDefinitions.computeIfAbsent(bareTypeString, k -> ConcurrentHashMap.newKeySet());
    definitions.add(definition);
  }

  @Override
  public void add(final BinaryOperatorDefinition definition) {
    final String key = this.getKey(definition);
    final Set<BinaryOperatorDefinition> definitions =
        this.binaryOperatorDefinitions.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet());
    definitions.add(definition);
  }

  @Override
  public void add(final ConditionDefinition definition) {
    final String name = definition.getName();
    final Set<ConditionDefinition> definitions =
        this.conditionDefinitions.computeIfAbsent(name, k -> ConcurrentHashMap.newKeySet());
    definitions.add(definition);
  }

  @Override
  public void add(final ProcedureDefinition definition) {
    final TypeString bareTypeString = definition.getTypeString().getWithoutGenerics();
    final Set<ProcedureDefinition> definitions =
        this.procedureDefinitions.computeIfAbsent(
            bareTypeString, k -> ConcurrentHashMap.newKeySet());
    definitions.add(definition);
  }

  @Override
  public void remove(final ProductDefinition definition) {
    final String name = definition.getName();
    final Set<ProductDefinition> definitions =
        this.productDefinitions.computeIfAbsent(name, k -> ConcurrentHashMap.newKeySet());
    definitions.remove(definition);
  }

  @Override
  public void remove(final ModuleDefinition definition) {
    final String name = definition.getName();
    final Set<ModuleDefinition> definitions =
        this.moduleDefinitions.computeIfAbsent(name, k -> ConcurrentHashMap.newKeySet());
    definitions.remove(definition);
  }

  @Override
  public void remove(final PackageDefinition definition) {
    final String name = definition.getName();
    final Set<PackageDefinition> definitions =
        this.packageDefinitions.computeIfAbsent(name, k -> ConcurrentHashMap.newKeySet());
    definitions.remove(definition);
  }

  @Override
  public void remove(ExemplarDefinition definition) {
    final TypeString bareTypeString = definition.getTypeString().getWithoutGenerics();
    final Set<ExemplarDefinition> definitions =
        this.exemplarDefinitions.computeIfAbsent(
            bareTypeString, k -> ConcurrentHashMap.newKeySet());
    definitions.remove(definition);
  }

  @Override
  public void remove(final MethodDefinition definition) {
    final TypeString bareTypeString = definition.getTypeName().getWithoutGenerics();
    final Set<MethodDefinition> definitions =
        this.methodDefinitions.computeIfAbsent(bareTypeString, k -> ConcurrentHashMap.newKeySet());
    definitions.remove(definition);
  }

  @Override
  public void remove(final GlobalDefinition definition) {
    final TypeString bareTypeString = definition.getTypeString().getWithoutGenerics();
    final Set<GlobalDefinition> definitions =
        this.globalDefinitions.computeIfAbsent(bareTypeString, k -> ConcurrentHashMap.newKeySet());
    definitions.remove(definition);
  }

  @Override
  public void remove(final BinaryOperatorDefinition definition) {
    final String key = this.getKey(definition);
    final Set<BinaryOperatorDefinition> definitions =
        this.binaryOperatorDefinitions.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet());
    definitions.remove(definition);
  }

  @Override
  public void remove(final ConditionDefinition definition) {
    final String name = definition.getName();
    final Set<ConditionDefinition> definitions =
        this.conditionDefinitions.computeIfAbsent(name, k -> ConcurrentHashMap.newKeySet());
    definitions.remove(definition);
  }

  @Override
  public void remove(final ProcedureDefinition definition) {
    final TypeString bareTypeString = definition.getTypeString().getWithoutGenerics();
    final Set<ProcedureDefinition> definitions =
        this.procedureDefinitions.computeIfAbsent(
            bareTypeString, k -> ConcurrentHashMap.newKeySet());
    definitions.remove(definition);
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
    return definitions.stream()
        .filter(def -> def.getTypeName().equals(typeString))
        .collect(Collectors.toSet());
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

  private String getKey(final BinaryOperatorDefinition definition) {
    return definition.getOperator()
        + "_"
        + definition.getLhsTypeName().getWithoutGenerics().getFullString()
        + "_"
        + definition.getRhsTypeName().getWithoutGenerics().getFullString();
  }

  @Override
  public Collection<BinaryOperatorDefinition> getBinaryOperatorDefinitions(
      final String operator, final TypeString lhs, final TypeString rhs) {
    final String key =
        operator
            + "_"
            + lhs.getWithoutGenerics().getFullString()
            + "_"
            + rhs.getWithoutGenerics().getFullString();
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

  /** Clear any contained {@link Definition}s. */
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
