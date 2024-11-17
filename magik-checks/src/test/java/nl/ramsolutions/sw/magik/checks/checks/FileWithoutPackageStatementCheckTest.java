package nl.ramsolutions.sw.magik.checks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;

/** Test FileWithoutPackageStatementCheck. */
class FileWithoutPackageStatementCheckTest extends MagikCheckTestBase {

  @Test
  void testNoPackageStatement() {
    final String code =
        """
        _method a.m1 _endmethod
        """;
    final FileWithoutPackageStatementCheck check = new FileWithoutPackageStatementCheck();
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  void testPackageStatement() {
    final String code =
        """
        _package user

        _method a.m1 _endmethod
        """;
    final FileWithoutPackageStatementCheck check = new FileWithoutPackageStatementCheck();
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }
}
