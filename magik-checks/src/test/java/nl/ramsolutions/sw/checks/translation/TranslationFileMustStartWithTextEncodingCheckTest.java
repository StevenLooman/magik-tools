package nl.ramsolutions.sw.checks.translation;

import static nl.ramsolutions.sw.checks.translation.TranslationCheckAssert.assertThat;

import nl.ramsolutions.sw.checks.TranslationCheck;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test {@link TranslationFileMustStartWithTextEncodingCheck}. */
class TranslationFileMustStartWithTextEncodingCheckTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        #% text_encoding = utf8
        external_name\\tpipe\\tunset\\ten_gb\\tPipe\\tWater pipe
        """,
        """
        #%text_encoding=iso8859_1
        external_name\\tpipe\\tunset\\ten_gb\\tPipe\\tWater pipe
        """,
      })
  void testValid(final String code) {
    final TranslationCheck check = new TranslationFileMustStartWithTextEncodingCheck();
    assertThat(check).reportsNoIssues(code);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        """
        external_name\\tpipe\\tunset\\ten_gb\\tPipe\\tWater pipe
        """,
        """
        # Comment
        #% text_encoding = utf8
        external_name\\tpipe\\tunset\\ten_gb\\tPipe\\tWater pipe
        """,
      })
  void testInvalid(final String code) {
    final TranslationCheck check = new TranslationFileMustStartWithTextEncodingCheck();
    assertThat(check).reportsIssueCount(code, 1);
  }
}
