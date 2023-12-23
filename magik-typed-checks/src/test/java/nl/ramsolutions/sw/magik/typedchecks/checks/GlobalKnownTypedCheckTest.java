package nl.ramsolutions.sw.magik.typedchecks.checks;

import java.util.List;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test GlobalKnownTypedCheck.
 */
class GlobalKnownTypedCheckTest extends MagikTypedCheckTestBase {

    @Test
    void testKnownGlobal() {
        final String code = ""
            + "float.m";
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

        final MagikTypedCheck check = new GlobalKnownTypedCheck();
        final List<MagikIssue> checkResults = this.runCheck(code, definitionKeeper, check);
        assertThat(checkResults)
            .isEmpty();
    }

    @Test
    void testUnknownGlobal() {
        final String code = ""
            + "abc.m";
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

        final MagikTypedCheck check = new GlobalKnownTypedCheck();
        final List<MagikIssue> checkResults = this.runCheck(code, definitionKeeper, check);
        assertThat(checkResults)
            .hasSize(1);
    }

}
