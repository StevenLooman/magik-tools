package nl.ramsolutions.sw.magik.parser;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import org.junit.jupiter.api.Test;

/** Tests for {@link TypeStringParser}. */
class TypeStringParserTest {

  private static final String SW_PACKAGE = "sw";

  @Test
  void testUndefined() {
    final String typeStr = "_undefined";
    final TypeString typeString = TypeStringParser.parseTypeString(typeStr, SW_PACKAGE);
    assertThat(typeString).isEqualTo(TypeString.UNDEFINED);
  }

  @Test
  void testSelf() {
    final String typeStr = "_self";
    final TypeString typeString = TypeStringParser.parseTypeString(typeStr, SW_PACKAGE);
    assertThat(typeString).isEqualTo(TypeString.SELF);
  }

  @Test
  void testAnonymousIdentifier() {
    // An anonymous procedure is persisted by its full string, so it must parse back to itself.
    final TypeString expected = TypeString.ofIdentifier("_proc__sw__a__b____0", "_anon");
    final String typeStr = expected.getFullString();
    final TypeString typeString = TypeStringParser.parseTypeString(typeStr, SW_PACKAGE);
    assertThat(typeString).isEqualTo(expected);
  }

  @Test
  void testAnonymousIdentifierInCombination() {
    final TypeString anonymous = TypeString.ofIdentifier("_proc__sw__a__b____0", "_anon");
    final TypeString expected = TypeString.ofCombination(anonymous, TypeString.SW_UNSET);
    final String typeStr = expected.getFullString();
    final TypeString typeString = TypeStringParser.parseTypeString(typeStr, SW_PACKAGE);
    assertThat(typeString).isEqualTo(expected);
  }

  @Test
  void testParameterRef() {
    final String typeStr = "_parameter(p1)";
    final TypeString typeString = TypeStringParser.parseTypeString(typeStr, SW_PACKAGE);
    final TypeString paramRef = TypeString.ofParameterRef("p1");
    assertThat(typeString).isEqualTo(paramRef);
  }

  @Test
  void testSlotRef() {
    final String typeStr = "_slot(my_slot)";
    final TypeString typeString = TypeStringParser.parseTypeString(typeStr, SW_PACKAGE);
    final TypeString slotRef = TypeString.ofSlotRef("my_slot");
    assertThat(typeString).isEqualTo(slotRef);
  }

  @Test
  void testGenericReference() {
    final String typeStr = "<E>";
    final TypeString typeString = TypeStringParser.parseTypeString(typeStr, SW_PACKAGE);
    final TypeString genericRef = TypeString.ofGenericReference("E");
    assertThat(typeString).isEqualTo(genericRef);
  }

  @Test
  void testIdentifier() {
    final String typeStr = "sw:integer";
    final TypeString typeString = TypeStringParser.parseTypeString(typeStr, SW_PACKAGE);
    assertThat(typeString).isEqualTo(TypeString.SW_INTEGER);
  }

  @Test
  void testGenericDefinitions() {
    final String typeStr = "sw:rope<E=sw:symbol>";
    final TypeString typeString = TypeStringParser.parseTypeString(typeStr, SW_PACKAGE);
    assertThat(typeString)
        .isEqualTo(
            TypeString.ofIdentifier(
                "rope", SW_PACKAGE, TypeString.ofGenericDefinition("E", TypeString.SW_SYMBOL)));
  }

  @Test
  void testGenericDefinitions2() {
    final String typeStr = "sw:property_list<K=sw:symbol, E=sw:integer>";
    final TypeString typeString = TypeStringParser.parseTypeString(typeStr, SW_PACKAGE);
    assertThat(typeString)
        .isEqualTo(
            TypeString.ofIdentifier(
                "property_list",
                SW_PACKAGE,
                TypeString.ofGenericDefinition("K", TypeString.SW_SYMBOL),
                TypeString.ofGenericDefinition("E", TypeString.SW_INTEGER)));
  }

