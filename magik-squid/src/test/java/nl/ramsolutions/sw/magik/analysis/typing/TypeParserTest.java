package nl.ramsolutions.sw.magik.analysis.typing;

import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResult;
import nl.ramsolutions.sw.magik.analysis.typing.types.SelfType;
import nl.ramsolutions.sw.magik.analysis.typing.types.UndefinedType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for TypeParser.
 */
class TypeParserTest {

    @Test
    void testParseSelf() {
        final String typeStr = "_self";
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final TypeParser parser = new TypeParser(typeKeeper);
        final AbstractType parsedType = parser.parseTypeString(typeStr, "sw");
        assertThat(parsedType).isEqualTo(SelfType.INSTANCE);
    }

    @Test
    void testParseUndefined() {
        final String typeStr = "_undefined";
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final TypeParser parser = new TypeParser(typeKeeper);
        final AbstractType parsedType = parser.parseTypeString(typeStr, "sw");
        assertThat(parsedType).isEqualTo(UndefinedType.INSTANCE);
    }

    @Test
    void testParseUndefinedResult() {
        final String resultStr = "__UNDEFINED_RESULT__";
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final TypeParser parser = new TypeParser(typeKeeper);
        ExpressionResult result = parser.parseExpressionResultString(resultStr, "sw");
        assertThat(result).isEqualTo(ExpressionResult.UNDEFINED);
    }

}
