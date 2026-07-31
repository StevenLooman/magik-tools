package nl.ramsolutions.sw.magik.analysis.typing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TypeStringTest {

  @Test
  void testOfVariadicWrapsInner() {
    final TypeString variadic = TypeString.ofVariadic(TypeString.SW_INTEGER);
    assertThat(variadic.isVariadic()).isTrue();
    assertThat(variadic.getVariadicInner()).isEqualTo(TypeString.SW_INTEGER);
  }

  @Test
  void testOfVariadicRejectsNestedVariadic() {
    final TypeString inner = TypeString.ofVariadic(TypeString.SW_INTEGER);
    assertThatThrownBy(() -> TypeString.ofVariadic(inner))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void testNonVariadicReportsFalse() {
    assertThat(TypeString.SW_INTEGER.isVariadic()).isFalse();
  }

  @Test
  void testGetCombinedTypesOnVariadicDoesNotLeakInner() {
    final TypeString variadic = TypeString.ofVariadic(TypeString.SW_INTEGER);
    assertThat(variadic.getCombinedTypes()).containsExactly(variadic);
  }

  @Test
  void testVariadicGetFullString() {
    final TypeString variadic = TypeString.ofVariadic(TypeString.SW_INTEGER);
    assertThat(variadic.getFullString()).isEqualTo("sw:integer...");
  }

  @Test
  void testVariadicGetFullStringCombinedInner() {
    final TypeString inner = TypeString.combine(TypeString.SW_INTEGER, TypeString.SW_UNSET);
    final TypeString variadic = TypeString.ofVariadic(inner);
    // Combined inner sorts alphabetically; matches existing combined behaviour.
    assertThat(variadic.getFullString()).isEqualTo("sw:integer|sw:unset...");
  }

  @Test
  void testVariadicEquality() {
    final TypeString a = TypeString.ofVariadic(TypeString.SW_INTEGER);
    final TypeString b = TypeString.ofVariadic(TypeString.SW_INTEGER);
    final TypeString c = TypeString.ofVariadic(TypeString.SW_SYMBOL);
    assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    assertThat(a).isNotEqualTo(c);
    assertThat(a).isNotEqualTo(TypeString.SW_INTEGER);
  }

  @Test
  void testVariadicContainsUndefined() {
    assertThat(TypeString.ofVariadic(TypeString.SW_INTEGER).containsUndefined()).isFalse();
    assertThat(TypeString.ofVariadic(TypeString.UNDEFINED).containsUndefined()).isTrue();
  }

  @Test
  void testVariadicIsUndefined() {
    assertThat(TypeString.ofVariadic(TypeString.UNDEFINED).isUndefined()).isFalse();
  }

  @Test
  void testVariadicSubstituteRecursesIntoInner() {
    final TypeString genericRef = TypeString.ofGenericReference("E");
    final TypeString variadic = TypeString.ofVariadic(genericRef);
    final TypeString substituted = variadic.substituteType(genericRef, TypeString.SW_INTEGER);
    assertThat(substituted.isVariadic()).isTrue();
    assertThat(substituted.getVariadicInner()).isEqualTo(TypeString.SW_INTEGER);
  }

  @Test
  void testEqualTypesWithReorderedUnionHaveEqualHashCode() {
    final TypeString ab = TypeString.ofCombination(TypeString.UNDEFINED, TypeString.SW_SYMBOL);
    final TypeString ba = TypeString.ofCombination(TypeString.SW_SYMBOL, TypeString.UNDEFINED);

    assertThat(ab).isEqualTo(ba);
    assertThat(ab.hashCode()).isEqualTo(ba.hashCode());
  }

  @Test
  void testCombineToleratesEqualGenericTypesWithReorderedUnion() {
    final TypeString svA =
        TypeString.ofIdentifier(
            "simple_vector",
            "sw",
            TypeString.ofGenericDefinition(
                "E", TypeString.ofCombination(TypeString.UNDEFINED, TypeString.SW_SYMBOL)));
    final TypeString svB =
        TypeString.ofIdentifier(
            "simple_vector",
            "sw",
            TypeString.ofGenericDefinition(
                "E", TypeString.ofCombination(TypeString.SW_SYMBOL, TypeString.UNDEFINED)));

    assertThat(svA).isEqualTo(svB);
    assertThat(svA.hashCode()).isEqualTo(svB.hashCode());
    assertThatCode(() -> TypeString.combine(svA, svB)).doesNotThrowAnyException();
  }
}
