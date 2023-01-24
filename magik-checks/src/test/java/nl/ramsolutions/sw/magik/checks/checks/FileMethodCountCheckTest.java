package nl.ramsolutions.sw.magik.checks.checks;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test FileMethodCountCheck.
 */
public class FileMethodCountCheckTest extends MagikCheckTestBase {

    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final int MAX_METHOD_COUNT = 10;

    @Test
    void testTooManyMethods() {
        final FileMethodCountCheck check = new FileMethodCountCheck();
        check.maxMethodCount = 2;
        final String code = ""
            + "_method a.m1 _endmethod\n"
            + "_method a.m1 _endmethod\n"
            + "_method a.m1 _endmethod\n";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(1);
    }

    @Test
    void testOk() {
        final FileMethodCountCheck check = new FileMethodCountCheck();
        check.maxMethodCount = MAX_METHOD_COUNT;
        final String code = ""
            + "_method a.m1 _endmethod\n"
            + "_method a.m1 _endmethod\n"
            + "_method a.m1 _endmethod\n";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

}
