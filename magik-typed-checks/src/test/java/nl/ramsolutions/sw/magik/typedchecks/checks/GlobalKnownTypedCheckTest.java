package nl.ramsolutions.sw.magik.typedchecks.checks;

import java.util.List;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeKeeper;
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
        final ITypeKeeper typeKeeper = new TypeKeeper();

        final MagikTypedCheck check = new GlobalKnownTypedCheck();
        final List<MagikIssue> checkResults = this.runCheck(code, typeKeeper, check);
        assertThat(checkResults)
            .isEmpty();
    }

    @Test
    void testUnknownGlobal() {
        final String code = ""
            + "abc.m";
        final ITypeKeeper typeKeeper = new TypeKeeper();

        final MagikTypedCheck check = new GlobalKnownTypedCheck();
        final List<MagikIssue> checkResults = this.runCheck(code, typeKeeper, check);
        assertThat(checkResults)
            .hasSize(1);
    }

}
