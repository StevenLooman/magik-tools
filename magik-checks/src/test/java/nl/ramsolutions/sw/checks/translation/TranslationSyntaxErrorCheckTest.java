package nl.ramsolutions.sw.checks.translation;

import static nl.ramsolutions.sw.checks.translation.TranslationCheckAssert.assertThat;

import nl.ramsolutions.sw.checks.TranslationCheck;
import org.junit.jupiter.api.Test;

/** Test {@link TranslationSyntaxErrorCheck}. */
class TranslationSyntaxErrorCheckTest {

  @Test
  void testNoSyntaxErrors() {
    final TranslationCheck check = new TranslationSyntaxErrorCheck();
    final String code =
        """
        #% text_encoding = utf8
        external_name\tpipe\tunset\ten_gb\tPipe\tWater pipe
        external_name\tpipe\tunset\tnl_nl\tPipe\t\t\tWaterleiding
        enumerator\tpipe_status\tunset\tnl_nl\tIn service\tIn bedrijf
        """;
    assertThat(check).reportsNoIssues(code);
  }

  @Test
  void testSpacesInsteadOfTabs() {
    final TranslationCheck check = new TranslationSyntaxErrorCheck();
    final String code =
        """
        external_name\tpipe\tunset\ten_gb\tPipe\tWater pipe
        external_name   pipe    unset   nl_nl   Pipe    Waterleiding
        """;
    assertThat(check).reportsIssueCount(code, 1);
  }

  @Test
  void testTooFewFields() {
    final TranslationCheck check = new TranslationSyntaxErrorCheck();
    final String code =
        """
        external_name\tpipe
        """;
    assertThat(check).reportsIssueCount(code, 1);
  }

  @Test
  void testUnknownType() {
    final TranslationCheck check = new TranslationSyntaxErrorCheck();
    final String code =
        """
        unknown_type\tpipe\tunset\ten_gb\tPipe\tWater pipe
        """;
    assertThat(check).reportsIssueCount(code, 1);
  }

  @Test
  void testIgnoredLinesAreNoSyntaxErrors() {
    final TranslationCheck check = new TranslationSyntaxErrorCheck();
    final String code =
        """
        external_name\tpipe\tunset\ten_gb
        external_name   pipe\tunset\ten_gb\tPipe\tWater pipe
        external_name\tpipe\tunset\ten_gb\tPipe\tWater pipe\tExtra
        external_name\tpipe\tunset\ten_gb\tPipe\t  \tWater pipe
        """;
    assertThat(check).reportsNoIssues(code);
  }
}
