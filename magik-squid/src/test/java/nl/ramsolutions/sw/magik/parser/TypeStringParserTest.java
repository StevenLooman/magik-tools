package nl.ramsolutions.sw.magik.parser;

import static org.assertj.core.api.Assertions.assertThat;

import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import org.junit.jupiter.api.Test;

/** Tests for TypeStringParser. */
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
  void testParameterRef() {
    final String typeStr = "_parameter(p1)";
    final TypeString typeString = TypeStringParser.parseTypeString(typeStr, SW_PACKAGE);
    assertThat(typeString).isEqualTo(TypeString.ofParameterRef("p1"));
  }

  @Test
  void testGenericReference() {
    final String typeStr = "<E>";
    final TypeString typeString = TypeStringParser.parseTypeString(typeStr, SW_PACKAGE);
    assertThat(typeString).isEqualTo(TypeString.ofGenericReference("E"));
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
  void testCombined() {
    final String typeStr = "sw:integer|sw:float";
    final TypeString typeString = TypeStringParser.parseTypeString(typeStr, SW_PACKAGE);
    assertThat(typeString)
        .isEqualTo(TypeString.ofCombination(TypeString.SW_INTEGER, TypeString.SW_FLOAT));
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
}
