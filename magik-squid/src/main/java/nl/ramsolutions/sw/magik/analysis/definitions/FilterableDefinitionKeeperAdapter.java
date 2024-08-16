package nl.ramsolutions.sw.magik.analysis.definitions;

import java.net.URI;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.definitions.ModuleDefinition;
import nl.ramsolutions.sw.definitions.ProductDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;

/** DefinitionKeeper which can filter definitions using predicates. */
public class FilterableDefinitionKeeperAdapter implements IDefinitionKeeper {

  final IDefinitionKeeper definitionKeeper;
  final Predicate<ProductDefinition> productDefinitionPredicate;
  final Predicate<ModuleDefinition> moduleDefinitionPredicate;
  final Predicate<MagikFileDefinition> magikFileDefinitionPredicate;
  final Predicate<PackageDefinition> packageDefinitionPredicate;
  final Predicate<ExemplarDefinition> exemplarDefinitionPredicate;
  final Predicate<MethodDefinition> methodDefinitionPredicate;
  final Predicate<GlobalDefinition> globalDefinitionPredicate;
  final Predicate<BinaryOperatorDefinition> binaryOperatorDefinitionPredicate;
  final Predicate<ConditionDefinition> conditionDefinitionPredicate;
  final Predicate<ProcedureDefinition> procedureDefinitionPredicate;

  @SuppressWarnings({"checkstyle:ParameterNumber", "java:S107"})
  public FilterableDefinitionKeeperAdapter(
      final IDefinitionKeeper definitionKeeper,
      final Predicate<ProductDefinition> productDefinitionPredicate,
      final Predicate<ModuleDefinition> moduleDefinitionPredicate,
      final Predicate<MagikFileDefinition> magikFileDefinitionPredicate,
      final Predicate<PackageDefinition> packageDefinitionPredicate,
      final Predicate<ExemplarDefinition> exemplarDefinitionPredicate,
      final Predicate<MethodDefinition> methodDefinitionPredicate,
      final Predicate<GlobalDefinition> globalDefinitionPredicate,
      final Predicate<BinaryOperatorDefinition> binaryOperatorDefinitionPredicate,
      final Predicate<ConditionDefinition> conditionDefinitionPredicate,
      final Predicate<ProcedureDefinition> procedureDefinitionPredicate) {
    this.definitionKeeper = definitionKeeper;
    this.productDefinitionPredicate = productDefinitionPredicate;
    this.moduleDefinitionPredicate = moduleDefinitionPredicate;
    this.magikFileDefinitionPredicate = magikFileDefinitionPredicate;
    this.packageDefinitionPredicate = packageDefinitionPredicate;
    this.exemplarDefinitionPredicate = exemplarDefinitionPredicate;
    this.methodDefinitionPredicate = methodDefinitionPredicate;
    this.globalDefinitionPredicate = globalDefinitionPredicate;
    this.binaryOperatorDefinitionPredicate = binaryOperatorDefinitionPredicate;
    this.conditionDefinitionPredicate = conditionDefinitionPredicate;
    this.procedureDefinitionPredicate = procedureDefinitionPredicate;
  }

  @Override
  public void add(final ProductDefinition definition) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void add(final ModuleDefinition definition) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void add(final MagikFileDefinition definition) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void add(final PackageDefinition definition) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void add(final ExemplarDefinition definition) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void add(final MethodDefinition definition) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void add(final GlobalDefinition definition) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void add(final BinaryOperatorDefinition definition) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void add(final ConditionDefinition definition) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void add(final ProcedureDefinition definition) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void remove(final ProductDefinition definition) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void remove(final ModuleDefinition definition) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void remove(final MagikFileDefinition definition) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void remove(final PackageDefinition definition) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void remove(final ExemplarDefinition definition) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void remove(final MethodDefinition definition) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void remove(final GlobalDefinition definition) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void remove(final BinaryOperatorDefinition definition) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void remove(final ConditionDefinition definition) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void remove(final ProcedureDefinition definition) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<ProductDefinition> getProductDefinitions(final String name) {
    return this.getProductDefinitions().stream()
        .filter(productDef -> productDef.getName().equals(name))
        .collect(Collectors.toSet());
  }

  @Override
  public Collection<ProductDefinition> getProductDefinitions() {
    return this.definitionKeeper.getProductDefinitions().stream()
        .filter(this.productDefinitionPredicate)
        .collect(Collectors.toSet());
  }

  @Override
  public Collection<ModuleDefinition> getModuleDefinitions(final String name) {
    return this.getModuleDefinitions().stream()
        .filter(moduleDef -> moduleDef.getName().equals(name))
        .collect(Collectors.toSet());
  }

