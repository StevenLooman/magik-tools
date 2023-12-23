package nl.ramsolutions.sw.magik.typedchecks.checks;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test MethodExistsTypedCheck.
 */
class MethodExistsTypedCheckTest extends MagikTypedCheckTestBase {

    @Test
    void testMethodUnknown() {
        final String code = ""
            + "_block\n"
            + "  object.m()\n"
            + "_endblock";
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final MagikTypedCheck check = new MethodExistsTypedCheck();
        final List<MagikIssue> issues = this.runCheck(code, definitionKeeper, check);
        assertThat(issues).hasSize(1);
    }

    @Test
    void testMethodKnown() {
        final String code = ""
            + "_block\n"
            + "  object.m()\n"
            + "_endblock";
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final TypeString objectRef = TypeString.ofIdentifier("object", "sw");
        definitionKeeper.add(
            new MethodDefinition(
                null,
                null,
                null,
                null,
                objectRef,
                "m()",
                EnumSet.noneOf(MethodDefinition.Modifier.class),
                Collections.emptyList(),
                null,
                new ExpressionResultString(objectRef),
                ExpressionResultString.EMPTY));
        final MagikTypedCheck check = new MethodExistsTypedCheck();
        final List<MagikIssue> issues = this.runCheck(code, definitionKeeper, check);
        assertThat(issues).isEmpty();
    }

}
