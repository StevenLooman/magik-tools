package nl.ramsolutions.sw.magik.analysis.typing;

import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResult;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.types.ParameterReferenceType;
import nl.ramsolutions.sw.magik.analysis.typing.types.SelfType;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.types.UndefinedType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for TypeParser.
 */
class TypeParserTest {

    @Test
    void testParseSelf() {
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final TypeParser parser = new TypeParser(typeKeeper);
        final TypeString typeStr = TypeString.SELF;
        final AbstractType parsedType = parser.parseTypeString(typeStr);
        assertThat(parsedType).isEqualTo(SelfType.INSTANCE);
    }

    @Test
    void testParseUndefined() {
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final TypeParser parser = new TypeParser(typeKeeper);
        final TypeString typeStr = TypeString.UNDEFINED;
        final AbstractType parsedType = parser.parseTypeString(typeStr);
        assertThat(parsedType).isEqualTo(UndefinedType.INSTANCE);
    }

    @Test
    void testParseUndefinedResult() {
        final String resultStr = "__UNDEFINED_RESULT__";
        final ExpressionResultString expressionResultString = ExpressionResultString.of(resultStr, "sw");
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final TypeParser parser = new TypeParser(typeKeeper);
        final ExpressionResult result = parser.parseExpressionResultString(expressionResultString);
        assertThat(result).isEqualTo(ExpressionResult.UNDEFINED);
    }

    @Test
    void testParameterReference() {
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final TypeParser parser = new TypeParser(typeKeeper);
        final TypeString typeStr = TypeString.of("_parameter(param1)", "sw");
        final AbstractType parsedType = parser.parseTypeString(typeStr);
        assertThat(parsedType).isInstanceOf(ParameterReferenceType.class);
        final ParameterReferenceType parameterType = (ParameterReferenceType) parsedType;
        assertThat(parameterType.getName()).isEqualTo("_parameter(param1)");
    }

}
