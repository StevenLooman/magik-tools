package nl.ramsolutions.sw.checks.moduledef;

import static nl.ramsolutions.sw.checks.moduledef.ModuleDefCheckAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import nl.ramsolutions.sw.checks.ModuleDefCheck;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test {@link ModuleDefSyntaxErrorCheck}. */
class ModuleDefSyntaxErrorCheckTest {

  @Test
  void testOk() {
    final ModuleDefCheck check = new ModuleDefSyntaxErrorCheck();
    final String code =
        """
        dummy 1
        """;
    assertThat(check).reportsNoIssues(code);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        dummy
        """,
        """
        dummy 1

        descriptions
        end
        """,
      })
  void testInvalid(final String code) {
    final ModuleDefCheck check = new ModuleDefSyntaxErrorCheck();
    assertThat(check).reportsIssueCount(code, 1);
  }
}
