package nl.ramsolutions.sw.magik.analysis.typing;

import java.util.List;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResult;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
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
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final ITypeKeeper typeKeeper = new DefinitionKeeperTypeKeeperAdapter(definitionKeeper);
        return this.parseTypeString(typeStr, typeKeeper);
    }

    private ExpressionResult parseExpressionResultString(final ExpressionResultString expressionResultstring) {
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final ITypeKeeper typeKeeper = new DefinitionKeeperTypeKeeperAdapter(definitionKeeper);
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
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final TypeString ropeRef = TypeString.ofIdentifier("rope", "sw");
        definitionKeeper.add(
            new ExemplarDefinition(
                null,
                "test_module",
                null,
                null,
                ExemplarDefinition.Sort.SLOTTED,
                ropeRef,
                List.of(),
                List.of(),
                List.of(
                    new ExemplarDefinition.GenericDeclaration(null, "E"))));

        final ITypeKeeper typeKeeper = new DefinitionKeeperTypeKeeperAdapter(definitionKeeper);
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
