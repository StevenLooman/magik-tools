package nl.ramsolutions.sw.magik.typedchecks.checks;

import java.util.EnumSet;
import java.util.List;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ParameterDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test {@link MethodArgumentCountMatchesParameterCountTypedCheck}.
 */
class MethodArgumentCountMatchesParameterCountTypedCheckTest extends MagikTypedCheckTestBase {

    @Test
    void testMethodUnknown() {
        final String code = ""
            + "_block\n"
            + "  object.m()\n"
            + "_endblock";
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final MagikTypedCheck check = new MethodArgumentCountMatchesParameterCountTypedCheck();
        final List<MagikIssue> issues = this.runCheck(code, definitionKeeper, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testArgumentCountMatches() {
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        definitionKeeper.add(
            new MethodDefinition(
                null,
                null,
                null,
                null,
                TypeString.SW_OBJECT,
                "m()",
                EnumSet.noneOf(MethodDefinition.Modifier.class),
                List.of(
                    new ParameterDefinition(
                        null,
                        null,
                        null,
                        null,
                        "p1",
                        ParameterDefinition.Modifier.NONE,
                        TypeString.SW_OBJECT),
                    new ParameterDefinition(
                        null,
                        null,
                        null,
                        null,
                        "p2",
                        ParameterDefinition.Modifier.NONE,
                        TypeString.SW_OBJECT)
                ),
                null,
                ExpressionResultString.EMPTY,
                ExpressionResultString.EMPTY));

        final String code = ""
            + "_block\n"
            + "  object.m(object, object)\n"
            + "_endblock";
        final MagikTypedCheck check = new MethodArgumentCountMatchesParameterCountTypedCheck();
        final List<MagikIssue> issues = this.runCheck(code, definitionKeeper, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testArgumentMissing() {
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        definitionKeeper.add(
            new MethodDefinition(
                null,
                null,
                null,
                null,
                TypeString.SW_OBJECT,
                "m()",
                EnumSet.noneOf(MethodDefinition.Modifier.class),
                List.of(
                    new ParameterDefinition(
                        null,
                        null,
                        null,
                        null,
                        "p1",
                        ParameterDefinition.Modifier.NONE,
                        TypeString.SW_OBJECT),
                    new ParameterDefinition(
                        null,
                        null,
                        null,
                        null,
                        "p2",
                        ParameterDefinition.Modifier.NONE,
                        TypeString.SW_OBJECT)),
                null,
                ExpressionResultString.EMPTY,
                ExpressionResultString.EMPTY));

        final String code = ""
            + "_block\n"
            + "  object.m(object)\n"
            + "_endblock";
        final MagikTypedCheck check = new MethodArgumentCountMatchesParameterCountTypedCheck();
        final List<MagikIssue> issues = this.runCheck(code, definitionKeeper, check);
        assertThat(issues).hasSize(1);
    }

}
