package nl.ramsolutions.sw.magik.typedchecks.checks;

import java.util.List;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalExpressionIsFalseTypedCheck}.
 */
class ConditionalExpressionIsFalseTypedCheckTest extends MagikTypedCheckTestBase {

    @ParameterizedTest
    @ValueSource(strings = {
        ""
            + "_if _true\n"
            + "_then\n"
            + "_endif\n",
        ""
            + "_if :a _is :b\n"
            + "_then\n"
            + "_endif\n",
    })
    void testOk(final String code) {
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final MagikTypedCheck check = new ConditionalExpressionIsFalseTypedCheck();
        final List<MagikIssue> checkResults = this.runCheck(code, definitionKeeper, check);
        assertThat(checkResults).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        ""
            + "_if :a\n"
            + "_then\n"
            + "_endif\n",
        ""
            + "_if _maybe\n"
            + "_then\n"
            + "_endif\n",
    })
    void testFail(final String code) {
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final MagikTypedCheck check = new ConditionalExpressionIsFalseTypedCheck();
        final List<MagikIssue> checkResults = this.runCheck(code, definitionKeeper, check);
        assertThat(checkResults).hasSize(1);
    }

}
