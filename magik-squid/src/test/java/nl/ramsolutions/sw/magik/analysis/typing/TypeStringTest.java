package nl.ramsolutions.sw.magik.analysis.typing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import nl.ramsolutions.sw.magik.parser.TypeStringParser;
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

  @Test
  void testGetGenericValueBound() {
    final TypeString eDefTypeStr = TypeString.ofGenericDefinition("E", TypeString.SW_INTEGER);
    final TypeString ropeTypeStr = TypeString.ofIdentifier("rope", "sw", eDefTypeStr);
    final TypeString eRefTypeStr = TypeString.ofGenericReference("E");
    final TypeString value = ropeTypeStr.getGenericValue(eRefTypeStr, TypeString.UNDEFINED);
    assertThat(value).isEqualTo(TypeString.SW_INTEGER);
  }

  @Test
  void testGetGenericValueUndeclaredGeneric() {
    final TypeString ropeTypeStr = TypeString.ofIdentifier("rope", "sw");
    final TypeString eRefTypeStr = TypeString.ofGenericReference("E");
    final TypeString value = ropeTypeStr.getGenericValue(eRefTypeStr, TypeString.UNDEFINED);
    assertThat(value).isEqualTo(TypeString.UNDEFINED);
  }

  @Test
  void testGetGenericValueUnboundGeneric() {
    // A bare `<E>` reference declares the generic but binds no value to it.
    final TypeString eRefTypeStr = TypeString.ofGenericReference("E");
    final TypeString methodTableTypeStr =
        TypeString.ofIdentifier("method_table", "sw", eRefTypeStr);
    final TypeString value = methodTableTypeStr.getGenericValue(eRefTypeStr, TypeString.UNDEFINED);
    assertThat(value).isEqualTo(TypeString.UNDEFINED);
  }

  @Test
  void testGetGenericValueNullFallback() {
    final TypeString ropeTypeStr = TypeString.ofIdentifier("rope", "sw");
    final TypeString eRefTypeStr = TypeString.ofGenericReference("E");
    final TypeString value = ropeTypeStr.getGenericValue(eRefTypeStr, null);
    assertThat(value).isNull();
  }

  @Test
  void testWithMergedGenericAddsToBareType() {
    final TypeString bare = TypeString.ofIdentifier("rope", "sw");
    final TypeString merged = bare.withMergedGeneric("E", TypeString.SW_INTEGER);

    assertThat(merged)
        .isEqualTo(
            TypeString.ofIdentifier(
                "rope", "sw", TypeString.ofGenericDefinition("E", TypeString.SW_INTEGER)));
  }

  @Test
  void testWithMergedGenericWidensExisting() {
    final TypeString existing =
        TypeString.ofIdentifier(
            "rope", "sw", TypeString.ofGenericDefinition("E", TypeString.SW_INTEGER));
    final TypeString merged = existing.withMergedGeneric("E", TypeString.SW_SYMBOL);

    final TypeString generic = merged.getGenericDefinition(TypeString.ofGenericReference("E"));
    assertThat(generic.getGenericType())
        .isEqualTo(TypeString.combine(TypeString.SW_INTEGER, TypeString.SW_SYMBOL));
  }

  @Test
  void testWithMergedGenericPreservesSiblings() {
    final TypeString existing =
        TypeString.ofIdentifier(
            "property_list",
            "sw",
            TypeString.ofGenericDefinition("K", TypeString.SW_SYMBOL),
            TypeString.ofGenericDefinition("E", TypeString.SW_INTEGER));
    final TypeString merged = existing.withMergedGeneric("E", TypeString.SW_SYMBOL);

    assertThat(merged.getGenericDefinition(TypeString.ofGenericReference("K")).getGenericType())
        .isEqualTo(TypeString.SW_SYMBOL);
    assertThat(merged.getGenericDefinition(TypeString.ofGenericReference("E")).getGenericType())
        .isEqualTo(TypeString.combine(TypeString.SW_INTEGER, TypeString.SW_SYMBOL));
  }

  @Test
  void testWithoutGenericRemovesNamedGeneric() {
    final TypeString existing =
        TypeString.ofIdentifier(
            "rope", "sw", TypeString.ofGenericDefinition("E", TypeString.SW_INTEGER));
    final TypeString reset = existing.withoutGeneric("E");

    assertThat(reset.hasGenerics()).isFalse();
  }

  @Test
  void testSubstituteTypeRecursesIntoGenericDefinitionValue() {
    final TypeString ropeOfParam =
        TypeString.ofIdentifier(
            "rope", "sw", TypeString.ofGenericDefinition("E", TypeString.ofParameterRef("value")));
    final TypeString substituted =
        ropeOfParam.substituteType(TypeString.ofParameterRef("value"), TypeString.SW_INTEGER);

    assertThat(substituted)
        .isEqualTo(
            TypeString.ofIdentifier(
                "rope", "sw", TypeString.ofGenericDefinition("E", TypeString.SW_INTEGER)));
  }

  @Test
  void testSubstituteTypeLeavesConcreteGenericValueUnchanged() {
    final TypeString ropeOfInt =
        TypeString.ofIdentifier(
            "rope", "sw", TypeString.ofGenericDefinition("E", TypeString.SW_INTEGER));
    final TypeString substituted =
        ropeOfInt.substituteType(TypeString.ofParameterRef("value"), TypeString.SW_SYMBOL);

    assertThat(substituted).isEqualTo(ropeOfInt);
  }

  @Test
  void testSuffixedParameterRefSubstituteTypeIsNoOp() {
    final TypeString ref = TypeString.ofParameterRef("values", TypeString.ofGenericReference("E"));

    // An unrelated substitution must leave the suffixed ref unchanged.
    final TypeString afterUnrelated = ref.substituteType(TypeString.SELF, TypeString.SW_INTEGER);
    assertThat(afterUnrelated).isEqualTo(ref);
    assertThat(afterUnrelated.getReferenceGeneric()).isEqualTo(TypeString.ofGenericReference("E"));

    // Whole-ref substitution must still work.
    final TypeString afterWholeRef = ref.substituteType(ref, TypeString.SW_INTEGER);
    assertThat(afterWholeRef).isEqualTo(TypeString.SW_INTEGER);
  }

  @Test
  void testParameterRefWithGenericReferenceRoundTrips() {
    final TypeString ref = TypeString.ofParameterRef("values", TypeString.ofGenericReference("E"));
    assertThat(ref.isParameterReference()).isTrue();
    assertThat(ref.getFullString()).isEqualTo("_parameter(values)<E>");
    assertThat(ref.getReferenceGeneric()).isEqualTo(TypeString.ofGenericReference("E"));

    final TypeString bare = TypeString.ofParameterRef("values");
    assertThat(bare.getReferenceGeneric()).isNull();
    assertThat(bare.getFullString()).isEqualTo("_parameter(values)");

    // A suffixed ref must not equal the bare ref (distinct getFullString).
    assertThat(ref).isNotEqualTo(bare);
  }

  @Test
  void testSlotRefWithGenericReferenceRoundTrips() {
    final TypeString ref = TypeString.ofSlotRef("a_slot", TypeString.ofGenericReference("E"));
    assertThat(ref.isSlotReference()).isTrue();
    assertThat(ref.getFullString()).isEqualTo("_slot(a_slot)<E>");
    assertThat(ref.getReferenceGeneric()).isEqualTo(TypeString.ofGenericReference("E"));

    final TypeString bare = TypeString.ofSlotRef("a_slot");
    assertThat(bare.getReferenceGeneric()).isNull();
    assertThat(bare.getFullString()).isEqualTo("_slot(a_slot)");

    // A suffixed ref must not equal the bare ref (distinct getFullString).
    assertThat(ref).isNotEqualTo(bare);
  }

  @Test
  void testCollectParameterRefsWithGenericFindsNestedRef() {
    // sw:rope<E=_parameter(other)<E>> — suffixed ref buried in a generic definition value.
    final TypeString suffixedRef =
        TypeString.ofParameterRef("other", TypeString.ofGenericReference("E"));
    final TypeString ropeWithRef =
        TypeString.ofIdentifier("rope", "sw", TypeString.ofGenericDefinition("E", suffixedRef));
    assertThat(ropeWithRef.collectParameterRefsWithGeneric()).containsExactly(suffixedRef);
  }

  @Test
  void testCollectParameterRefsWithGenericIgnoresBareRef() {
    // sw:rope<E=_parameter(other)> — bare parameter ref (no suffix) must not be collected.
    final TypeString bareRef = TypeString.ofParameterRef("other");
    final TypeString ropeWithBare =
        TypeString.ofIdentifier("rope", "sw", TypeString.ofGenericDefinition("E", bareRef));
    assertThat(ropeWithBare.collectParameterRefsWithGeneric()).isEmpty();
  }

  @Test
  void testCollectParameterRefsWithGenericWorksOnVariadic() {
    // A variadic wrapping a type with a suffixed ref inside.
    final TypeString suffixedRef =
        TypeString.ofParameterRef("x", TypeString.ofGenericReference("E"));
    final TypeString inner =
        TypeString.ofIdentifier("rope", "sw", TypeString.ofGenericDefinition("E", suffixedRef));
    final TypeString variadic = TypeString.ofVariadic(inner);
    assertThat(variadic.collectParameterRefsWithGeneric()).containsExactly(suffixedRef);
  }

  @Test
  void testResultProjectionAsTrailingUnionMemberParsesBack() {
    // Trailing position exercises the second `firstOf` list; a rule registered in only the first
    // list makes the member parse to nothing at all rather than raising a syntax error.
    final TypeString parsed = TypeStringParser.parseTypeString("sw:unset|_parameter(a_proc)<R>");
    final TypeString resultsReference = TypeString.ofGenericReference(TypeString.SIGNATURE_RESULTS);
    final TypeString projection = TypeString.ofParameterRef("a_proc", resultsReference);
    final TypeString expected = TypeString.combine(TypeString.SW_UNSET, projection);
    assertThat(parsed).isEqualTo(expected);
  }

  @Test
  void testTwoParameterSignatureParsesBack() {
    final TypeString parsed = TypeStringParser.parseTypeString("_invokable<P=[_self,sw:integer]>");
    final TypeString tuple = TypeString.ofTuple(TypeString.SELF, TypeString.SW_INTEGER);
    final TypeString genericDef =
        TypeString.ofGenericDefinition(TypeString.SIGNATURE_PARAMETERS, tuple);
    final TypeString expected = TypeString.ofInvokable(genericDef);
    assertThat(parsed).isEqualTo(expected);
  }

  @Test
  void testGenericParameterSignatureParsesBack() {
    final TypeString parsed = TypeStringParser.parseTypeString("_invokable<P=[<E>]>");
    final TypeString genericRef = TypeString.ofGenericReference("E");
    final TypeString tuple = TypeString.ofTuple(genericRef);
    final TypeString genericDef =
        TypeString.ofGenericDefinition(TypeString.SIGNATURE_PARAMETERS, tuple);
    final TypeString expected = TypeString.ofInvokable(genericDef);
    assertThat(parsed).isEqualTo(expected);
  }

  @Test
  void testSubstituteTypeReachesIntoSignatureParameters() {
    final TypeString tuple = TypeString.ofTuple(TypeString.SELF);
    final TypeString genericDef =
        TypeString.ofGenericDefinition(TypeString.SIGNATURE_PARAMETERS, tuple);
    final TypeString invokable = TypeString.ofInvokable(genericDef);
    final TypeString fooRef = TypeString.ofIdentifier("foo", "sw");
    final TypeString substituted = invokable.substituteType(TypeString.SELF, fooRef);
    final String fullString = substituted.getFullString();
    assertThat(fullString).isEqualTo("_invokable<P=[sw:foo]>");
  }

  @Test
  void testCollectSignatureProjectionsFindsProjectionInParameters() {
    // Signature parameters survive generic substitution, so the collector must reach into them or
    // the sweep is not total and a marker escapes.
    final TypeString resultsReference = TypeString.ofGenericReference(TypeString.SIGNATURE_RESULTS);
    final TypeString projection = TypeString.ofParameterRef("a_proc", resultsReference);
    final TypeString tuple = TypeString.ofTuple(projection);
    final TypeString genericDef =
        TypeString.ofGenericDefinition(TypeString.SIGNATURE_PARAMETERS, tuple);
    final TypeString invokable = TypeString.ofInvokable(genericDef);
    final Set<TypeString> collected = invokable.collectSignatureProjections();
    assertThat(collected).containsExactly(projection);
  }

  @Test
  void testSelfWithGenericPrintsGeneric() {
    final TypeString resultsReference = TypeString.ofGenericReference(TypeString.SIGNATURE_RESULTS);
    final TypeString value = TypeString.ofParameterRef("function", resultsReference);
    final TypeString selfWithE = TypeString.SELF.withGenericDefinition("E", value);
    final String full = selfWithE.getFullString();
    assertThat(full).isEqualTo("_self<E=_parameter(function)<R>>");
  }

  @Test
  void testBareSelfStillPrintsBare() {
    final String full = TypeString.SELF.getFullString();
    assertThat(full).isEqualTo("_self");
  }

  @Test
  void testWithGenericDefinitionReplacesRatherThanUnions() {
    final TypeString ropeWithInteger =
        TypeString.ofIdentifier(
            "rope", "sw", TypeString.ofGenericDefinition("E", TypeString.SW_INTEGER));
    final TypeString redefined = ropeWithInteger.withGenericDefinition("E", TypeString.SW_SYMBOL);
    final TypeString eDef = redefined.getGenericDefinition(TypeString.ofGenericReference("E"));
    final TypeString eType = eDef.getGenericType();
    assertThat(eType).isEqualTo(TypeString.SW_SYMBOL);
  }

  @Test
  void testSelfRebindOverridesElementGeneric() {
    final TypeString resultsReference = TypeString.ofGenericReference(TypeString.SIGNATURE_RESULTS);
    final TypeString projection = TypeString.ofParameterRef("function", resultsReference);
    final TypeString selfWithE = TypeString.SELF.withGenericDefinition("E", projection);
    final TypeString receiver =
        TypeString.ofIdentifier(
            "rope", "sw", TypeString.ofGenericDefinition("E", TypeString.SW_INTEGER));
    final TypeString rebound = selfWithE.substituteType(TypeString.SELF, receiver);
    final TypeString expected =
        TypeString.ofIdentifier("rope", "sw", TypeString.ofGenericDefinition("E", projection));
    assertThat(rebound).isEqualTo(expected);
  }

  @Test
  void testSelfRebindPreservesOtherGenerics() {
    final TypeString resultsReference = TypeString.ofGenericReference(TypeString.SIGNATURE_RESULTS);
    final TypeString projection = TypeString.ofParameterRef("function", resultsReference);
    final TypeString selfWithE = TypeString.SELF.withGenericDefinition("E", projection);
    final TypeString receiver =
        TypeString.ofIdentifier(
            "property_list",
            "sw",
            TypeString.ofGenericDefinition("K", TypeString.SW_SYMBOL),
            TypeString.ofGenericDefinition("E", TypeString.SW_INTEGER));
    final TypeString rebound = selfWithE.substituteType(TypeString.SELF, receiver);
    final TypeString kDef = rebound.getGenericDefinition(TypeString.ofGenericReference("K"));
    final TypeString eDef = rebound.getGenericDefinition(TypeString.ofGenericReference("E"));
    final TypeString kType = kDef.getGenericType();
    assertThat(kType).isEqualTo(TypeString.SW_SYMBOL);
    final TypeString eType = eDef.getGenericType();
    assertThat(eType).isEqualTo(projection);
  }

  @Test
  void testOfTupleRejectsNestedTuple() {
    final TypeString inner = TypeString.ofTuple(TypeString.SW_INTEGER);
    assertThatThrownBy(() -> TypeString.ofTuple(inner))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void testOfTupleRejectsNonTailVariadic() {
    final TypeString variadic = TypeString.ofVariadic(TypeString.SW_INTEGER);
    assertThatThrownBy(() -> TypeString.ofTuple(variadic, TypeString.SW_FLOAT))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void testTupleExpressionResultStringRoundTrip() {
    final TypeString tuple = TypeString.ofTuple(TypeString.SW_INTEGER, TypeString.SW_FLOAT);
    final ExpressionResultString ers = tuple.toExpressionResultString();
    final TypeString back = TypeString.ofExpressionResultString(ers);
    assertThat(back).isEqualTo(tuple);
  }

  @Test
  void testTupleSubstitutesElements() {
    final TypeString genericRef = TypeString.ofGenericReference("E");
    final TypeString tuple = TypeString.ofTuple(genericRef);
    final TypeString substituted = tuple.substituteType(genericRef, TypeString.SW_INTEGER);
    final List<TypeString> elements = substituted.getTupleTypes();
    assertThat(elements).containsExactly(TypeString.SW_INTEGER);
  }

  @Test
  void testOfInvokableBuildsSignature() {
    final TypeString tuple = TypeString.ofTuple(TypeString.SW_INTEGER);
    final TypeString genericDef = TypeString.ofGenericDefinition("P", tuple);
    final TypeString invokable = TypeString.ofInvokable(genericDef);
    final String fullString = invokable.getFullString();
    assertThat(fullString).isEqualTo("_invokable<P=[sw:integer]>");
  }

  @Test
  void testGetSignatureTypesAbsent() {
    final TypeString bare = TypeString.INVOKABLE;
    final List<TypeString> types = bare.getSignatureTypes(TypeString.SIGNATURE_PARAMETERS);
    assertThat(types).isNull();
  }

  @Test
  void testGetSignatureTypesTuple() {
    final TypeString tuple = TypeString.ofTuple(TypeString.SW_INTEGER, TypeString.SW_FLOAT);
    final TypeString genericDef = TypeString.ofGenericDefinition("P", tuple);
    final TypeString invokable = TypeString.ofInvokable(genericDef);
    final List<TypeString> types = invokable.getSignatureTypes(TypeString.SIGNATURE_PARAMETERS);
    assertThat(types).containsExactly(TypeString.SW_INTEGER, TypeString.SW_FLOAT);
  }

  @Test
  void testGetSignatureTypesEmptyTupleIsEmptyNotNull() {
    final TypeString emptyTuple = TypeString.ofTuple();
    final TypeString genericDef = TypeString.ofGenericDefinition("P", emptyTuple);
    final TypeString invokable = TypeString.ofInvokable(genericDef);
    final List<TypeString> types = invokable.getSignatureTypes(TypeString.SIGNATURE_PARAMETERS);
    assertThat(types).isNotNull();
    assertThat(types).isEmpty();
  }

  @Test
  void testGetSignatureTypesSingleValueSugar() {
    final TypeString genericDef = TypeString.ofGenericDefinition("R", TypeString.SW_INTEGER);
    final TypeString invokable = TypeString.ofInvokable(genericDef);
    final List<TypeString> types = invokable.getSignatureTypes(TypeString.SIGNATURE_RESULTS);
    assertThat(types).containsExactly(TypeString.SW_INTEGER);
  }

  @Test
  void testSelfRebindHandlesUnionReceiver() {
    final TypeString resultsReference = TypeString.ofGenericReference(TypeString.SIGNATURE_RESULTS);
    final TypeString projection = TypeString.ofParameterRef("function", resultsReference);
    final TypeString selfWithE = TypeString.SELF.withGenericDefinition("E", projection);
    final TypeString ropeMember =
        TypeString.ofIdentifier(
            "rope", "sw", TypeString.ofGenericDefinition("E", TypeString.SW_INTEGER));
    final TypeString svMember =
        TypeString.ofIdentifier(
            "simple_vector", "sw", TypeString.ofGenericDefinition("E", TypeString.SW_SYMBOL));
    final TypeString receiver = TypeString.combine(ropeMember, svMember);
    final TypeString rebound = selfWithE.substituteType(TypeString.SELF, receiver);
    final TypeString expectedRope =
        TypeString.ofIdentifier("rope", "sw", TypeString.ofGenericDefinition("E", projection));
    final TypeString expectedSv =
        TypeString.ofIdentifier(
            "simple_vector", "sw", TypeString.ofGenericDefinition("E", projection));
    final TypeString expected = TypeString.combine(expectedRope, expectedSv);
    assertThat(rebound).isEqualTo(expected);
  }
}
