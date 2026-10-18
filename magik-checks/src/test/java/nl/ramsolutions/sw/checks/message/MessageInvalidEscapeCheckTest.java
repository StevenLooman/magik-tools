package nl.ramsolutions.sw.checks.message;

import static nl.ramsolutions.sw.checks.message.MessageCheckAssert.assertThat;

import nl.ramsolutions.sw.checks.MessageCheck;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test {@link MessageInvalidEscapeCheck}. */
class MessageInvalidEscapeCheckTest {

  @Test
  void testValid() {
    final MessageCheck check = new MessageInvalidEscapeCheck();
    final String code =
        """
        # Comment
        :id_1 #1 of #2: #@3, #h4
        :id_2 Line 1#nLine 2#tTabbed#s
        :id_3 Number ## 5
        :id_4 Ends with a #
        """;
    assertThat(check).reportsNoIssues(code);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        ":id Number # 5",
        ":id Unknown #x",
        ":id Parameter #0",
        ":id Translated #@",
        ":id\n# No comment",
      })
  void testInvalid(final String code) {
    final MessageCheck check = new MessageInvalidEscapeCheck();
    assertThat(check).reportsIssueCount(code, 1);
  }
}
