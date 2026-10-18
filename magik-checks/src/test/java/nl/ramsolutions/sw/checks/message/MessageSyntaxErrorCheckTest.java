package nl.ramsolutions.sw.checks.message;

import static nl.ramsolutions.sw.checks.message.MessageCheckAssert.assertThat;

import nl.ramsolutions.sw.checks.MessageCheck;
import org.junit.jupiter.api.Test;

/** Test {@link MessageSyntaxErrorCheck}. */
class MessageSyntaxErrorCheckTest {

  @Test
  void testNoSyntaxErrors() {
    final MessageCheck check = new MessageSyntaxErrorCheck();
    final String code =
        """
        #% text_encoding = utf8
        +model
        # Comment
        :id_1\tText 1
        :|id 2| Text 2
        :id_3
        Text 3, line 1
        #1 Text 3, line 2
        :id_4 :en_gb Text 4
        # Text 4, line 2
        """;
    assertThat(check).reportsNoIssues(code);
  }

  @Test
  void testInvalidMessageIdWithText() {
    final MessageCheck check = new MessageSyntaxErrorCheck();
    final String code =
        """
        :1id Text 1
        Text 1, line 2
        Text 1, line 3
        :id_2 Text 2
        """;
    assertThat(check).reportsIssueCount(code, 1);
  }

  @Test
  void testTextBeforeFirstMessage() {
    final MessageCheck check = new MessageSyntaxErrorCheck();
    final String code =
        """
        #% text_encoding = utf8
        Text without a message
        :id_1 Text 1
        """;
    assertThat(check).reportsIssueCount(code, 1);
  }

  @Test
  void testInvalidMessageId() {
    final MessageCheck check = new MessageSyntaxErrorCheck();
    final String code =
        """
        :id_1 Text 1
        :2id Text 2
        :|id 3 Text 3
        """;
    assertThat(check).reportsIssueCount(code, 2);
  }
}
