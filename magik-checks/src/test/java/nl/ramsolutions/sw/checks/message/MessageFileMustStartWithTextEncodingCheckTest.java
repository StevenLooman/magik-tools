package nl.ramsolutions.sw.checks.message;

import static nl.ramsolutions.sw.checks.message.MessageCheckAssert.assertThat;

import nl.ramsolutions.sw.checks.MessageCheck;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test {@link MessageFileMustStartWithTextEncodingCheck}. */
class MessageFileMustStartWithTextEncodingCheckTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        #% text_encoding = utf8
        :id_1 Text 1
        """,
        """
        #%text_encoding=iso8859_1
        :id_1 Text 1
        """,
      })
  void testValid(final String code) {
    final MessageCheck check = new MessageFileMustStartWithTextEncodingCheck();
    assertThat(check).reportsNoIssues(code);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        """
        :id_1 Text 1
        """,
        """
        # Comment
        #% text_encoding = utf8
        :id_1 Text 1
        """,
      })
  void testInvalid(final String code) {
    final MessageCheck check = new MessageFileMustStartWithTextEncodingCheck();
    assertThat(check).reportsIssueCount(code, 1);
  }
}
