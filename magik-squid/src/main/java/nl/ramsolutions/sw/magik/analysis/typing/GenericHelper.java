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

/** Generic helper. */
public class GenericHelper {

  private final ITypeKeeper typeKeeper;
  private final AbstractType type;

  /**
   * Constructor.
   *
   * @param typeKeeper TypeKeeper to use.
   * @param type Type to use.
   */
  public GenericHelper(final ITypeKeeper typeKeeper, final AbstractType type) {
    this.typeKeeper = typeKeeper;
    this.type = type;
  }

  /**
   * Substitute generic for {@link Method}.
   *
   * @param method Method.
   * @return Method with generics substituted.
   */
  public Method substituteGenerics(final Method method) {
    if (this.type.getGenericDefinitions().isEmpty()) {
      return method;
    }

    // Substitute parameters.
    final List<Parameter> parameters =
        method.getParameters().stream()
            .map(
                param -> {
                  final Location location = param.getLocation();
                  final String name = param.getName();
                  final Parameter.Modifier modifier = param.getModifier();
                  final TypeString typeStr = param.getType();
                  final TypeString newTypeStr = this.substituteGenerics(typeStr);
                  return new Parameter(location, name, modifier, newTypeStr);
                })
            .toList();

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
   *
   * @param slot Slot.
   * @return Slot with generics substituted.
   */
  public Slot substituteGenerics(final Slot slot) {
    if (this.type.getGenericDefinitions().isEmpty()) {
      return slot;
    }

    final TypeString slotType = slot.getType();
    final TypeString newSlotType = this.substituteGenerics(slotType);
    return new Slot(slot.getLocation(), slot.getName(), newSlotType);
  }

  /**
   * Substitute generics for {@link AbstractType}.
   *
   * @param sourceType Source type to rebuild.
   * @return Type with generics substituted.
   */
  public AbstractType substituteGenerics(final AbstractType sourceType) {
    if (this.type.getGenericDefinitions().isEmpty()) {
      return sourceType;
    }

    final TypeString typeString = sourceType.getTypeString();
    final TypeString newTypeString = this.substituteGenerics(typeString);
    final MagikType.Sort sort =
        sourceType instanceof MagikType magikType ? magikType.getSort() : MagikType.Sort.UNDEFINED;
    final MagikType newType = new MagikType(this.typeKeeper, null, null, sort, newTypeString);
    this.type
        .getGenericDefinitions()
        .forEach(
            genDef -> newType.addGenericDefinition(genDef.getLocation(), genDef.getTypeString()));
    return newType;
  }

  /**
   * Substitute generics for {@link ExpressionResultString}.
   *
   * @param expressionResultString {@link ExpressionResultString} to rebuild.
   * @return {@link ExpressionResultString} with generics substituted.
   */
  public ExpressionResultString substituteGenerics(
      final ExpressionResultString expressionResultString) {
    if (expressionResultString == ExpressionResultString.UNDEFINED) {
      // Nothing to substitute.
      return ExpressionResultString.UNDEFINED;
    }

    return expressionResultString.stream()
        .map(this::substituteGenerics)
        .collect(ExpressionResultString.COLLECTOR);
  }

  /**
   * Substitute generics for {@link TypeString}.
   *
   * @param typeString {@link TypeString} to rebuild.
   * @return {@link TypeString} with generics substituted.
   */
  public TypeString substituteGenerics(final TypeString typeString) {
    if (typeString == TypeString.UNDEFINED) {
      return TypeString.UNDEFINED;
    }

    final Map<TypeString, TypeString> genericTypeMapping = this.getGenericReferenceTypeMapping();
    final TypeString newTypeString = genericTypeMapping.getOrDefault(typeString, typeString);
    if (newTypeString.isCombined()) {
      final TypeString[] newTypeStrings =
          newTypeString.getCombinedTypes().stream()
              .map(this::substituteGenerics)
              .toList()
              .toArray(TypeString[]::new);
      return TypeString.ofCombination(newTypeString.getPakkage(), newTypeStrings);
    }

    if (newTypeString.isGenericDefinition()) {
      final TypeString genericTypeString = typeString.getGenericType();
      final TypeString newGenericTypeString =
          genericTypeMapping.getOrDefault(genericTypeString, genericTypeString);
      return TypeString.ofGenericDefinition(newTypeString.getIdentifier(), newGenericTypeString);
    }

    final TypeString[] generics =
        typeString.getGenerics().stream()
            .map(this::substituteGenerics)
            .toList()
            .toArray(TypeString[]::new);
    return TypeString.ofIdentifier(
        newTypeString.getIdentifier(), newTypeString.getPakkage(), generics);
  }

  private Map<TypeString, TypeString> getGenericReferenceTypeMapping() {
    return this.type.getGenericDefinitions().stream()
        .collect(
            Collectors.toMap(
                def -> def.getGenericReference(), def -> def.getTypeString().getGenericType()));
  }
}
