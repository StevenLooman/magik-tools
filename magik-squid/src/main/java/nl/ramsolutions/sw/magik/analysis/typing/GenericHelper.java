package nl.ramsolutions.sw.magik.analysis.typing;

import java.util.Map;
import java.util.stream.Collectors;

/** Generic helper. */
public class GenericHelper {

  private final TypeString typeStr;

  /**
   * Constructor.
   *
   * @param typeKeeper TypeKeeper to use.
   * @param typeStr Type to use.
   */
  public GenericHelper(final TypeString typeStr) {
    this.typeStr = typeStr;
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
      return TypeString.ofCombination(newTypeStrings);
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
    return this.typeStr.getGenerics().stream()
        .collect(Collectors.toMap(def -> def.getGenericReference(), def -> def.getGenericType()));
  }
}
