package nl.ramsolutions.sw.magik.analysis.typing;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import nl.ramsolutions.sw.magik.Location;
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

/**
 * {@link TypeKeeper} {@link Definition} inserter.
 */
public class TypeKeeperDefinitionInserter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TypeKeeperDefinitionInserter.class);

    private static final Map<ExemplarDefinition.Sort, MagikType.Sort> EXEMPLAR_SORT_MAPPING = Map.of(
        ExemplarDefinition.Sort.UNDEFINED, MagikType.Sort.UNDEFINED,
        ExemplarDefinition.Sort.OBJECT, MagikType.Sort.OBJECT,
        ExemplarDefinition.Sort.INDEXED, MagikType.Sort.INDEXED,
        ExemplarDefinition.Sort.INTRINSIC, MagikType.Sort.INTRINSIC,
        ExemplarDefinition.Sort.SLOTTED, MagikType.Sort.SLOTTED);
    private static final Map<ParameterDefinition.Modifier, Parameter.Modifier> PARAMETER_MODIFIER_MAPPING = Map.of(
        ParameterDefinition.Modifier.NONE, Parameter.Modifier.NONE,
        ParameterDefinition.Modifier.OPTIONAL, Parameter.Modifier.OPTIONAL,
        ParameterDefinition.Modifier.GATHER, Parameter.Modifier.GATHER);
    private static final Map<MethodDefinition.Modifier, Method.Modifier> METHOD_MODIFIER_MAPPING = Map.of(
        MethodDefinition.Modifier.ABSTRACT, Method.Modifier.ABSTRACT,
        MethodDefinition.Modifier.ITER, Method.Modifier.ITER,
        MethodDefinition.Modifier.PRIVATE, Method.Modifier.PRIVATE);
    private static final Map<ProcedureDefinition.Modifier, ProcedureInstance.Modifier> PROCEDURE_MODIFIER_MAPPING =
        Map.of(ProcedureDefinition.Modifier.ITER, ProcedureInstance.Modifier.ITER);
    private static final TypeString SW_PROCEDURE_REF = TypeString.ofIdentifier("procedure", "sw");

    private final ITypeKeeper typeKeeper;

    public TypeKeeperDefinitionInserter(final ITypeKeeper typeKeeper) {
        this.typeKeeper = typeKeeper;
    }

    /**
     * Feed a {@link PackageDefinition}.
     * @param definition Defition to feed.
     */
    public Package feed(final PackageDefinition definition) {
        final String packageName = definition.getName();
        if (!this.typeKeeper.hasPackage(packageName)) {
            new Package(this.typeKeeper, definition.getLocation(), definition.getModuleName(), packageName);
        }

        final Package pakkage = this.typeKeeper.getPackage(packageName);
        definition.getUses().forEach(pakkage::addUse);

        return pakkage;
    }

    /**
     * Feed a {@link ExemplarDefinition}.
     * @param definition Defition to feed.
     */
    public AbstractType feed(final ExemplarDefinition definition) {
        this.ensurePackageExists(definition.getPackage());

        final TypeString typeRef = definition.getTypeString();
        final ExemplarDefinition.Sort definitionSort = definition.getSort();
        final MagikType.Sort magikTypeSort = TypeKeeperDefinitionInserter.EXEMPLAR_SORT_MAPPING.get(definitionSort);
        final Location location = definition.getLocation();

        final MagikType magikType;
        final AbstractType abstractType = this.getType(typeRef);
        if (abstractType instanceof UndefinedType) {
            // Create a new type. Note that we might overwrite something?
            if (this.typeKeeper.hasType(typeRef)) {
                final AbstractType removedType = this.typeKeeper.getType(typeRef);
                LOGGER.debug("Overwriting type: {}, with: {}", removedType, definition);
                this.typeKeeper.removeType(removedType);
            }

            magikType = new MagikType(
                this.typeKeeper,
                location,
                definition.getModuleName(),
                magikTypeSort,
                typeRef);
        } else {
            magikType = (MagikType) abstractType;
        }

        final String doc = definition.getDoc();
        magikType.setDoc(doc);

        definition.getSlots().stream()
            .filter(slotDef -> magikType.getSlot(slotDef.getName()) == null)
            .forEach(slotDef -> magikType.addSlot(slotDef.getLocation(), slotDef.getName(), slotDef.getTypeName()));

        definition.getGenericDeclarations().stream()
            .filter(genDef -> magikType.getGeneric(genDef.getName()) == null)
            .forEach(genDef -> magikType.addGeneric(genDef.getLocation(), genDef.getName()));

        definition.getParents().stream()
            .filter(parentTypeRef -> !magikType.getParentsTypeRefs().contains(parentTypeRef))
            .forEach(parentTypeRef -> magikType.addParent(parentTypeRef));

        return magikType;
    }

    /**
     * Feed a {@link MethodDefinition}.
     * @param definition Defition to feed.
     */
    public Method feed(final MethodDefinition definition) {
        final MagikType magikType = this.ensureMagikTypeExists(definition.getTypeName());

        final String methodName = definition.getMethodName();
        final Location location = definition.getLocation();
        final Method existingMethod = magikType.getLocalMethods(methodName).stream()
            .filter(method ->
                method.getLocation() == null && location == null
                || method.getLocation() != null && method.getLocation().equals(location))
            .findAny()
            .orElse(null);
        if (existingMethod != null) {
            return existingMethod;
        }

        final EnumSet<Method.Modifier> modifiers = definition.getModifiers().stream()
            .map(TypeKeeperDefinitionInserter.METHOD_MODIFIER_MAPPING::get)
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(Method.Modifier.class)));
        final List<Parameter> parameters = definition.getParameters().stream()
            .map(paramDef -> new Parameter(
                paramDef.getLocation(),
                paramDef.getName(),
                paramDef.getModifier() != null
                    ? TypeKeeperDefinitionInserter.PARAMETER_MODIFIER_MAPPING.get(paramDef.getModifier())
                    : Parameter.Modifier.NONE,
                paramDef.getTypeName()))
            .collect(Collectors.toList());
        final ParameterDefinition assignmentParameterDef = definition.getAssignmentParameter();
        final Parameter.Modifier assignmentParameterModifier =
            assignmentParameterDef != null && assignmentParameterDef.getModifier() != null
                ? TypeKeeperDefinitionInserter.PARAMETER_MODIFIER_MAPPING.get(assignmentParameterDef.getModifier())
                : Parameter.Modifier.NONE;
        final Parameter assignmentParameter = assignmentParameterDef != null
            ?  new Parameter(
                assignmentParameterDef.getLocation(),
                assignmentParameterDef.getName(),
                assignmentParameterModifier,
                assignmentParameterDef.getTypeName())
            : null;
        return magikType.addMethod(
            location,
            definition.getModuleName(),
            modifiers,
            methodName,
            parameters,
            assignmentParameter,
            definition.getDoc(),
            definition.getReturnTypes(),
            definition.getLoopTypes());
    }

    /**
     * Feed a {@link GlobalDefinition}.
     * @param definition Defition to feed.
     */
    public AliasType feed(final GlobalDefinition definition) {
        this.ensurePackageExists(definition.getPackage());

        final TypeString typeRef = definition.getTypeString();
        final AbstractType abstractType = this.typeKeeper.getType(typeRef);
        if (abstractType != UndefinedType.INSTANCE) {
            LOGGER.debug("Overwriting type: {}, with: {}", abstractType, definition);
            this.typeKeeper.removeType(abstractType);
        }

        return new AliasType(
            this.typeKeeper,
            definition.getLocation(),
            definition.getModuleName(),
            typeRef,
            definition.getAliasedTypeName());
    }

    /**
     * Feed a {@link BinaryOperatorDefinition}.
     * @param definition Defition to feed.
     */
    public BinaryOperator feed(final BinaryOperatorDefinition definition) {
        final String operatorStr = definition.getOperator();
        final BinaryOperator.Operator operator = BinaryOperator.Operator.valueFor(operatorStr);
        final TypeString lhsRef = definition.getLhsTypeName();
        final TypeString rhsRef = definition.getRhsTypeName();
        final TypeString resultRef = definition.getResultTypeName();
        final BinaryOperator binaryOperator = new BinaryOperator(
            definition.getLocation(),
            definition.getModuleName(),
            operator,
            lhsRef,
            rhsRef,
            resultRef,
            definition.getDoc());
        this.typeKeeper.addBinaryOperator(binaryOperator);
        return binaryOperator;
    }

    /**
     * Feed a {@link ConditionDefinition}.
     * @param definition Defition to feed.
     */
    public Condition feed(final ConditionDefinition definition) {
        final Condition condition = new Condition(
            definition.getModuleName(),
            definition.getLocation(),
            definition.getName(),
            definition.getParent(),
            definition.getDataNames(),
            definition.getDoc());
        this.typeKeeper.addCondition(condition);
        return condition;
    }

    /**
     * Feed a {@link ProcedureDefinition}.
     * @param definition Definition to feed.
     */
    public void feed(final ProcedureDefinition definition) {
        this.ensurePackageExists(definition.getPackage());

        final MagikType procedureType = (MagikType) typeKeeper.getType(TypeKeeperDefinitionInserter.SW_PROCEDURE_REF);
        final EnumSet<ProcedureInstance.Modifier> modifiers = definition.getModifiers().stream()
            .map(TypeKeeperDefinitionInserter.PROCEDURE_MODIFIER_MAPPING::get)
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(ProcedureInstance.Modifier.class)));
        final List<Parameter> parameters = definition.getParameters().stream()
            .map(paramDef -> new Parameter(
                paramDef.getLocation(),
                paramDef.getName(),
                paramDef.getModifier() != null
                    ? TypeKeeperDefinitionInserter.PARAMETER_MODIFIER_MAPPING.get(paramDef.getModifier())
                    : Parameter.Modifier.NONE,
                paramDef.getTypeName()))
            .collect(Collectors.toList());
        final ProcedureInstance instance = new ProcedureInstance(
            definition.getLocation(),
            definition.getModuleName(),
            procedureType,
            definition.getProcedureName(),
            modifiers,
            parameters,
            definition.getDoc(),
            definition.getReturnTypes(),
            definition.getLoopTypes());

        final TypeString typeRef = definition.getTypeName();
        if (typeRef == null) {
            throw new IllegalStateException("Cannot feed ProcedureDefinition without a type reference.");
        }

        new AliasType(
            this.typeKeeper,
            definition.getLocation(),
            definition.getModuleName(),
            typeRef,
            instance);
    }

    /**
     * Get the {@link AbstractType}, following {@link AliasType}s if needed.
     * @param typeRef Type reference.
     * @return Type, which might also be {@link UndefinedType.INSTANCE}.
     */
    private AbstractType getType(final TypeString typeRef) {
        if (this.typeKeeper.hasType(typeRef)) {
            final AbstractType abstractType = this.typeKeeper.getType(typeRef);
            if (abstractType instanceof AliasType) {
                final AliasType aliasType = (AliasType) abstractType;
                final TypeString aliasedRef = aliasType.getAliasedType().getTypeString();
                return this.getType(aliasedRef);
            }
        }

        // Can be the UndefinedType!
        return this.typeKeeper.getType(typeRef);
    }

    private void ensurePackageExists(final @Nullable String name) {
        if (name == null) {
            return;
        }

        final Package pakkage = this.typeKeeper.getPackage(name);
        if (pakkage != null) {
            return;
        }

        final PackageDefinition definition = new PackageDefinition(
            null,
            null,
            null,
            null,
            name,
            Collections.emptyList());
        this.feed(definition);
    }

    private MagikType ensureMagikTypeExists(final TypeString typeRef) {
        this.ensurePackageExists(typeRef.getPakkage());

        final AbstractType abstractType = this.getType(typeRef);
        if (!(abstractType instanceof MagikType)) {
            final ExemplarDefinition exemplarDefinition = new ExemplarDefinition(
                null,
                null,
                null,
                null,
                ExemplarDefinition.Sort.UNDEFINED,
                typeRef,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList());
            this.feed(exemplarDefinition);
        }

        final AbstractType abstractType2 = this.getType(typeRef);
        if (!(abstractType2 instanceof MagikType)) {
            LOGGER.info("Unexpected non-magik type for ref: {} -> {}", typeRef, abstractType2);
        }

        return (MagikType) abstractType2;
    }

}
