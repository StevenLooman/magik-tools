package nl.ramsolutions.sw.magik.checks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test FileWithoutPackageStatementCheck. */
class FileMustStartWithPackageStatementCheckTest extends MagikCheckTestBase {

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _package user

        _method a.m1 _endmethod
        """,
        """
        # This is just a comment
        _package user

        _method a.m1 _endmethod
        """,
      })
  void testValid(final String code) {
    final MagikCheck check = new FileMustStartWithPackageStatementCheck();
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.m1 _endmethod
        """,
        """
        _method a.m1 _endmethod

        _package user

        _method a.m2 _endmethod
        """,
      })
  void testInvalid(final String code) {
    final MagikCheck check = new FileMustStartWithPackageStatementCheck();
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }
}
