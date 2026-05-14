package nl.ramsolutions.sw.magik.analysis.typing;

import java.util.Map;
import java.util.stream.Collectors;

/** Generic helper. */
public class GenericHelper {

  private final TypeString typeStr;

  /**
   * Constructor.
   *
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
    if (expressionResultString.equals(ExpressionResultString.UNDEFINED)) {
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
    if (typeString.isUndefined()) {
      return TypeString.UNDEFINED;
    }

    if (typeString.isVariadic()) {
      final TypeString substitutedInner = this.substituteGenerics(typeString.getVariadicInner());
      // If the substituted inner is itself variadic (because a generic ref was bound to
      // a variadic, e.g. simple_vector<E=variadic(T)> from a misused @param), don't
      // re-wrap — that would violate the variadic-cannot-wrap-variadic invariant.
      if (substitutedInner.isVariadic()) {
        return substitutedInner;
      }

      return TypeString.ofVariadic(substitutedInner);
    }

    final Map<TypeString, TypeString> genericTypeMapping = this.getGenericReferenceTypeMapping();
    final TypeString newTypeString = genericTypeMapping.getOrDefault(typeString, typeString);
    // If a generic reference resolves to a variadic — possible when a _gather parameter
    // was declared with a variadic @param doc, producing simple_vector<E=variadic(T)> —
    // propagate the variadic as-is rather than falling through to ofIdentifier which
    // would call getIdentifier() and throw.
    if (newTypeString.isVariadic()) {
      return newTypeString;
    }

    if (newTypeString.isCombined()) {
      final TypeString[] newTypeStrings =
          newTypeString.getCombinedTypes().stream()
              .map(this::substituteGenerics)
              .toList()
              .toArray(TypeString[]::new);
      return TypeString.combine(newTypeStrings);
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
        .filter(TypeString::isGenericDefinition)
        .collect(Collectors.toMap(TypeString::getGenericReference, TypeString::getGenericType));
  }
}