  @Test
  void testGenericDefinitionsNested() {
    final String typeStr = "sw:property_list<K=sw:symbol, E=sw:rope<E=sw:integer>>";
    final TypeString typeString = TypeStringParser.parseTypeString(typeStr, SW_PACKAGE);
    assertThat(typeString)
        .isEqualTo(
            TypeString.ofIdentifier(
                "property_list",
                SW_PACKAGE,
                TypeString.ofGenericDefinition("K", TypeString.SW_SYMBOL),
                TypeString.ofGenericDefinition(
                    "E",
                    TypeString.ofIdentifier(
                        "rope",
                        SW_PACKAGE,
                        TypeString.ofGenericDefinition("E", TypeString.SW_INTEGER)))));
  }

  @Test
  void testMixedGenericDefinitionsAndReferences() {
    final String typeStr = "sw:property_list<K=sw:symbol,object>";
    final TypeString typeString = TypeStringParser.parseTypeString(typeStr, SW_PACKAGE);
    assertThat(typeString)
        .isEqualTo(
            TypeString.ofIdentifier(
                "property_list",
                SW_PACKAGE,
                TypeString.ofGenericDefinition("K", TypeString.SW_SYMBOL),
                TypeString.ofGenericReference("object")));
  }

  @Test
  void testCombined() {
    final String typeStr = "sw:integer|sw:float";
    final TypeString typeString = TypeStringParser.parseTypeString(typeStr, SW_PACKAGE);
    assertThat(typeString)
        .isEqualTo(TypeString.combine(TypeString.SW_INTEGER, TypeString.SW_FLOAT));
  }

  @Test
  void testSyntaxError() {
    final String typeStr = "_sel";
    final TypeString typeString = TypeStringParser.parseTypeString(typeStr, SW_PACKAGE);
    assertThat(typeString).isEqualTo(TypeString.UNDEFINED);
  }

  @Test
  void testExpressionResultStringSelf() {
    final String exprStr = "_self";
    final ExpressionResultString result =
        TypeStringParser.parseExpressionResultString(exprStr, SW_PACKAGE);
    assertThat(result).isEqualTo(new ExpressionResultString(TypeString.SELF));
  }

  @Test
  void testExpressionResultStringSelf2() {
    final String exprStr = "_self, _self";
    final ExpressionResultString result =
        TypeStringParser.parseExpressionResultString(exprStr, SW_PACKAGE);
    assertThat(result).isEqualTo(new ExpressionResultString(TypeString.SELF, TypeString.SELF));
  }

  @Test
  void testExpressionResultStringSyntaxError() {
    final String exprStr = "_sel, _clon";
    final ExpressionResultString result =
        TypeStringParser.parseExpressionResultString(exprStr, SW_PACKAGE);
    assertThat(result).isEqualTo(new ExpressionResultString(TypeString.UNDEFINED));
  }

  @Test
  void testUndefinedResultString() {
    final String exprStr = "__UNDEFINED_RESULT__";
    final ExpressionResultString result =
        TypeStringParser.parseExpressionResultString(exprStr, SW_PACKAGE);
    assertThat(result).isEqualTo(ExpressionResultString.UNDEFINED);
  }

  @Test
  void testParseVariadicExpressionResult() {
    final ExpressionResultString result =
        TypeStringParser.parseExpressionResultString("sw:integer...", SW_PACKAGE);
    assertThat(result.size()).isEqualTo(1);
    assertThat(result.get(0, null)).isEqualTo(TypeString.ofVariadic(TypeString.SW_INTEGER));
  }

  @Test
  void testParseLeadingThenVariadic() {
    final ExpressionResultString result =
        TypeStringParser.parseExpressionResultString("sw:symbol, sw:integer...", SW_PACKAGE);
    assertThat(result.size()).isEqualTo(2);
    assertThat(result.get(0, null)).isEqualTo(TypeString.SW_SYMBOL);
    assertThat(result.get(1, null)).isEqualTo(TypeString.ofVariadic(TypeString.SW_INTEGER));
  }

