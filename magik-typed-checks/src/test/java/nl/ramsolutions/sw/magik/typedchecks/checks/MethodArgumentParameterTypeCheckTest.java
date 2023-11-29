package nl.ramsolutions.sw.magik.typedchecks.checks;

import java.util.EnumSet;
import java.util.List;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.types.MagikType;
import nl.ramsolutions.sw.magik.analysis.typing.types.Method;
import nl.ramsolutions.sw.magik.analysis.typing.types.Parameter;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test MethodArgumentParameterTypeCheck.
 */
class MethodArgumentParameterTypeCheckTest extends MagikTypedCheckTestBase {

    private void addTestMethod(final ITypeKeeper typeKeeper) {
        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        final MagikType integerType = (MagikType) typeKeeper.getType(integerRef);
        final TypeString symbolRef = TypeString.ofIdentifier("symbol", "sw");
        integerType.addMethod(
            null,
            null,
            EnumSet.noneOf(Method.Modifier.class),
            "m1()",
            List.of(
                new Parameter(null, "p1", Parameter.Modifier.NONE, symbolRef)),
            null,
            null,
            ExpressionResultString.UNDEFINED,
            new ExpressionResultString());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "integer.m1(:symbol)",
        "integer.m1()",
        "integer.m1(:symbol, :symbol)",
    })
    void testValid(final String code) {
        final ITypeKeeper typeKeeper = new TypeKeeper();
        this.addTestMethod(typeKeeper);

        final MagikTypedCheck check = new MethodArgumentParameterTypedCheck();
        final List<MagikIssue> checkResults = this.runCheck(code, typeKeeper, check);
        assertThat(checkResults).isEmpty();
    }

    @Test
    void testArgumentTypeNotMatches() {
        final String code = "integer.m1(1)";
        final ITypeKeeper typeKeeper = new TypeKeeper();
        this.addTestMethod(typeKeeper);

        final MagikTypedCheck check = new MethodArgumentParameterTypedCheck();
        final List<MagikIssue> checkResults = this.runCheck(code, typeKeeper, check);
        assertThat(checkResults).hasSize(1);
    }

}
