package nl.ramsolutions.sw.magik.analysis.typing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GenericHelperTest {

  @Test
  void testSubstituteGenericsResolvesVariadicInner() {
    final TypeString boundType =
        TypeString.ofIdentifier(
            "stack", "sw", TypeString.ofGenericDefinition("E", TypeString.SW_INTEGER));
    final GenericHelper helper = new GenericHelper(boundType);

    final TypeString variadicWithGenericRef =
        TypeString.ofVariadic(TypeString.ofGenericReference("E"));

    final TypeString result = helper.substituteGenerics(variadicWithGenericRef);

    assertThat(result.isVariadic()).isTrue();
    assertThat(result.getVariadicInner()).isEqualTo(TypeString.SW_INTEGER);
  }

  @Test
  void testSubstituteGenericsOnVariadicOfGenericMappedToVariadic() {
    final TypeString variadicInteger = TypeString.ofVariadic(TypeString.SW_INTEGER);
    final TypeString boundType =
        TypeString.ofIdentifier(
            TypeString.SW_SIMPLE_VECTOR.getIdentifier(),
            TypeString.SW_SIMPLE_VECTOR.getPakkage(),
            TypeString.ofGenericDefinition("E", variadicInteger));
    final GenericHelper helper = new GenericHelper(boundType);

    final TypeString variadicOfGenericRef =
        TypeString.ofVariadic(TypeString.ofGenericReference("E"));

    // Must not throw.
    final TypeString result = helper.substituteGenerics(variadicOfGenericRef);

    assertThat(result.isVariadic()).isTrue();
    assertThat(result.getVariadicInner()).isEqualTo(TypeString.SW_INTEGER);
  }

  @Test
  void testSubstituteGenericsHandlesGenericMappedToVariadic() {
    final TypeString variadicInteger = TypeString.ofVariadic(TypeString.SW_INTEGER);
    final TypeString boundType =
        TypeString.ofIdentifier(
            TypeString.SW_SIMPLE_VECTOR.getIdentifier(),
            TypeString.SW_SIMPLE_VECTOR.getPakkage(),
            TypeString.ofGenericDefinition("E", variadicInteger));
    final GenericHelper helper = new GenericHelper(boundType);

    final TypeString genericRef = TypeString.ofGenericReference("E");
    final TypeString result = helper.substituteGenerics(genericRef);

    assertThat(result.isVariadic()).isTrue();
    assertThat(result.getVariadicInner()).isEqualTo(TypeString.SW_INTEGER);
  }

  @Test
  void testSubstituteGenericsDuplicateGenericDefinitions() {
    final TypeString boundType =
        TypeString.ofIdentifier(
            "rope",
            "sw",
            TypeString.ofGenericDefinition("E", TypeString.SW_INTEGER),
            TypeString.ofGenericDefinition("E", TypeString.SW_FLOAT));
    final GenericHelper helper = new GenericHelper(boundType);

    final TypeString result = helper.substituteGenerics(TypeString.ofGenericReference("E"));

    assertThat(result).isEqualTo(TypeString.SW_INTEGER);
  }
}