  @Test
  void testParseVariadicCombined() {
    final ExpressionResultString result =
        TypeStringParser.parseExpressionResultString("sw:integer|sw:unset...", SW_PACKAGE);
    final TypeString inner = TypeString.combine(TypeString.SW_INTEGER, TypeString.SW_UNSET);
    assertThat(result.size()).isEqualTo(1);
    assertThat(result.get(0, null)).isEqualTo(TypeString.ofVariadic(inner));
  }

  @Test
  void testRoundTripVariadic() {
    final String typeStr = "sw:integer...";
    final ExpressionResultString parsed =
        TypeStringParser.parseExpressionResultString(typeStr, SW_PACKAGE);
    assertThat(parsed.getFullString()).isEqualTo(typeStr);
  }

  @Test
  void testMalformedTrailingDotsDoesNotProduceVariadic() {
    final ExpressionResultString result =
        TypeStringParser.parseExpressionResultString("sw:integer,...", SW_PACKAGE);
    final TypeString last = result.get(result.size() - 1, null);
    if (last != null) {
      assertThat(last.isVariadic()).isFalse();
    }
  }

  @Test
  void testParseLeadingPlusVariadicMultipleElements() {
    final ExpressionResultString result =
        TypeStringParser.parseExpressionResultString(
            "sw:symbol, sw:char16_vector, sw:integer, sw:float...", SW_PACKAGE);
    assertThat(result.size()).isEqualTo(4);
    assertThat(result.get(0, null)).isEqualTo(TypeString.SW_SYMBOL);
    assertThat(result.get(1, null)).isEqualTo(TypeString.SW_CHAR16_VECTOR);
    assertThat(result.get(2, null)).isEqualTo(TypeString.SW_INTEGER);
    assertThat(result.get(3, null)).isEqualTo(TypeString.ofVariadic(TypeString.SW_FLOAT));
    assertThat(result.getFullString())
        .isEqualTo("sw:symbol,sw:char16_vector,sw:integer,sw:float...");
  }

  @Test
  void testParseGenericReferenceVariadic() {
    final ExpressionResultString result =
        TypeStringParser.parseExpressionResultString("<E>...", SW_PACKAGE);
    assertThat(result.size()).isEqualTo(1);
    assertThat(result.get(0, null))
        .isEqualTo(TypeString.ofVariadic(TypeString.ofGenericReference("E")));
  }

  @Test
  void testParseVariadicInsideGenericIsNotVariadic() {
    final TypeString typeString =
        TypeStringParser.parseTypeString("sw:rope<E=sw:integer...>", SW_PACKAGE);
    if (!typeString.isUndefined() && typeString.hasGenerics()) {
      final TypeString gen = typeString.getGenericDefinition(TypeString.ofGenericReference("E"));
      if (gen != null && gen.getGenericType() != null) {
        assertThat(gen.getGenericType().isVariadic()).isFalse();
      }
    }
  }

  @Test
  void testParseParameterRefWithGenericReference() {
    final TypeString parsed = TypeStringParser.parseTypeString("_parameter(values)<E>", "sw");
    assertThat(parsed)
        .isEqualTo(TypeString.ofParameterRef("values", TypeString.ofGenericReference("E")));
    assertThat(parsed.getReferenceGeneric()).isEqualTo(TypeString.ofGenericReference("E"));
  }

  @Test
  void testParseSlotRefWithGenericReference() {
    final TypeString parsed = TypeStringParser.parseTypeString("_slot(a_slot)<E>", "sw");
    assertThat(parsed)
        .isEqualTo(TypeString.ofSlotRef("a_slot", TypeString.ofGenericReference("E")));
    assertThat(parsed.getReferenceGeneric()).isEqualTo(TypeString.ofGenericReference("E"));
  }

  @Test
  void testParseVariadicInsideCombinedIsNotVariadic() {
    final TypeString typeString =
        TypeStringParser.parseTypeString("sw:integer... | sw:symbol", SW_PACKAGE);
    assertThat(typeString.isVariadic()).isFalse();
  }

