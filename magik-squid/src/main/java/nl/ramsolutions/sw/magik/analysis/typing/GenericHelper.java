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

    // A reference's suffix is a projection resolved later, never a receiver generic to expand.
    // No reference is ever a mapping key, the keys all being generic references.
    if (typeString.isParameterReference() || typeString.isSlotReference()) {
      return typeString;
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

    if (typeString.isTuple()) {
      final TypeString[] substitutedElements =
          typeString.getTupleTypes().stream()
              .map(this::substituteGenerics)
              .toArray(TypeString[]::new);
      return TypeString.ofTuple(substitutedElements);
    }

    final Map<TypeString, TypeString> genericTypeMapping = this.getGenericReferenceTypeMapping();
    final TypeString newTypeString = genericTypeMapping.getOrDefault(typeString, typeString);
    // A bound value is the receiver's own and already concrete: keep it whole, union or not.
    if (typeString.isGenericReference()) {
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
      return this.substituteGenericDefinition(typeString, newTypeString, genericTypeMapping);
    }

    final TypeString[] generics =
        typeString.getGenerics().stream()
            .map(generic -> this.substituteGenericEntry(generic, genericTypeMapping))
            .toList()
            .toArray(TypeString[]::new);
    final String identifier = newTypeString.getIdentifier();
    final String pakkage = newTypeString.getPakkage();
    return TypeString.ofIdentifier(identifier, pakkage, generics);
  }

  private TypeString substituteGenericDefinition(
      final TypeString typeString,
      final TypeString newTypeString,
      final Map<TypeString, TypeString> genericTypeMapping) {
    final TypeString genericTypeString = typeString.getGenericType();
    final TypeString boundTypeString = genericTypeMapping.get(genericTypeString);
    // A bound value is the receiver's own, already concrete; re-substituting it never terminates
    // when it references the very generic it binds (sw:rope<E=sw:rope<E=<E>>>).
    final TypeString newGenericTypeString =
        boundTypeString != null ? boundTypeString : this.substituteGenerics(genericTypeString);
    final String identifier = newTypeString.getIdentifier();
    return TypeString.ofGenericDefinition(identifier, newGenericTypeString);
  }

  private TypeString substituteGenericEntry(
      final TypeString generic, final Map<TypeString, TypeString> genericTypeMapping) {
    // A bound bare reference in a generics list becomes a binding, as an entry must be one.
    final TypeString boundTypeString = genericTypeMapping.get(generic);
    if (boundTypeString != null) {
      final String identifier = generic.getIdentifier();
      return TypeString.ofGenericDefinition(identifier, boundTypeString);
    }

    return this.substituteGenerics(generic);
  }

  private Map<TypeString, TypeString> getGenericReferenceTypeMapping() {
    return this.typeStr.getGenerics().stream()
        .filter(TypeString::isGenericDefinition)
        .collect(
            Collectors.toMap(
                TypeString::getGenericReference, TypeString::getGenericType, (a, b) -> a));
  }
}