  @Override
  public Collection<ModuleDefinition> getModuleDefinitions() {
    return this.definitionKeeper.getModuleDefinitions().stream()
        .filter(this.moduleDefinitionPredicate)
        .collect(Collectors.toSet());
  }

  @Override
  public Collection<MagikFileDefinition> getMagikFileDefinitions(URI uri) {
    return this.getMagikFileDefinitions().stream()
        .filter(magikFileDef -> magikFileDef.getUri().equals(uri))
        .collect(Collectors.toSet());
  }

  @Override
  public Collection<MagikFileDefinition> getMagikFileDefinitions() {
    return this.definitionKeeper.getMagikFileDefinitions().stream()
        .filter(this.magikFileDefinitionPredicate)
        .collect(Collectors.toSet());
  }

  @Override
  public Collection<PackageDefinition> getPackageDefinitions(final String name) {
    return this.getPackageDefinitions().stream()
        .filter(packageDef -> packageDef.getName().equals(name))
        .collect(Collectors.toSet());
  }

  @Override
  public Collection<PackageDefinition> getPackageDefinitions() {
    return this.definitionKeeper.getPackageDefinitions().stream()
        .filter(this.packageDefinitionPredicate)
        .collect(Collectors.toSet());
  }

  @Override
  public Collection<ExemplarDefinition> getExemplarDefinitions(final TypeString typeName) {
    return this.getExemplarDefinitions().stream()
        .filter(exemplarDef -> exemplarDef.getTypeString().equals(typeName))
        .collect(Collectors.toSet());
  }

  @Override
  public Collection<ExemplarDefinition> getExemplarDefinitions() {
    return this.definitionKeeper.getExemplarDefinitions().stream()
        .filter(this.exemplarDefinitionPredicate)
        .collect(Collectors.toSet());
  }

  @Override
  public Collection<MethodDefinition> getMethodDefinitions(final TypeString typeName) {
    return this.getMethodDefinitions().stream()
        .filter(methodDef -> methodDef.getTypeName().equals(typeName))
        .collect(Collectors.toSet());
  }

  @Override
  public Collection<MethodDefinition> getMethodDefinitions() {
    return this.definitionKeeper.getMethodDefinitions().stream()
        .filter(this.methodDefinitionPredicate)
        .collect(Collectors.toSet());
  }

  @Override
  public Collection<GlobalDefinition> getGlobalDefinitions(final TypeString typeName) {
    return this.getGlobalDefinitions().stream()
        .filter(globalDef -> globalDef.getTypeString().equals(typeName))
        .collect(Collectors.toSet());
  }

  @Override
  public Collection<GlobalDefinition> getGlobalDefinitions() {
    return this.definitionKeeper.getGlobalDefinitions().stream()
        .filter(this.globalDefinitionPredicate)
        .collect(Collectors.toSet());
  }

  @Override
  public Collection<BinaryOperatorDefinition> getBinaryOperatorDefinitions(
      final String operator, final TypeString lhs, final TypeString rhs) {
    return this.getBinaryOperatorDefinitions().stream()
        .filter(
            binaryOperatorDef ->
                binaryOperatorDef.getOperator().equals(operator)
                    && binaryOperatorDef.getLhsTypeName().equals(lhs)
                    && binaryOperatorDef.getRhsTypeName().equals(rhs))
        .collect(Collectors.toSet());
  }

  @Override
  public Collection<BinaryOperatorDefinition> getBinaryOperatorDefinitions() {
    return this.definitionKeeper.getBinaryOperatorDefinitions().stream()
        .filter(this.binaryOperatorDefinitionPredicate)
        .collect(Collectors.toSet());
  }

  @Override
  public Collection<ConditionDefinition> getConditionDefinitions(final String name) {
    return this.getConditionDefinitions().stream()
        .filter(conditionDef -> conditionDef.getName().equals(name))
        .collect(Collectors.toSet());
  }

  @Override
  public Collection<ConditionDefinition> getConditionDefinitions() {
    return this.definitionKeeper.getConditionDefinitions().stream()
        .filter(this.conditionDefinitionPredicate)
        .collect(Collectors.toSet());
  }

  @Override
  public Collection<ProcedureDefinition> getProcedureDefinitions(final TypeString typeName) {
    return this.getProcedureDefinitions().stream()
        .filter(procedureDef -> procedureDef.getTypeString().equals(typeName))
        .collect(Collectors.toSet());
  }

  @Override
  public Collection<ProcedureDefinition> getProcedureDefinitions() {
    return this.definitionKeeper.getProcedureDefinitions().stream()
        .filter(this.procedureDefinitionPredicate)
        .collect(Collectors.toSet());
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }
}
