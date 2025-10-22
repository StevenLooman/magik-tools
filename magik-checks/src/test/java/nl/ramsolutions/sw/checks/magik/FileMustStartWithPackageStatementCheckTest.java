package nl.ramsolutions.sw.checks.magik;

import static nl.ramsolutions.sw.checks.magik.MagikCheckAssert.assertThat;

import nl.ramsolutions.sw.checks.MagikCheck;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test {@link FileMustStartWithPackageStatementCheck}. */
class FileMustStartWithPackageStatementCheckTest {

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
    assertThat(check).reportsNoIssues(code);
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
    assertThat(check).reportsIssueCount(code, 1);
  }
}
