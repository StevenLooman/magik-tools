package nl.ramsolutions.sw.magik.analysis.typing.types;

import java.util.List;
import java.util.Set;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.DefinitionKeeperTypeKeeperAdapter;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeReader;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for MagikType.
 */
class MagikTypeTest {

    @Test
    void testCreateGenericType1() {
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final TypeString propertyListRef = TypeString.ofIdentifier("property_list", "sw");
        definitionKeeper.add(
            new ExemplarDefinition(
                null,
                "test_module",
                null,
                null,
                ExemplarDefinition.Sort.SLOTTED,
                propertyListRef,
                List.of(),
                List.of(),
                List.of(
                    new ExemplarDefinition.GenericDeclaration(null, "K"),
                    new ExemplarDefinition.GenericDeclaration(null, "E"))));
        definitionKeeper.add(
            new MethodDefinition(
                null,
                null,
                null,
                null,
                propertyListRef,
                "fast_keys_and_elements()",
                Set.of(),
                List.of(),
                null,
                ExpressionResultString.EMPTY,
                new ExpressionResultString(
                    TypeString.ofGeneric("K"),
                    TypeString.ofGeneric("E"))));

        final ITypeKeeper typeKeeper = new DefinitionKeeperTypeKeeperAdapter(definitionKeeper);
        final AbstractType magikType = typeKeeper.getType(propertyListRef);
        final GenericDeclaration key = magikType.getGeneric("K");
        final GenericDeclaration element = magikType.getGeneric("E");
        final TypeReader typeReader = new TypeReader(typeKeeper);
        final ExpressionResultString loopbodyResultString =
            new ExpressionResultString(
                TypeString.ofGeneric("K"),
                TypeString.ofGeneric("E"));
        final ExpressionResult loopbody = typeReader.parseExpressionResultString(loopbodyResultString);
        final GenericDeclaration genericKey = (GenericDeclaration) loopbody.get(0, null);
        assertThat(genericKey)
            .isEqualTo(key);

        final GenericDeclaration genericElement = (GenericDeclaration) loopbody.get(1, null);
        assertThat(genericElement)
            .isEqualTo(element);
    }

}
