package nl.ramsolutions.sw.magik.analysis.typing;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.definitions.BinaryOperatorDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ConditionDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.Definition;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.PackageDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ParameterDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ProcedureDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.TypeStringDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.AliasType;
import nl.ramsolutions.sw.magik.analysis.typing.types.Condition;
import nl.ramsolutions.sw.magik.analysis.typing.types.MagikType;
import nl.ramsolutions.sw.magik.analysis.typing.types.Method;
import nl.ramsolutions.sw.magik.analysis.typing.types.Package;
import nl.ramsolutions.sw.magik.analysis.typing.types.Parameter;
import nl.ramsolutions.sw.magik.analysis.typing.types.ProcedureInstance;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.types.UndefinedType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** {@link IDefinitionKeeper} to {@link ITypeKeeper} adapter. */
public class DefinitionKeeperTypeKeeperAdapter implements ITypeKeeper {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(DefinitionKeeperTypeKeeperAdapter.class);

  private static class BinaryOperatorKey {

    private final BinaryOperator.Operator operator;
    private final TypeString leftType;
    private final TypeString rightType;

    BinaryOperatorKey(
        final BinaryOperator.Operator operator,
        final TypeString leftType,
        final TypeString rightType) {
      this.operator = operator;
      this.leftType = leftType;
      this.rightType = rightType;
    }

    String getString() {
      return this.operator
          + "-"
          + this.leftType.getFullString()
          + "-"
          + this.rightType.getFullString();
    }

    @Override
    public int hashCode() {
      return Objects.hash(this.operator, this.leftType, this.rightType);
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }

      if (obj == null) {
        return false;
      }

      if (this.getClass() != obj.getClass()) {
        return false;
      }

