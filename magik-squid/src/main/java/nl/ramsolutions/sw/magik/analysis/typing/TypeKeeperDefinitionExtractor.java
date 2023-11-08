package nl.ramsolutions.sw.magik.analysis.typing;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.analysis.definitions.BinaryOperatorDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ConditionDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.Definition;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.PackageDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ParameterDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ProcedureDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.AliasType;
import nl.ramsolutions.sw.magik.analysis.typing.types.MagikType;
import nl.ramsolutions.sw.magik.analysis.typing.types.Method;
import nl.ramsolutions.sw.magik.analysis.typing.types.Parameter;
import nl.ramsolutions.sw.magik.analysis.typing.types.ProcedureInstance;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;

/**
 * {@link TypeKeeper} {@link Definition} extractor.
 */
public class TypeKeeperDefinitionExtractor {

    private static final Map<MagikType.Sort, ExemplarDefinition.Sort> EXEMPLAR_SORT_MAPPING = Map.of(
        MagikType.Sort.OBJECT, ExemplarDefinition.Sort.OBJECT,
        MagikType.Sort.INDEXED, ExemplarDefinition.Sort.INDEXED,
        MagikType.Sort.INTRINSIC, ExemplarDefinition.Sort.INTRINSIC,
        MagikType.Sort.SLOTTED, ExemplarDefinition.Sort.SLOTTED);
    private static final Map<Parameter.Modifier, ParameterDefinition.Modifier> PARAMETER_MODIFIER_MAPPING = Map.of(
        Parameter.Modifier.NONE, ParameterDefinition.Modifier.NONE,
        Parameter.Modifier.OPTIONAL, ParameterDefinition.Modifier.OPTIONAL,
        Parameter.Modifier.GATHER, ParameterDefinition.Modifier.GATHER);
    private static final Map<Method.Modifier, MethodDefinition.Modifier> METHOD_MODIFIER_MAPPING = Map.of(
        Method.Modifier.ABSTRACT, MethodDefinition.Modifier.ABSTRACT,
        Method.Modifier.ITER, MethodDefinition.Modifier.ITER,
        Method.Modifier.PRIVATE, MethodDefinition.Modifier.PRIVATE);
    private static final Map<Method.Modifier, ProcedureDefinition.Modifier> PROCEDURE_MODIFIER_MAPPING =
        Map.of(Method.Modifier.ITER, ProcedureDefinition.Modifier.ITER);

    private final ITypeKeeper typeKeeper;

    public TypeKeeperDefinitionExtractor(final ITypeKeeper typeKeeper) {
        this.typeKeeper = typeKeeper;
    }

    public Collection<PackageDefinition> getPackageDefinitions() {
        return this.typeKeeper.getPackages().stream()
            .map(pakkage -> new PackageDefinition(
                pakkage.getLocation(),
                pakkage.getModuleName(),
                null,
                pakkage.getName(),
                List.copyOf(pakkage.getUses()),
                pakkage.getDoc()))
            .collect(Collectors.toSet());
    }

    public Collection<ExemplarDefinition> getExemplarDefinitions() {
        return this.typeKeeper.getTypes().stream()
            .filter(type -> type instanceof MagikType)
            .map(MagikType.class::cast)
            .map(type -> {
                final ExemplarDefinition.Sort sort =
                    TypeKeeperDefinitionExtractor.EXEMPLAR_SORT_MAPPING.get(type.getSort());
                final List<TypeString> parents = type.getParents().stream()
                    .map(AbstractType::getTypeString)
                    .sorted()
                    .collect(Collectors.toList());
                final List<ExemplarDefinition.GenericDeclaration> genericDeclarations = type.getGenerics().stream()
                    .map(genericDecl -> new ExemplarDefinition.GenericDeclaration(null, genericDecl.getName()))
                    .collect(Collectors.toList());
                final List<ExemplarDefinition.Slot> slots = type.getSlots().stream()
                    .map(slot -> new ExemplarDefinition.Slot(
                        slot.getLocation(),
                        null,
                        slot.getName(),
                        slot.getType()))
                    .collect(Collectors.toList());
                return new ExemplarDefinition(
                    type.getLocation(),
                    type.getModuleName(),
                    null,
                    sort,
                    type.getTypeString(),
                    slots,
                    parents,
                    type.getDoc(),
                    genericDeclarations);
            })
            .collect(Collectors.toSet());
    }

