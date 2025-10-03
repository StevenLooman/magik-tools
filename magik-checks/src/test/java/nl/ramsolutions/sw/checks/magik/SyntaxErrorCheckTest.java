package nl.ramsolutions.sw.checks.magik;

import static nl.ramsolutions.sw.checks.magik.MagikCheckAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import nl.ramsolutions.sw.checks.MagikCheck;
import org.junit.jupiter.api.Test;

/** Test {@link SyntaxErrorCheck}. */
class SyntaxErrorCheckTest {

  @Test
  void testSyntaxError() {
    final MagikCheck check = new SyntaxErrorCheck();
    final String code =
        """
        _block
        _endbloc""";
    assertThat(check).reportsIssueCount(code, 1);
  }

  @Test
  void testSytnaxError2() {
    final MagikCheck check = new SyntaxErrorCheck();
    final String code = "_method";
    assertThat(check).reportsIssueCount(code, 1);
  }
}