      final BinaryOperatorKey other = (BinaryOperatorKey) obj;
      return Objects.equals(this.operator, other.operator)
          && Objects.equals(this.leftType, other.leftType)
          && Objects.equals(this.rightType, other.rightType);
    }
  }

  private static final Map<ExemplarDefinition.Sort, MagikType.Sort> EXEMPLAR_SORT_MAPPING =
      Map.of(
          ExemplarDefinition.Sort.UNDEFINED, MagikType.Sort.UNDEFINED,
          ExemplarDefinition.Sort.OBJECT, MagikType.Sort.OBJECT,
          ExemplarDefinition.Sort.INDEXED, MagikType.Sort.INDEXED,
          ExemplarDefinition.Sort.INTRINSIC, MagikType.Sort.INTRINSIC,
          ExemplarDefinition.Sort.SLOTTED, MagikType.Sort.SLOTTED);
  private static final Map<ParameterDefinition.Modifier, Parameter.Modifier>
      PARAMETER_MODIFIER_MAPPING =
          Map.of(
              ParameterDefinition.Modifier.NONE, Parameter.Modifier.NONE,
              ParameterDefinition.Modifier.OPTIONAL, Parameter.Modifier.OPTIONAL,
              ParameterDefinition.Modifier.GATHER, Parameter.Modifier.GATHER);
  private static final Map<MethodDefinition.Modifier, Method.Modifier> METHOD_MODIFIER_MAPPING =
      Map.of(
          MethodDefinition.Modifier.ABSTRACT, Method.Modifier.ABSTRACT,
          MethodDefinition.Modifier.ITER, Method.Modifier.ITER,
          MethodDefinition.Modifier.PRIVATE, Method.Modifier.PRIVATE);
  private static final Map<ProcedureDefinition.Modifier, ProcedureInstance.Modifier>
      PROCEDURE_MODIFIER_MAPPING =
          Map.of(ProcedureDefinition.Modifier.ITER, ProcedureInstance.Modifier.ITER);

  private final IDefinitionKeeper definitionKeeper;
  private final Map<String, Package> packageCache = new ConcurrentHashMap<>();
  private final Map<TypeString, AbstractType> typeCache = new ConcurrentHashMap<>();
  private final Map<String, Condition> conditionCache = new ConcurrentHashMap<>();
  private final Map<BinaryOperatorKey, BinaryOperator> binaryOperatorCache =
      new ConcurrentHashMap<>();

  public DefinitionKeeperTypeKeeperAdapter(final IDefinitionKeeper definitionKeeper) {
    this.definitionKeeper = definitionKeeper;
  }

  @Override
  public void addPackage(final Package pakkage) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean hasPackage(final String pakkageName) {
    final Collection<PackageDefinition> definitions =
        this.definitionKeeper.getPackageDefinitions(pakkageName);
    return !definitions.isEmpty();
  }

  @Override
  public synchronized Package getPackage(final String pakkageName) {
    if (this.packageCache.containsKey(pakkageName)) {
      return this.packageCache.get(pakkageName);
    }

    final Collection<PackageDefinition> definitions =
        this.definitionKeeper.getPackageDefinitions(pakkageName);
    if (definitions.isEmpty()) {
      // this.packageCache.put(pakkageName, null);  // TODO: ConcurrentHashMap does not support null
      // values.

      return null;
    }

    final PackageDefinition definition = definitions.stream().findAny().orElseThrow();
    final Location location = definition.getLocation();
    final String moduleName = definition.getModuleName();
    final String name = definition.getName();
    final Package pakkage = new Package(this, location, moduleName, name);
    definition.getUses().forEach(pakkage::addUse);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "{} Created package, package name: {}, package: {}",
          Integer.toHexString(this.hashCode()),
          pakkageName,
          pakkage);
    }
    this.packageCache.put(pakkageName, pakkage);
    return pakkage;
  }

  @Override
  public void removePackage(final Package pakkage) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<Package> getPackages() {
    return this.definitionKeeper.getPackageDefinitions().stream()
        .map(PackageDefinition::getName)
        .map(this::getPackage)
        .collect(Collectors.toSet());
  }

  @CheckForNull
  private TypeStringDefinition findTypeDefinition(final TypeString typeString) {
    final TypeStringResolver resolver = new TypeStringResolver(this.definitionKeeper);
    return resolver.resolve(typeString).stream().findAny().orElse(null);
  }

  @Override
  public boolean hasType(final TypeString typeString) {
    return this.findTypeDefinition(typeString) != null;
  }

  @Override
  public boolean hasTypeInPackage(final TypeString typeString) {
    return !this.definitionKeeper.getExemplarDefinitions(typeString).isEmpty()
        || !this.definitionKeeper.getProcedureDefinitions(typeString).isEmpty()
        || !this.definitionKeeper.getGlobalDefinitions(typeString).isEmpty();
  }

  @Override
  public void addType(final AbstractType type) {
    throw new UnsupportedOperationException();
  }

  @Override
  public synchronized AbstractType getType(final TypeString typeString) {
    if (this.typeCache.containsKey(typeString)) {
      return this.typeCache.get(typeString);
    }

    final Definition definition = this.findTypeDefinition(typeString);
    final AbstractType type;
    if (definition instanceof ExemplarDefinition exemplarDefinition) {
      type = this.createType(exemplarDefinition, typeString);
    } else if (definition instanceof ProcedureDefinition procedureDefinition) {
      type = this.createType(procedureDefinition);
    } else if (definition instanceof GlobalDefinition globalDefinition) {
      type = this.createType(globalDefinition);
    } else {
      type = UndefinedType.INSTANCE;
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "{} Created type, typeString: {} type: {}, method count: {}",
          Integer.toHexString(this.hashCode()),
          typeString.getFullString(),
          type,
          type.getLocalMethods().size());
    }
    this.typeCache.put(typeString, type);
    return type;
  }

  private AbstractType createType(
      final ExemplarDefinition exemplarDefinition, final TypeString searchTypeString) {
    // Construct type.
    final TypeString definitionTypeString = exemplarDefinition.getTypeString();
    final Location location = exemplarDefinition.getLocation();
    final String moduleName = exemplarDefinition.getModuleName();
    final ExemplarDefinition.Sort definitionSort = exemplarDefinition.getSort();
    final MagikType.Sort sort =
        DefinitionKeeperTypeKeeperAdapter.EXEMPLAR_SORT_MAPPING.get(definitionSort);
    final MagikType magikType =
        new MagikType(this, location, moduleName, sort, definitionTypeString);

    // Add topics.
    exemplarDefinition.getTopics().forEach(magikType::addTopic);

    // Add doc.
    final String doc = exemplarDefinition.getDoc();
    magikType.setDoc(doc);

    // Add parents.
    exemplarDefinition.getParents().stream().forEach(magikType::addParent);

    // Add slots.
    exemplarDefinition.getSlots().stream()
        .forEach(
            slotDef ->
                magikType.addSlot(slotDef.getLocation(), slotDef.getName(), slotDef.getTypeName()));

    // Add generic definitions.
    searchTypeString.getGenerics().stream()
        .forEach(genTypeStr -> magikType.addGenericDefinition(null, genTypeStr));

    // Add methods from ExemplarDefinition.
    final TypeString bareDefinitionTypeString = definitionTypeString.getWithoutGenerics();
    this.definitionKeeper.getMethodDefinitions(bareDefinitionTypeString).stream()
        .forEach(methodDef -> this.addMethod(magikType, methodDef));

    // The upper is incomplete. In case a method is defined on `system`, but from package `user`.
    // I.e., on
    // `user:system`. The resolver resolve resolves use of `system` to `sw:system` and gathers the
    // methods with that
    // typeref, but does not "see" the methods on `user:system`.
    // To resolve this, all descendant packages of the package where the exemplar lives should be
    // searched
    // and tested if there is NOT a `<package>:system`. If so, all the methods should be added as
    // well.
    final Deque<String> packages = new ArrayDeque<>();
    final String startPackage = definitionTypeString.getPakkage();
    packages.push(startPackage);

    final List<PackageDefinition> seen = new ArrayList<>();
    while (!packages.isEmpty()) {
      // Find all packages which use the current package.
      final String packageName = packages.pop();
      this.definitionKeeper.getPackageDefinitions().stream()
          .filter(pkgDef -> pkgDef.getUses().contains(packageName))
          .filter(pkgDef -> !seen.contains(pkgDef))
          .filter(
              pkgDef -> {
                final String pkgName = pkgDef.getName();
                packages.push(pkgName);

                // Test if the package does not contain an ExemplarDefinition with the specific
                // identifier.
                return this.definitionKeeper.getPackageDefinitions(packageName).stream()
                    .anyMatch(
                        childPkgDef -> {
                          final String childPkgName = childPkgDef.getName();
                          final String identifier = definitionTypeString.getIdentifier();
                          final TypeString childPkgTypeStr =
                              TypeString.ofIdentifier(identifier, childPkgName);
                          return this.definitionKeeper
                              .getExemplarDefinitions(childPkgTypeStr)
                              .isEmpty();
                        });
              })
          .flatMap(
              pkgDef ->
                  // Get all MethodDefinitions from the package.
                  this.definitionKeeper.getPackageDefinitions(packageName).stream()
                      .flatMap(
                          childPkgDef -> {
                            final String childPkgName = childPkgDef.getName();
                            final String identifier = definitionTypeString.getIdentifier();
                            final TypeString childPkgTypeStr =
                                TypeString.ofIdentifier(identifier, childPkgName);
                            return this.definitionKeeper
                                .getMethodDefinitions(childPkgTypeStr)
                                .stream();
                          }))
          .distinct()
          .forEach(methodDef -> this.addMethod(magikType, methodDef));
    }

    return magikType;
  }

  private AbstractType createType(final ProcedureDefinition procedureDefinition) {
    final AbstractType abstractType = this.getType(TypeString.SW_PROCEDURE);
    final MagikType procedureType = (MagikType) abstractType;

    final EnumSet<ProcedureInstance.Modifier> modifiers =
        procedureDefinition.getModifiers().stream()
            .map(DefinitionKeeperTypeKeeperAdapter.PROCEDURE_MODIFIER_MAPPING::get)
            .collect(
                Collectors.toCollection(() -> EnumSet.noneOf(ProcedureInstance.Modifier.class)));
    final List<Parameter> parameters =
        procedureDefinition.getParameters().stream()
            .map(
                paramDef ->
                    new Parameter(
                        paramDef.getLocation(),
                        paramDef.getName(),
                        paramDef.getModifier() != null
                            ? DefinitionKeeperTypeKeeperAdapter.PARAMETER_MODIFIER_MAPPING.get(
                                paramDef.getModifier())
                            : Parameter.Modifier.NONE,
                        paramDef.getTypeName()))
            .toList();
    final ProcedureInstance instance =
        new ProcedureInstance(
            procedureDefinition.getLocation(),
            procedureDefinition.getModuleName(),
            procedureType,
            procedureDefinition.getProcedureName(),
            modifiers,
            parameters,
            procedureDefinition.getDoc(),
            procedureDefinition.getReturnTypes(),
            procedureDefinition.getLoopTypes());

    final TypeString typeRef = procedureDefinition.getTypeString();
    if (typeRef == null) {
      throw new IllegalStateException("Cannot feed ProcedureDefinition without a type reference.");
    }

    return new AliasType(
        this,
        procedureDefinition.getLocation(),
        procedureDefinition.getModuleName(),
        typeRef,
        instance);
  }

  private AbstractType createType(final GlobalDefinition globalDefinition) {
    final TypeString typeRef = globalDefinition.getTypeString();
    final TypeString aliasedTypeName = globalDefinition.getAliasedTypeName();
    return new AliasType(
        this,
        globalDefinition.getLocation(),
        globalDefinition.getModuleName(),
        typeRef,
        aliasedTypeName);
  }

  private Method addMethod(final MagikType type, final MethodDefinition methodDefinition) {
    final ParameterDefinition assignmentParameter = methodDefinition.getAssignmentParameter();
    final Method method =
        type.addMethod(
            methodDefinition.getLocation(),
            methodDefinition.getModuleName(),
            methodDefinition.getModifiers().stream()
                .map(DefinitionKeeperTypeKeeperAdapter.METHOD_MODIFIER_MAPPING::get)
                .collect(Collectors.toSet()),
            methodDefinition.getMethodName(),
            methodDefinition.getParameters().stream()
                .map(
                    paramDef ->
                        new Parameter(
                            paramDef.getLocation(),
                            paramDef.getName(),
                            DefinitionKeeperTypeKeeperAdapter.PARAMETER_MODIFIER_MAPPING.get(
                                paramDef.getModifier()),
                            paramDef.getTypeName()))
                .toList(),
            assignmentParameter != null
                ? new Parameter(
                    assignmentParameter.getLocation(),
                    assignmentParameter.getName(),
                    DefinitionKeeperTypeKeeperAdapter.PARAMETER_MODIFIER_MAPPING.get(
                        assignmentParameter.getModifier()),
                    assignmentParameter.getTypeName())
                : null,
            methodDefinition.getDoc(),
            methodDefinition.getReturnTypes(),
            methodDefinition.getLoopTypes());

    methodDefinition.getTopics().forEach(method::addTopic);

    methodDefinition
        .getUsedGlobals()
        .forEach(
            globalUsage ->
                method.addUsedGlobal(
                    new Method.GlobalUsage(globalUsage.getTypeName(), globalUsage.getLocation())));
    methodDefinition
        .getUsedMethods()
        .forEach(
            methodUsage ->
                method.addCalledMethod(
                    new Method.MethodUsage(
                        methodUsage.getTypeName(),
                        methodUsage.getMethodName(),
                        methodUsage.getLocation())));
    methodDefinition
        .getUsedSlots()
        .forEach(
            slotUsage ->
                method.addUsedSlot(
                    new Method.SlotUsage(slotUsage.getSlotName(), slotUsage.getLocation())));
    methodDefinition
        .getUsedConditions()
        .forEach(
            conditionUsage ->
                method.addUsedCondition(
                    new Method.ConditionUsage(
                        conditionUsage.getConditionName(), conditionUsage.getLocation())));
    return method;
  }

  @Override
  public AbstractType getTypeInPackage(final TypeString typeString) {
    final Collection<ExemplarDefinition> definitions =
        this.definitionKeeper.getExemplarDefinitions(typeString);
    if (definitions.isEmpty()) {
      return UndefinedType.INSTANCE;
    }

    final ExemplarDefinition definition = definitions.stream().findAny().orElseThrow();
    final TypeString definitionTypeString = definition.getTypeString();
    if (!definitionTypeString.getPakkage().equals(typeString.getPakkage())) {
      return UndefinedType.INSTANCE;
    }

    return this.getType(typeString);
  }

  @Override
  public void removeType(final AbstractType type) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<AbstractType> getTypes() {
    return this.definitionKeeper.getExemplarDefinitions().stream()
        .map(ExemplarDefinition::getTypeString)
        .map(this::getType)
        .collect(Collectors.toSet());
  }

  @Override
  public void addCondition(final Condition condition) {
    throw new UnsupportedOperationException();
  }

  @Override
  public synchronized Condition getCondition(final String name) {
    if (this.conditionCache.containsKey(name)) {
      return this.conditionCache.get(name);
    }

    final Collection<ConditionDefinition> definitions =
        this.definitionKeeper.getConditionDefinitions(name);
    if (definitions.isEmpty()) {
      return null;
    }

    final ConditionDefinition definition = definitions.stream().findAny().orElseThrow();
    final Condition condition =
        new Condition(
            definition.getModuleName(),
            definition.getLocation(),
            definition.getName(),
            definition.getParent(),
            definition.getDataNames(),
            definition.getDoc());

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "{} Created condition, name: {}, condition: {}",
          Integer.toHexString(this.hashCode()),
          name,
          condition);
    }
    this.conditionCache.put(name, condition);
    return condition;
  }

  @Override
  public Collection<Condition> getConditions() {
    return this.definitionKeeper.getConditionDefinitions().stream()
        .map(ConditionDefinition::getName)
        .map(this::getCondition)
        .collect(Collectors.toSet());
  }

  @Override
  public void removeCondition(final Condition condition) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addBinaryOperator(final BinaryOperator binaryOperator) {
    throw new UnsupportedOperationException();
  }

  @Override
  public synchronized BinaryOperator getBinaryOperator(
      final BinaryOperator.Operator operator,
      final TypeString leftType,
      final TypeString rightType) {
    final BinaryOperatorKey key = new BinaryOperatorKey(operator, leftType, rightType);
    if (this.binaryOperatorCache.containsKey(key)) {
      return this.binaryOperatorCache.get(key);
    }

    final String operatorStr = operator.getValue();
    final Collection<BinaryOperatorDefinition> definitions =
        this.definitionKeeper.getBinaryOperatorDefinitions(operatorStr, leftType, rightType);
    if (definitions.isEmpty()) {
      // this.binaryOperatorCache.put(cacheKey, null);  // TODO: ConcurrentHashMap does not support
      // null values.

      return null;
    }

    // Take first, even though multiple can be registered.
    final BinaryOperatorDefinition definition = definitions.stream().findAny().orElseThrow();
    final BinaryOperator binaryOperator =
        new BinaryOperator(
            definition.getLocation(),
            definition.getModuleName(),
            operator,
            definition.getLhsTypeName(),
            definition.getRhsTypeName(),
            definition.getResultTypeName(),
            definition.getDoc());

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "{} Binary operator, key: {}, binary operator: {}",
          Integer.toHexString(this.hashCode()),
          key.getString(),
          binaryOperator);
    }
    this.binaryOperatorCache.put(key, binaryOperator);
    return binaryOperator;
  }

  @Override
  public void removeBinaryOperator(final BinaryOperator binaryOperator) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<BinaryOperator> getBinaryOperators() {
    return this.definitionKeeper.getBinaryOperatorDefinitions().stream()
        .map(
            definition ->
                this.getBinaryOperator(
                    BinaryOperator.Operator.valueFor(definition.getOperator()),
                    definition.getLhsTypeName(),
                    definition.getRhsTypeName()))
        .collect(Collectors.toSet());
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }
}
