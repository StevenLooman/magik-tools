package nl.ramsolutions.sw.magik.analysis.typing;

import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.CombinedType;

/**
 * Type matcher used to test if a type matches a type-criterium.
 *
 * <p>This matcher is used to test if a given expression-type matches the paramatere type, e.g.
 */
public final class TypeMatcher {

  private TypeMatcher() {}

  /**
   * Test if type matches criterium.
   *
   * @param type Type to test.
   * @param criterium Criterium to test against.
   * @return True if type matches, false if not.
   */
  public static boolean typeMatches(final AbstractType type, final AbstractType criterium) {
    if (type instanceof CombinedType combinedTypeType) {
      return combinedTypeType.getTypes().stream()
          .allMatch(typeType -> TypeMatcher.typeMatches(typeType, criterium));
    }
    if (criterium instanceof CombinedType combinedTypeCriterium) {
      return combinedTypeCriterium.getTypes().stream()
          .anyMatch(crit -> TypeMatcher.typeMatches(type, crit));
    }
    return TypeMatcher.isKindOf(type, criterium);
  }

  public static boolean isKindOf(final AbstractType type, final AbstractType criterium) {
    return type.isKindOf(criterium);
  }
}
