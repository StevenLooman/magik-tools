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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test MethodArgumentCountTypedCheck.
 */
class MethodArgumentCountTypedCheckTest extends MagikTypedCheckTestBase {

    @Test
    void testMethodUnknown() {
        final String code = ""
            + "_block\n"
            + "  object.m()\n"
            + "_endblock";
        final MagikTypedCheck check = new MethodArgumentCountTypedCheck();
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final List<MagikIssue> issues = this.runCheck(code, typeKeeper, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testArgumentCountMatches() {
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final TypeString objectTypeStr = TypeString.ofIdentifier("object", "sw");
        final MagikType objectType = (MagikType) typeKeeper.getType(objectTypeStr);
        objectType.addMethod(
            null,
            null,
            EnumSet.noneOf(Method.Modifier.class),
            "m()",
            List.of(
                new Parameter("p1", Parameter.Modifier.NONE, objectTypeStr),
                new Parameter("p2", Parameter.Modifier.NONE, objectTypeStr)),
            null,
            "method_doc",
            new ExpressionResultString(),
            new ExpressionResultString());

        final String code = ""
            + "_block\n"
            + "  object.m(object, object)\n"
            + "_endblock";
        final MagikTypedCheck check = new MethodArgumentCountTypedCheck();
        final List<MagikIssue> issues = this.runCheck(code, typeKeeper, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testArgumentMissing() {
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final TypeString objectTypeStr = TypeString.ofIdentifier("object", "sw");
        final MagikType objectType = (MagikType) typeKeeper.getType(objectTypeStr);
        objectType.addMethod(
            null,
            null,
            EnumSet.noneOf(Method.Modifier.class),
            "m()",
            List.of(
                new Parameter("p1", Parameter.Modifier.NONE, objectTypeStr),
                new Parameter("p2", Parameter.Modifier.NONE, objectTypeStr)),
            null,
            "method_doc",
            new ExpressionResultString(),
            new ExpressionResultString());

        final String code = ""
            + "_block\n"
            + "  object.m(object)\n"
            + "_endblock";
        final MagikTypedCheck check = new MethodArgumentCountTypedCheck();
        final List<MagikIssue> issues = this.runCheck(code, typeKeeper, check);
        assertThat(issues).hasSize(1);
    }

}