    public Collection<MethodDefinition> getMethodDefinitions() {
        return this.typeKeeper.getTypes().stream()
            .flatMap(type -> type.getLocalMethods().stream())
            .filter(MagikType.class::isInstance)
            .map(method -> {
                final List<ParameterDefinition> parameterDefs = method.getParameters().stream()
                    .map(parameter -> {
                        return new ParameterDefinition(
                            parameter.getLocation(),
                            method.getModuleName(),
                            null,
                            parameter.getName(),
                            TypeKeeperDefinitionExtractor.PARAMETER_MODIFIER_MAPPING.get(parameter.getModifier()),
                            parameter.getType(),
                            null);
                    })
                    .collect(Collectors.toList());
                final Parameter assignmentParameter = method.getAssignmentParameter();
                final ParameterDefinition assignmentParameterDef = assignmentParameter != null
                    ? new ParameterDefinition(
                        assignmentParameter.getLocation(),
                        method.getModuleName(),
                        null,
                        assignmentParameter.getName(),
                        TypeKeeperDefinitionExtractor.PARAMETER_MODIFIER_MAPPING.get(assignmentParameter.getModifier()),
                        assignmentParameter.getType(),
                        null)
                    : null;
                final Set<MethodDefinition.Modifier> modifiers = method.getModifiers().stream()
                    .map(TypeKeeperDefinitionExtractor.METHOD_MODIFIER_MAPPING::get)
                    .collect(Collectors.toSet());
                return new MethodDefinition(
                    method.getLocation(),
                    method.getModuleName(),
                    null,
                    method.getOwner().getTypeString(),
                    method.getName(),
                    modifiers,
                    parameterDefs,
                    assignmentParameterDef,
                    method.getDoc(),
                    method.getCallResult(),
                    method.getLoopbodyResult());
            })
            .collect(Collectors.toSet());
    }

    public Collection<ProcedureDefinition> getProcedureDefinitions() {
        return this.typeKeeper.getTypes().stream()
            .filter(type -> type instanceof AliasType)
            .map(AliasType.class::cast)
            .map(AliasType::getAliasedType)
            .filter(type -> type instanceof ProcedureInstance)
            .map(ProcedureInstance.class::cast)
            .map(type -> {
                final Method method = type.getLocalMethods("invoke()").stream()
                    .findAny()
                    .orElseThrow();
                final List<ParameterDefinition> parameterDefs = method.getParameters().stream()
                    .map(parameter -> {
                        return new ParameterDefinition(
                            parameter.getLocation(),
                            method.getModuleName(),
                            null,
                            parameter.getName(),
                            TypeKeeperDefinitionExtractor.PARAMETER_MODIFIER_MAPPING.get(parameter.getModifier()),
                            parameter.getType(),
                            null);
                    })
                    .collect(Collectors.toList());
                final Set<ProcedureDefinition.Modifier> modifiers = method.getModifiers().stream()
                    .map(TypeKeeperDefinitionExtractor.PROCEDURE_MODIFIER_MAPPING::get)
                    .collect(Collectors.toSet());
                return new ProcedureDefinition(
                    type.getLocation(),
                    type.getModuleName(),
                    null,
                    modifiers,
                    type.getTypeString(),  // TODO: Is this right? Shouldn't this be the alias?
                    type.getName(),
                    parameterDefs,
                    type.getDoc(),
                    method.getCallResult(),
                    method.getLoopbodyResult());
            })
            .collect(Collectors.toSet());
    }

    public Collection<GlobalDefinition> getGlobalDefinitions() {
        return this.typeKeeper.getTypes().stream()
            .filter(type -> type instanceof AliasType)
            .map(AliasType.class::cast)
            .filter(alias -> !(alias.getAliasedType() instanceof ProcedureInstance))
            .map(type -> {
                return new GlobalDefinition(
                    type.getLocation(),
                    type.getModuleName(),
                    null,
                    type.getTypeString(),
                    type.getAliasedType().getTypeString(),
                    type.getDoc());
            })
            .collect(Collectors.toSet());
    }

    public Collection<ConditionDefinition> getConditionDefinitions() {
        return this.typeKeeper.getConditions().stream()
            .map(condition -> {
                return new ConditionDefinition(
                    condition.getLocation(),
                    condition.getModuleName(),
                    null,
                    condition.getName(),
                    condition.getParent(),
                    condition.getDataNameList(),
                    condition.getDoc());
            })
            .collect(Collectors.toSet());
    }

    public Collection<BinaryOperatorDefinition> getBinaryOperatorDefinitions() {
        return this.typeKeeper.getBinaryOperators().stream()
            .map(operator -> {
                return new BinaryOperatorDefinition(
                    operator.getLocation(),
                    operator.getModuleName(),
                    null,
                    operator.getOperator().toString().toLowerCase(),
                    operator.getLeftType(),
                    operator.getRightType(),
                    operator.getResultType(),
                    operator.getDoc());
            })
            .collect(Collectors.toSet());
    }

}
