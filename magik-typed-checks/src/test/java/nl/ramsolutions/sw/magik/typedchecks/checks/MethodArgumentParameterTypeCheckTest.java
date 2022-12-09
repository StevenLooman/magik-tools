package nl.ramsolutions.sw.magik.typedchecks.checks;

import java.util.EnumSet;
import java.util.List;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.types.MagikType;
import nl.ramsolutions.sw.magik.analysis.typing.types.Method;
import nl.ramsolutions.sw.magik.analysis.typing.types.Parameter;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test MethodArgumentParameterTypeCheck.
 */
class MethodArgumentParameterTypeCheckTest extends MagikTypedCheckTestBase {

    @Test
    void testArgumentTypeMatches() {
        final String code = "integer.m1(:symbol)";
        final ITypeKeeper typeKeeper = new TypeKeeper();

        final TypeString integerRef = TypeString.of("sw:integer");
        final MagikType integerType = (MagikType) typeKeeper.getType(integerRef);
        final TypeString symbolRef = TypeString.of("sw:symbol");
        final AbstractType symbolType = typeKeeper.getType(symbolRef);
        final Parameter param1 = new Parameter("p1", Parameter.Modifier.NONE, symbolType);
        integerType.addMethod(
            null,
            EnumSet.noneOf(Method.Modifier.class),
            "m1()",
            List.of(param1),
            null,
            null,
            ExpressionResultString.UNDEFINED,
            new ExpressionResultString());

        final MagikTypedCheck check = new MethodArgumentParameterTypedCheck();
        final List<MagikIssue> checkResults = this.runCheck(code, typeKeeper, check);
        assertThat(checkResults).isEmpty();
    }

    @Test
    void testArgumentTypeNotMatches() {
        final String code = "integer.m1(1)";
        final ITypeKeeper typeKeeper = new TypeKeeper();

        final TypeString integerRef = TypeString.of("sw:integer");
        final MagikType integerType = (MagikType) typeKeeper.getType(integerRef);
        final TypeString symbolRef = TypeString.of("sw:symbol");
        final AbstractType symbolType = typeKeeper.getType(symbolRef);
        final Parameter param1 = new Parameter("p1", Parameter.Modifier.NONE, symbolType);
        integerType.addMethod(
            null,
            EnumSet.noneOf(Method.Modifier.class),
            "m1()",
            List.of(param1),
            null,
            null,
            ExpressionResultString.UNDEFINED,
            new ExpressionResultString());

        final MagikTypedCheck check = new MethodArgumentParameterTypedCheck();
        final List<MagikIssue> checkResults = this.runCheck(code, typeKeeper, check);
        assertThat(checkResults).hasSize(1);
    }

    @Test
    void testNoArguments() {
        final String code = "integer.m1()";
        final ITypeKeeper typeKeeper = new TypeKeeper();

        final TypeString integerRef = TypeString.of("sw:integer");
        final MagikType integerType = (MagikType) typeKeeper.getType(integerRef);
        final TypeString symbolRef = TypeString.of("sw:symbol");
        final AbstractType symbolType = typeKeeper.getType(symbolRef);
        final Parameter param1 = new Parameter("p1", Parameter.Modifier.NONE, symbolType);
        integerType.addMethod(
            null,
            EnumSet.noneOf(Method.Modifier.class),
            "m1()",
            List.of(param1),
            null,
            null,
            ExpressionResultString.UNDEFINED,
            new ExpressionResultString());

        final MagikTypedCheck check = new MethodArgumentParameterTypedCheck();
        final List<MagikIssue> checkResults = this.runCheck(code, typeKeeper, check);
        assertThat(checkResults).isEmpty();
    }

    @Test
    void testTooManyArguments() {
        final String code = "integer.m1(:symbol, :symbol)";
        final ITypeKeeper typeKeeper = new TypeKeeper();

        final TypeString integerRef = TypeString.of("sw:integer");
        final MagikType integerType = (MagikType) typeKeeper.getType(integerRef);
        final TypeString symbolRef = TypeString.of("sw:symbol");
        final AbstractType symbolType = typeKeeper.getType(symbolRef);
        final Parameter param1 = new Parameter("p1", Parameter.Modifier.NONE, symbolType);
        integerType.addMethod(
            null,
            EnumSet.noneOf(Method.Modifier.class),
            "m1()",
            List.of(param1),
            null,
            null,
            ExpressionResultString.UNDEFINED,
            new ExpressionResultString());

        final MagikTypedCheck check = new MethodArgumentParameterTypedCheck();
        final List<MagikIssue> checkResults = this.runCheck(code, typeKeeper, check);
        assertThat(checkResults).isEmpty();
    }

}
