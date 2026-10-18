package nl.ramsolutions.sw.checks.translation;

import static nl.ramsolutions.sw.checks.translation.TranslationCheckAssert.assertThat;

import nl.ramsolutions.sw.checks.TranslationCheck;
import org.junit.jupiter.api.Test;

/** Test {@link TranslationIgnoredFieldCheck}. */
class TranslationIgnoredFieldCheckTest {

  @Test
  void testValid() {
    final TranslationCheck check = new TranslationIgnoredFieldCheck();
    final String code =
        """
        #% text_encoding = utf8
        external_name\tpipe\tunset\ten_gb\tPipe\tWater pipe
        external_name\tpipe\tunset\tnl_nl\tPipe\t\t\tWaterleiding\t
        """;
    assertThat(check).reportsNoIssues(code);
  }

  @Test
  void testIgnoredFields() {
    final TranslationCheck check = new TranslationIgnoredFieldCheck();
    final String code =
        """
        external_name\tpipe\tunset\ten_gb\tPipe\tWater pipe\tExtra 1\tExtra 2
        external_name\tvalve\tunset\ten_gb\tValve\t  \tWater valve
        """;
    assertThat(check).reportsIssueCount(code, 2);
  }
}
