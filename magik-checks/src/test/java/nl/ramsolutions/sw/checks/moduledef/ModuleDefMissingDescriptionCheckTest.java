package nl.ramsolutions.sw.checks.moduledef;

import static nl.ramsolutions.sw.checks.moduledef.ModuleDefCheckAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import nl.ramsolutions.sw.checks.ModuleDefCheck;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test {@link ModuleDefMissingDescriptionCheck}. */
class ModuleDefMissingDescriptionCheckTest {

  @Test
  void testOk() {
    final ModuleDefCheck check = new ModuleDefMissingDescriptionCheck();
    final String code =
        """
        dummy 1

        description
          Dummy module
        end
        """;
    assertThat(check).reportsNoIssues(code);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        dummy 1
        """,
        """
        dummy 1

        description
        end
        """,
        """
        dummy 1

        description

        end
        """,
      })
  void testInvalid(final String code) {
    final ModuleDefCheck check = new ModuleDefMissingDescriptionCheck();
    assertThat(check).reportsIssueCount(code, 1);
  }
}
