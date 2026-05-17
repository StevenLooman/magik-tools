package nl.ramsolutions.sw.magik.analysis.typing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import org.junit.jupiter.api.Test;

class ExpressionResultStringTest {

  @Test
  void testGetTypeNamesUndefinedRendersSerializedName() {
    assertThat(ExpressionResultString.UNDEFINED.getTypeNames(","))
        .isEqualTo(ExpressionResultString.UNDEFINED_SERIALIZED_NAME);
  }

  @Test
  void testGetTypeNamesVariadic() {
    final ExpressionResultString result =
        new ExpressionResultString(TypeString.ofVariadic(TypeString.SW_INTEGER));
    assertThat(result.getTypeNames(",")).isEqualTo("sw:integer...");
  }

  @Test
  void testGetTypeNamesLeadingThenVariadic() {
    final ExpressionResultString result =
        new ExpressionResultString(
            TypeString.SW_SYMBOL, TypeString.ofVariadic(TypeString.SW_INTEGER));
    assertThat(result.getTypeNames(",")).isEqualTo("sw:symbol,sw:integer...");
  }

  @Test
  void testUndefinedAndUndefinedVariadicAreDistinct() {
    final ExpressionResultString variadicUndefined =
        new ExpressionResultString(TypeString.ofVariadic(TypeString.UNDEFINED));
    assertThat(variadicUndefined).isNotEqualTo(ExpressionResultString.UNDEFINED);
  }

  @Test
  void testMaterializeNoVariadic() {
    final ExpressionResultString result =
        new ExpressionResultString(TypeString.SW_INTEGER, TypeString.SW_SYMBOL);
    assertThat(result.materialize(4))
        .containsExactly(
            TypeString.SW_INTEGER, TypeString.SW_SYMBOL, TypeString.SW_UNSET, TypeString.SW_UNSET);
  }

  @Test
  void testMaterializeVariadicTailFillsPositionsWithInnerOrUnset() {
    final ExpressionResultString result =
        new ExpressionResultString(TypeString.ofVariadic(TypeString.SW_INTEGER));
    final TypeString integerOrUnset =
        TypeString.combine(TypeString.SW_INTEGER, TypeString.SW_UNSET);
    assertThat(result.materialize(3))
        .containsExactly(integerOrUnset, integerOrUnset, integerOrUnset);
  }

  @Test
  void testMaterializeLeadingThenVariadic() {
    final ExpressionResultString result =
        new ExpressionResultString(
            TypeString.SW_SYMBOL, TypeString.ofVariadic(TypeString.SW_INTEGER));
    final TypeString integerOrUnset =
        TypeString.combine(TypeString.SW_INTEGER, TypeString.SW_UNSET);
    assertThat(result.materialize(3))
        .containsExactly(TypeString.SW_SYMBOL, integerOrUnset, integerOrUnset);
  }

  @Test
  void testMaterializeUndefinedConstantYieldsUndefinedPositions() {
    assertThat(ExpressionResultString.UNDEFINED.materialize(3))
        .containsExactly(TypeString.UNDEFINED, TypeString.UNDEFINED, TypeString.UNDEFINED);
  }

  @Test
  void testMaterializeVariadicUndefinedYieldsUndefinedOrUnset() {
    final ExpressionResultString result =
        new ExpressionResultString(TypeString.ofVariadic(TypeString.UNDEFINED));
    final TypeString undefinedOrUnset =
        TypeString.combine(TypeString.UNDEFINED, TypeString.SW_UNSET);
    assertThat(result.materialize(2)).containsExactly(undefinedOrUnset, undefinedOrUnset);
  }

  @Test
  void testMaterializeVariadicAlreadyContainsUnsetIsIdempotent() {
    final TypeString explicitlyNullable =
        TypeString.combine(TypeString.SW_INTEGER, TypeString.SW_UNSET);
    final ExpressionResultString result =
        new ExpressionResultString(TypeString.ofVariadic(explicitlyNullable));
    assertThat(result.materialize(2)).containsExactly(explicitlyNullable, explicitlyNullable);
  }

  @Test
  void testMaterializeShorterThanLeadingTruncates() {
    final ExpressionResultString result =
        new ExpressionResultString(
            TypeString.SW_SYMBOL, TypeString.ofVariadic(TypeString.SW_INTEGER));
    assertThat(result.materialize(1)).containsExactly(TypeString.SW_SYMBOL);
  }

  @Test
  void testConstructorRejectsMoreThanMaxItems() {
    assertThatThrownBy(
            () ->
                new ExpressionResultString(
                    Collections.nCopies(
                        ExpressionResultString.MAX_ITEMS + 1, TypeString.UNDEFINED)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void testConstructorAcceptsExactlyMaxItems() {
    final ExpressionResultString result =
        new ExpressionResultString(
            Collections.nCopies(ExpressionResultString.MAX_ITEMS, TypeString.UNDEFINED));
    assertThat(result.size()).isEqualTo(ExpressionResultString.MAX_ITEMS);
  }

  @Test
  void testMaterializeCapsAtMaxItems() {
    final ExpressionResultString result =
        new ExpressionResultString(TypeString.ofVariadic(TypeString.SW_INTEGER));
    assertThat(result.materialize(ExpressionResultString.MAX_ITEMS + 500))
        .hasSize(ExpressionResultString.MAX_ITEMS);
  }
}
