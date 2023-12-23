package nl.ramsolutions.sw.magik.analysis.typing.types;

import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.DefinitionKeeperTypeKeeperAdapter;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test ExpressionResult.
 */
class ExpressionResultTest {

    @Test
    void testToStringOne() {
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final ITypeKeeper typeKeeper = new DefinitionKeeperTypeKeeperAdapter(definitionKeeper);
        final TypeString symbolRef = TypeString.ofIdentifier("symbol", "sw");
        final AbstractType symbolType = typeKeeper.getType(symbolRef);
        final ExpressionResult result = new ExpressionResult(symbolType);
        final String toString = result.toString();
        assertThat(toString).contains("(sw:symbol)");
    }

    @Test
    void testToStringThree() {
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final ITypeKeeper typeKeeper = new DefinitionKeeperTypeKeeperAdapter(definitionKeeper);
        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        final AbstractType integerType = typeKeeper.getType(integerRef);
        final ExpressionResult result = new ExpressionResult(integerType, integerType, integerType);
        final String toString = result.toString();
        assertThat(toString).contains("(sw:integer,sw:integer,sw:integer)");
    }

    @Test
    void testToStringUndefined() {
        final ExpressionResult result = ExpressionResult.UNDEFINED;
        final String toString = result.toString();
        assertThat(toString).contains("(UNDEFINED...)");
    }

    @Test
    void testToStringRepeating() {
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final ITypeKeeper typeKeeper = new DefinitionKeeperTypeKeeperAdapter(definitionKeeper);
        final TypeString unsetRef = TypeString.ofIdentifier("unset", "sw");
        final AbstractType unsetType = typeKeeper.getType(unsetRef);
        final TypeString symbolRef = TypeString.ofIdentifier("symbol", "sw");
        final AbstractType symbolType = typeKeeper.getType(symbolRef);
        final ExpressionResult result1 = ExpressionResult.UNDEFINED;
        final ExpressionResult result2 = new ExpressionResult(symbolType);
        final ExpressionResult result = new ExpressionResult(result1, result2, unsetType);
        final String toString = result.toString();
        assertThat(toString).contains("(_undefined|sw:symbol,_undefined|sw:unset...)");
    }

    @Test
    void testSubstituteType1() {
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final ITypeKeeper typeKeeper = new DefinitionKeeperTypeKeeperAdapter(definitionKeeper);
        final TypeString symbolRef = TypeString.ofIdentifier("symbol", "sw");
        final AbstractType symbolType = typeKeeper.getType(symbolRef);
        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        final AbstractType integerType = typeKeeper.getType(integerRef);

        final ExpressionResult result = new ExpressionResult(symbolType);
        final ExpressionResult newResult = result.substituteType(symbolType, integerType);
        final AbstractType newType = newResult.get(0, null);
        assertThat(newType).isEqualTo(integerType);
    }

    @Test
    void testSubstituteType2() {
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final ITypeKeeper typeKeeper = new DefinitionKeeperTypeKeeperAdapter(definitionKeeper);
        final TypeString symbolRef = TypeString.ofIdentifier("symbol", "sw");
        final AbstractType symbolType = typeKeeper.getType(symbolRef);
        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        final AbstractType integerType = typeKeeper.getType(integerRef);
        final CombinedType combinedType = new CombinedType(symbolType, integerType);

        final ExpressionResult result = new ExpressionResult(combinedType);
        final ExpressionResult newResult = result.substituteType(symbolType, integerType);
        final AbstractType newType = newResult.get(0, null);
        final AbstractType expectedType = new CombinedType(newType);
        assertThat(newType).isEqualTo(expectedType);
    }

    @Test
    void testSubstituteType3() {
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final ITypeKeeper typeKeeper = new DefinitionKeeperTypeKeeperAdapter(definitionKeeper);
        final TypeString symbolRef = TypeString.ofIdentifier("symbol", "sw");
        final AbstractType symbolType = typeKeeper.getType(symbolRef);
        final AbstractType parameterReferenceType = new ParameterReferenceType("p1");
        final CombinedType combinedType = new CombinedType(symbolType, parameterReferenceType);

        final ExpressionResult result = new ExpressionResult(combinedType);
        final ExpressionResult newResult = result.substituteType(parameterReferenceType, symbolType);
        final AbstractType newType = newResult.get(0, null);
        final AbstractType expectedType = new CombinedType(symbolType);
        assertThat(newType).isEqualTo(expectedType);
    }

}
