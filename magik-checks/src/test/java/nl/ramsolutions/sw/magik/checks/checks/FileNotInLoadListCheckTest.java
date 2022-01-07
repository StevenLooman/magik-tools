package nl.ramsolutions.sw.magik.checks.checks;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test FileNotInLoadListCheck.
 */
class FileNotInLoadListCheckTest extends MagikCheckTestBase {

    @Test
    void testNotInLoadList() throws IllegalArgumentException, IOException {
        final Path path =
            Path.of("magik-checks/src/test/resources/test_product/test_module/source/not_in_load_list.magik");
        final MagikCheck check = new FileNotInLoadListCheck();
        final List<MagikIssue> issues = this.runCheck(path, check);
        assertThat(issues).isNotEmpty();
    }

    @Test
    void testInLoadList() throws IllegalArgumentException, IOException {
        final Path path = Path.of("magik-checks/src/test/resources/test_product/test_module/source/in_load_list.magik");
        final MagikCheck check = new FileNotInLoadListCheck();
        final List<MagikIssue> issues = this.runCheck(path, check);
        assertThat(issues).isEmpty();
    }

}
