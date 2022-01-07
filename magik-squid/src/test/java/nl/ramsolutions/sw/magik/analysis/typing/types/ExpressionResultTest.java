package nl.ramsolutions.sw.magik.analysis.typing.types;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test ExpressionResult.
 */
class ExpressionResultTest {

    @Test
    void testToStringOne() {
        final AbstractType symbolType = new IndexedType(GlobalReference.of("sw:symbol"));
        final ExpressionResult result = new ExpressionResult(symbolType);
        final String toString = result.toString();
        assertThat(toString).contains("(sw:symbol)");
    }

    @Test
    void testToStringThree() {
        final AbstractType integerType = new SlottedType(GlobalReference.of("sw:integer"));
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
        final AbstractType unsetType = new SlottedType(GlobalReference.of("sw:unset"));
        final AbstractType symbolType = new IndexedType(GlobalReference.of("sw:symbol"));
        final ExpressionResult result1 = ExpressionResult.UNDEFINED;
        final ExpressionResult result2 = new ExpressionResult(symbolType);
        final ExpressionResult result = new ExpressionResult(result1, result2, unsetType);
        final String toString = result.toString();
        assertThat(toString).contains("(_undefined|sw:symbol,_undefined|sw:unset...)");
    }

}