  @Test
  void testParseVariadicNonTailInExpressionResultIsWellFormed() {
    final ExpressionResultString result =
        TypeStringParser.parseExpressionResultString("sw:integer..., sw:symbol", SW_PACKAGE);
    for (int i = 0; i < result.size() - 1; ++i) {
      assertThat(result.get(i, null).isVariadic())
          .as("non-last entry at index %d must not be variadic", i)
          .isFalse();
    }
  }

  @Test
  void testParseWhitespaceAroundVariadicDots() {
    final ExpressionResultString result =
        TypeStringParser.parseExpressionResultString("sw:integer ...", SW_PACKAGE);
    assertThat(result.get(0, null)).isEqualTo(TypeString.ofVariadic(TypeString.SW_INTEGER));
  }

  @Test
  void testDeclaredGenericsRoundTrip() {
    final TypeString original =
        TypeString.ofIdentifier(
            "rope",
            "sw",
            TypeString.ofGenericReference("E"),
            TypeString.ofGenericDefinition("K", TypeString.SW_INTEGER));
    final TypeString roundTripped =
        TypeStringParser.parseTypeString(original.getFullString(), SW_PACKAGE);
    assertThat(roundTripped).isEqualTo(original);
  }

  @Test
  void testParseSelfWithGeneric() {
    final TypeString parsed = TypeStringParser.parseTypeString("_self<E=sw:integer>");
    final TypeString expected = TypeString.SELF.withGenericDefinition("E", TypeString.SW_INTEGER);
    assertThat(parsed).isEqualTo(expected);
  }

  @Test
  void testTupleAsGenericValue() {
    final String typeStr = "sw:procedure<P=[sw:integer, sw:float]>";
    final TypeString typeString = TypeStringParser.parseTypeString(typeStr, SW_PACKAGE);
    final List<TypeString> generics = typeString.getGenerics();
    assertThat(generics).hasSize(1);
    final TypeString genericDef = generics.get(0);
    final TypeString value = genericDef.getGenericType();
    final boolean isTuple = value.isTuple();
    assertThat(isTuple).isTrue();
    final List<TypeString> elements = value.getTupleTypes();
    assertThat(elements).containsExactly(TypeString.SW_INTEGER, TypeString.SW_FLOAT);
  }

  @Test
  void testEmptyTupleDistinctFromAbsent() {
    final String typeStr = "sw:procedure<P=[]>";
    final TypeString typeString = TypeStringParser.parseTypeString(typeStr, SW_PACKAGE);
    final List<TypeString> generics = typeString.getGenerics();
    final TypeString genericDef = generics.get(0);
    final TypeString value = genericDef.getGenericType();
    final boolean isTuple = value.isTuple();
    assertThat(isTuple).isTrue();
    final List<TypeString> elements = value.getTupleTypes();
    assertThat(elements).isEmpty();
  }

  @Test
  void testTupleVariadicTail() {
    final String typeStr = "sw:procedure<P=[sw:symbol, sw:object...]>";
    final TypeString typeString = TypeStringParser.parseTypeString(typeStr, SW_PACKAGE);
    final List<TypeString> generics = typeString.getGenerics();
    final TypeString genericDef = generics.get(0);
    final TypeString value = genericDef.getGenericType();
    final List<TypeString> elements = value.getTupleTypes();
    assertThat(elements).hasSize(2);
    final TypeString tail = elements.get(1);
    final boolean tailVariadic = tail.isVariadic();
    assertThat(tailVariadic).isTrue();
  }

  @Test
  void testTupleVariadicTailRoundTrips() {
    final String typeStr = "sw:procedure<P=[sw:symbol, sw:object...]>";
    final TypeString typeString = TypeStringParser.parseTypeString(typeStr, SW_PACKAGE);
    final String fullString = typeString.getFullString();
    assertThat(fullString).isEqualTo("sw:procedure<P=[sw:symbol,sw:object...]>");
    final TypeString reparsed = TypeStringParser.parseTypeString(fullString, SW_PACKAGE);
    assertThat(reparsed).isEqualTo(typeString);
  }

