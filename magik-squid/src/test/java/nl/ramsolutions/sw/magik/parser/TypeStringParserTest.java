package nl.ramsolutions.sw.magik.parser;

import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for TypeStringParser.
 */
class TypeStringParserTest {

    private static final String SW_PACKAGE = "sw";

    @Test
    void testSelf() {
        final String typeStr = "_self";
        final TypeString typeString = TypeStringParser.parseTypeString(typeStr, SW_PACKAGE);
        assertThat(typeString)
            .isEqualTo(TypeString.SELF);
    }

    @Test
    void testParameterRef() {
        final String typeStr = "_parameter(p1)";
        final TypeString typeString = TypeStringParser.parseTypeString(typeStr, SW_PACKAGE);
        assertThat(typeString)
            .isEqualTo(TypeString.ofParameterRef("p1"));
    }

    @Test
    void testGeneric() {
        final String typeStr = "<E>";
        final TypeString typeString = TypeStringParser.parseTypeString(typeStr, SW_PACKAGE);
        assertThat(typeString)
            .isEqualTo(TypeString.ofGeneric("E"));
    }

    @Test
    void testIdentifier() {
        final String typeStr = "sw:integer";
        final TypeString typeString = TypeStringParser.parseTypeString(typeStr, SW_PACKAGE);
        assertThat(typeString)
            .isEqualTo(TypeString.ofIdentifier("integer", "sw"));
    }

    @Test
    void testGenericDefinitions() {
        final String typeStr = "sw:rope<sw:symbol>";
        final TypeString typeString = TypeStringParser.parseTypeString(typeStr, SW_PACKAGE);
        assertThat(typeString)
            .isEqualTo(
                TypeString.ofIdentifier("rope", SW_PACKAGE,
                    TypeString.ofIdentifier("symbol", SW_PACKAGE)));
    }

    @Test
    void testGenericDefinitions2() {
        final String typeStr = "sw:property_list<sw:symbol, sw:integer>";
        final TypeString typeString = TypeStringParser.parseTypeString(typeStr, SW_PACKAGE);
        assertThat(typeString)
            .isEqualTo(
                TypeString.ofIdentifier("property_list", SW_PACKAGE,
                    TypeString.ofIdentifier("symbol", SW_PACKAGE),
                    TypeString.ofIdentifier("integer", SW_PACKAGE)));
    }

    @Test
    void testGenericDefinitionsNested() {
        final String typeStr = "sw:property_list<sw:symbol, sw:rope<sw:integer>>";
        final TypeString typeString = TypeStringParser.parseTypeString(typeStr, SW_PACKAGE);
        assertThat(typeString)
            .isEqualTo(
                TypeString.ofIdentifier("property_list", SW_PACKAGE,
                    TypeString.ofIdentifier("symbol", SW_PACKAGE),
                    TypeString.ofIdentifier("rope", SW_PACKAGE,
                        TypeString.ofIdentifier("integer", SW_PACKAGE))));
    }

    @Test
    void testCombined() {
        final String typeStr = "sw:integer|sw:float";
        final TypeString typeString = TypeStringParser.parseTypeString(typeStr, SW_PACKAGE);
        assertThat(typeString)
            .isEqualTo(
                TypeString.ofCombination(
                    SW_PACKAGE,
                    TypeString.ofIdentifier("integer", SW_PACKAGE),
                    TypeString.ofIdentifier("float", SW_PACKAGE)));
    }

    @Test
    void testSyntaxError() {
        final String typeStr = "_sel";
        final TypeString typeString = TypeStringParser.parseTypeString(typeStr, SW_PACKAGE);
        assertThat(typeString)
            .isEqualTo(TypeString.UNDEFINED);
    }

    @Test
    void testExpressionResultStringSelf() {
        final String exprStr = "_self";
        final ExpressionResultString result = TypeStringParser.parseExpressionResultString(exprStr, SW_PACKAGE);
        assertThat(result)
            .isEqualTo(
                new ExpressionResultString(
                    TypeString.SELF));
    }

    @Test
    void testExpressionResultStringSelf2() {
        final String exprStr = "_self, _self";
        final ExpressionResultString result = TypeStringParser.parseExpressionResultString(exprStr, SW_PACKAGE);
        assertThat(result)
            .isEqualTo(
                new ExpressionResultString(
                    TypeString.SELF,
                    TypeString.SELF));
    }

    @Test
    void testExpressionResultStringSyntaxError() {
        final String exprStr = "_sel, _clon";
        final ExpressionResultString result = TypeStringParser.parseExpressionResultString(exprStr, SW_PACKAGE);
        assertThat(result)
            .isEqualTo(new ExpressionResultString(TypeString.UNDEFINED));
    }

    @Test
    void testUndefinedResultString() {
        final String exprStr = "__UNDEFINED_RESULT__";
        final ExpressionResultString result = TypeStringParser.parseExpressionResultString(exprStr, SW_PACKAGE);
        assertThat(result)
            .isEqualTo(ExpressionResultString.UNDEFINED);
    }

}
