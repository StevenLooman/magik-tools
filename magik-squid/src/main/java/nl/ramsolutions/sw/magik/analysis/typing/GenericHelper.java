package nl.ramsolutions.sw.magik.analysis.typing;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.types.MagikType;
import nl.ramsolutions.sw.magik.analysis.typing.types.Method;
import nl.ramsolutions.sw.magik.analysis.typing.types.Parameter;
import nl.ramsolutions.sw.magik.analysis.typing.types.Slot;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;

/**
 * Generic helper.
 */
public class GenericHelper {

    private final ITypeKeeper typeKeeper;
    private final AbstractType type;

    /**
     * Constructor.
     * @param typeKeeper TypeKeeper to use.
     * @param type Type to use.
     */
    public GenericHelper(final ITypeKeeper typeKeeper, final AbstractType type) {
        this.typeKeeper = typeKeeper;
        this.type = type;
    }

    /**
     * Substitute generic for {@link Method}.
     * @param method Method.
     * @return Method with generics substituted.
     */
    public Method substituteGenerics(final Method method) {
        if (this.type.getGenericDefinitions().isEmpty()) {
            return method;
        }

        final Map<TypeString, TypeString> genericTypeMapping = this.getGenericDefinitionTypeMapping();

        // Substitute parameters.
        final List<Parameter> parameters = method.getParameters().stream()
            .map(param -> {
                final Location location = param.getLocation();
                final String name = param.getName();
                final Parameter.Modifier modifier = param.getModifier();
                TypeString newTypeStr = param.getType();
                for (final Map.Entry<TypeString, TypeString> entry : genericTypeMapping.entrySet()) {
                    final TypeString from = entry.getKey();
                    final TypeString to = entry.getValue();
                    newTypeStr = newTypeStr.substituteType(from, to);
                }
                return new Parameter(location, name, modifier, newTypeStr);
            })
            .collect(Collectors.toList());

        // Subsitute results.
        final ExpressionResultString callResult = method.getCallResult();
        final ExpressionResultString loopbodyResult = method.getLoopbodyResult();
        return new Method(
            method.getLocation(),
            method.getModuleName(),
            method.getModifiers(),
            method.getOwner(),
            method.getName(),
            parameters,
            method.getAssignmentParameter(),
            method.getDoc(),
            this.substituteGenerics(callResult),
            this.substituteGenerics(loopbodyResult));
    }

    /**
     * Substitute generic for {@link Slot}.
     * @param slot Slot.
     * @return Slot with generics substituted.
     */
    public Slot substituteGenerics(final Slot slot) {
        if (this.type.getGenericDefinitions().isEmpty()) {
            return slot;
        }

        final Map<TypeString, TypeString> genericTypes = this.getGenericDefinitionTypeMapping();
        final TypeString slotType = slot.getType();
        final TypeString newSlotType = genericTypes.getOrDefault(slotType, slotType);
        return new Slot(
            slot.getLocation(),
            slot.getName(),
            newSlotType);
    }

    /**
    * Substitute generics for {@link AbstractType}.
    * @param sourceType Source type to rebuild.
    * @return Type with generics substituted.
    */
    public AbstractType substituteGenerics(final AbstractType sourceType) {
        if (this.type.getGenericDefinitions().isEmpty()) {
            return sourceType;
        }

        final TypeString typeString = sourceType.getTypeString();
        final TypeString[] genTypeStrsArr = this.type.getGenericDefinitions().stream()
            .map(genDef -> genDef.getTypeString())
            .toArray(TypeString[]::new);
        final TypeString typeStringWithGenerics =
            TypeString.ofIdentifier(
                typeString.getIdentifier(), typeString.getPakkage(),
                genTypeStrsArr);
        final MagikType.Sort sort = sourceType instanceof MagikType
            ? ((MagikType) sourceType).getSort()
            : MagikType.Sort.UNDEFINED;
        final MagikType newType = new MagikType(this.typeKeeper, null, null, sort, typeStringWithGenerics);
        this.type.getGenericDefinitions()
            .forEach(genDef -> newType.addGenericDefinition(genDef.getLocation(), genDef.getTypeString()));
        return newType;
    }

    /**
     * Substitute generics for {@link ExpressionResultString}.
     * @param expressionResultString {@link ExpressionResultString} to rebuild.
     * @return {@link ExpressionResultString} with generics substituted.
     */
    public ExpressionResultString substituteGenerics(final ExpressionResultString expressionResultString) {
        ExpressionResultString substitutedResultString = expressionResultString;
        final Map<TypeString, TypeString> genericTypeMapping = this.getGenericDefinitionTypeMapping();
        for (final Map.Entry<TypeString, TypeString> entry : genericTypeMapping.entrySet()) {
            final TypeString from = entry.getKey();
            final TypeString to = entry.getValue();
            substitutedResultString = substitutedResultString.substituteType(from, to);
        }

        return substitutedResultString;
    }

    private Map<TypeString, TypeString> getGenericDefinitionTypeMapping() {
        return this.type.getGenericDefinitions().stream()
            .collect(Collectors.toMap(
                def -> def.getGenericReference(),
                // TODO: Use GenericDefinition? Or substituted type?
                def -> def.getTypeString().getGenerics().get(0)));
    }

}
