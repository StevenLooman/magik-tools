package nl.ramsolutions.sw.magik.analysis.typing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GenericHelperTest {

  @Test
  void testSubstituteGenericsSubstitutesIntoSignatureParameters() {
    final TypeString boundType =
        TypeString.ofIdentifier(
            "rope", "sw", TypeString.ofGenericDefinition("E", TypeString.SW_INTEGER));
    final GenericHelper helper = new GenericHelper(boundType);

    final TypeString genericRef = TypeString.ofGenericReference("E");
    final TypeString callbackTuple = TypeString.ofTuple(genericRef);
    final TypeString callbackDef =
        TypeString.ofGenericDefinition(TypeString.SIGNATURE_PARAMETERS, callbackTuple);
    final TypeString callback = TypeString.ofInvokable(callbackDef);

    final TypeString result = helper.substituteGenerics(callback);

    final String fullString = result.getFullString();
    assertThat(fullString).isEqualTo("_invokable<P=[sw:integer]>");
  }

  @Test
  void testSubstituteGenericsTerminatesOnSelfReferentialBinding() {
    final TypeString genericRef = TypeString.ofGenericReference("E");
    final TypeString innerRope =
        TypeString.ofIdentifier("rope", "sw", TypeString.ofGenericDefinition("E", genericRef));
    final TypeString boundType =
        TypeString.ofIdentifier("rope", "sw", TypeString.ofGenericDefinition("E", innerRope));
    final GenericHelper helper = new GenericHelper(boundType);

    final TypeString result = helper.substituteGenerics(innerRope);

    assertThat(result).isEqualTo(boundType);
  }

  @Test
  void testSubstituteGenericsLeavesProjectionSuffixWhenReceiverDeclaresSameGeneric() {
    final TypeString resultsDefinition =
        TypeString.ofGenericDefinition(TypeString.SIGNATURE_RESULTS, TypeString.SW_INTEGER);
    final TypeString boundType = TypeString.ofIdentifier("matrix", "sw", resultsDefinition);
    final GenericHelper helper = new GenericHelper(boundType);
    final TypeString resultsRef = TypeString.ofGenericReference(TypeString.SIGNATURE_RESULTS);
    final TypeString projection = TypeString.ofParameterRef("function", resultsRef);

    final TypeString result = helper.substituteGenerics(projection);

    assertThat(result).isEqualTo(projection);
  }

  @Test
  void testSubstituteGenericsLeavesNestedProjectionSuffixWhenReceiverDeclaresSameGeneric() {
    final TypeString resultsDefinition =
        TypeString.ofGenericDefinition(TypeString.SIGNATURE_RESULTS, TypeString.SW_INTEGER);
    final TypeString boundType = TypeString.ofIdentifier("matrix", "sw", resultsDefinition);
    final GenericHelper helper = new GenericHelper(boundType);
    final TypeString resultsRef = TypeString.ofGenericReference(TypeString.SIGNATURE_RESULTS);
    final TypeString projection = TypeString.ofParameterRef("function", resultsRef);
    final TypeString elementDefinition = TypeString.ofGenericDefinition("E", projection);
    final TypeString returnType = TypeString.ofIdentifier("rope", "sw", elementDefinition);

    final TypeString result = helper.substituteGenerics(returnType);

    assertThat(result).isEqualTo(returnType);
  }

  @Test
  void testSubstituteGenericsInsideTupleValue() {
    final TypeString ropeWithGeneric =
        TypeString.ofIdentifier(
            "rope", "sw", TypeString.ofGenericDefinition("E", TypeString.SW_INTEGER));
    final GenericHelper helper = new GenericHelper(ropeWithGeneric);
    final TypeString genericRef = TypeString.ofGenericReference("E");
    final TypeString tuple = TypeString.ofTuple(genericRef);
    final TypeString contract =
        TypeString.ofIdentifier("procedure", "sw", TypeString.ofGenericDefinition("P", tuple));
    final TypeString substituted = helper.substituteGenerics(contract);
    final TypeString expectedTuple = TypeString.ofTuple(TypeString.SW_INTEGER);
    final TypeString expected =
        TypeString.ofIdentifier(
            "procedure", "sw", TypeString.ofGenericDefinition("P", expectedTuple));
    assertThat(substituted).isEqualTo(expected);
  }

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