  @Test
  void testTupleRoundTripsOrdered() {
    final String typeStr = "sw:procedure<P=[sw:integer, sw:float]>";
    final TypeString typeString = TypeStringParser.parseTypeString(typeStr, SW_PACKAGE);
    final String fullString = typeString.getFullString();
    assertThat(fullString).isEqualTo("sw:procedure<P=[sw:integer,sw:float]>");
    final TypeString reparsed = TypeStringParser.parseTypeString(fullString, SW_PACKAGE);
    assertThat(reparsed).isEqualTo(typeString);
  }

  @Test
  void testInvokableBare() {
    final TypeString typeString = TypeStringParser.parseTypeString("_invokable", SW_PACKAGE);
    final boolean isInvokable = typeString.isInvokable();
    assertThat(isInvokable).isTrue();
    final boolean hasGenerics = typeString.hasGenerics();
    assertThat(hasGenerics).isFalse();
  }

  @Test
  void testInvokableWithSignature() {
    final String typeStr = "_invokable<P=[sw:integer], R=[sw:float]>";
    final TypeString typeString = TypeStringParser.parseTypeString(typeStr, SW_PACKAGE);
    final boolean isInvokable = typeString.isInvokable();
    assertThat(isInvokable).isTrue();
    final String fullString = typeString.getFullString();
    assertThat(fullString).isEqualTo("_invokable<P=[sw:integer],R=[sw:float]>");
  }

  @Test
  void testInvocableTypoFailsParse() {
    final TypeString typeString = TypeStringParser.parseTypeString("_invocable", SW_PACKAGE);
    assertThat(typeString).isEqualTo(TypeString.UNDEFINED);
  }

  @Test
  void testInvokableAsTrailingUnionMember() {
    final TypeString parsed = TypeStringParser.parseTypeString("sw:unset|_invokable", SW_PACKAGE);
    final TypeString expected = TypeString.combine(TypeString.SW_UNSET, TypeString.INVOKABLE);
    assertThat(parsed).isEqualTo(expected);
  }

  @Test
  void testTrailingGarbageDoesNotSilentlyDropTheTail() {
    final TypeString typeString = TypeStringParser.parseTypeString("sw:integer bogus", SW_PACKAGE);
    assertThat(typeString).isEqualTo(TypeString.UNDEFINED);
  }

  @Test
  void testExpressionResultTrailingGarbageDoesNotSilentlyDropTheTail() {
    final ExpressionResultString result =
        TypeStringParser.parseExpressionResultString("sw:integer bogus", SW_PACKAGE);
    assertThat(result).isEqualTo(ExpressionResultString.UNDEFINED);
  }

  @Test
  void testSurroundingWhitespaceStillParses() {
    final TypeString typeString = TypeStringParser.parseTypeString(" sw:integer ", SW_PACKAGE);
    assertThat(typeString).isEqualTo(TypeString.SW_INTEGER);
  }

  @Test
  void testExpressionResultSurroundingWhitespaceStillParses() {
    final ExpressionResultString result =
        TypeStringParser.parseExpressionResultString(" sw:integer, sw:float ", SW_PACKAGE);
    final ExpressionResultString expected =
        new ExpressionResultString(TypeString.SW_INTEGER, TypeString.SW_FLOAT);
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void testCallArgumentsNoLongerParse() {
    final TypeString typeString =
        TypeStringParser.parseTypeString("sw:procedure(_self)", SW_PACKAGE);
    assertThat(typeString).isEqualTo(TypeString.UNDEFINED);
  }

  @Test
  void testResultMarkerNoLongerParses() {
    final TypeString typeString = TypeStringParser.parseTypeString("_result(a_proc)", SW_PACKAGE);
    assertThat(typeString).isEqualTo(TypeString.UNDEFINED);
  }
}
