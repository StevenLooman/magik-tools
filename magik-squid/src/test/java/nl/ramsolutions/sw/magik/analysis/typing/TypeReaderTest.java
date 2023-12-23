package nl.ramsolutions.sw.magik.analysis.typing;

import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResult;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.types.MagikType;
import nl.ramsolutions.sw.magik.analysis.typing.types.MagikType.Sort;
import nl.ramsolutions.sw.magik.analysis.typing.types.MagikTypeInstance;
import nl.ramsolutions.sw.magik.analysis.typing.types.ParameterReferenceType;
import nl.ramsolutions.sw.magik.analysis.typing.types.SelfType;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.types.UndefinedType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for TypeReader.
 */
class TypeReaderTest {

    private AbstractType parseTypeString(final TypeString typeStr, final ITypeKeeper typeKeeper) {
        final TypeReader reader = new TypeReader(typeKeeper);
        return reader.parseTypeString(typeStr);
    }

    private AbstractType parseTypeString(final TypeString typeStr) {
        final ITypeKeeper typeKeeper = new TypeKeeper();
        return this.parseTypeString(typeStr, typeKeeper);
    }

    private ExpressionResult parseExpressionResultString(final ExpressionResultString expressionResultstring) {
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final TypeReader reader = new TypeReader(typeKeeper);
        return reader.parseExpressionResultString(expressionResultstring);
    }

    @Test
    void testParseSelf() {
        final TypeString typeStr = TypeString.SELF;
        final AbstractType parsedType = this.parseTypeString(typeStr);
        assertThat(parsedType).isEqualTo(SelfType.INSTANCE);
    }

    @Test
    void testParseUndefined() {
        final TypeString typeStr = TypeString.UNDEFINED;
        final AbstractType parsedType = this.parseTypeString(typeStr);
        assertThat(parsedType).isEqualTo(UndefinedType.INSTANCE);
    }

    @Test
    void testParseUndefinedResult() {
        final ExpressionResultString expressionResultString = ExpressionResultString.UNDEFINED;
        final ExpressionResult result = this.parseExpressionResultString(expressionResultString);
        assertThat(result).isEqualTo(ExpressionResult.UNDEFINED);
    }

    @Test
    void testParameterReference() {
        final TypeString typeStr = TypeString.ofParameterRef("param1");
        final AbstractType parsedType = this.parseTypeString(typeStr);
        assertThat(parsedType).isInstanceOf(ParameterReferenceType.class);
        final ParameterReferenceType parameterType = (ParameterReferenceType) parsedType;
        assertThat(parameterType.getTypeString())
            .isEqualTo(TypeString.ofParameterRef("param1"));
    }

    @Test
    void testParseGeneric() {
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final TypeString ropeRef = TypeString.ofIdentifier("rope", "sw");
        final MagikType ropeType = new MagikType(typeKeeper, null, null, Sort.SLOTTED, ropeRef);
        ropeType.addGeneric(null, "E");
        typeKeeper.addType(ropeType);

        final TypeString typeStr = TypeString.ofIdentifier(
            "rope", "sw",
            TypeString.ofIdentifier("integer", "sw"));
        final AbstractType parsedType = this.parseTypeString(typeStr, typeKeeper);
        assertThat(parsedType).isInstanceOf(MagikTypeInstance.class);
        final MagikTypeInstance parsedRopeType = (MagikTypeInstance) parsedType;
        assertThat(parsedRopeType.getTypeString()).isEqualTo(
            TypeString.ofIdentifier(
                "rope", "sw",
                TypeString.ofIdentifier("integer", "sw")));
    }

}
