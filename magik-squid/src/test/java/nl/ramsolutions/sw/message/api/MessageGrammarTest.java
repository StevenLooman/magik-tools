package nl.ramsolutions.sw.message.api;

import static org.sonar.sslr.tests.Assertions.assertThat;

import com.sonar.sslr.api.Grammar;
import org.junit.jupiter.api.Test;

/** Tests for MessageGrammar. */
class MessageGrammarTest {
  private final Grammar grammar = MessageGrammar.create();

  @Test
  void testMessageId() {
    assertThat(this.grammar.rule(MessageGrammar.MESSAGE_ID))
        .matches(":id")
        .matches(":id_1")
        .matches(":valid?")
        .matches(":a!b?c")
        .matches(":_id")
        .matches(":\u00e9")
        .matches(":|id with spaces|")
        .matches(":|method()|")
        .matches(":prefix|piped part|suffix")
        .matches(":a\\|b")
        .matches(":a\\ b")
        .notMatches("id")
        .notMatches(":")
        .notMatches(":1id")
        .notMatches(":id-1")
        .notMatches(":|unclosed")
        .notMatches(":id|unclosed");
  }

  @Test
  void testLanguage() {
    assertThat(this.grammar.rule(MessageGrammar.LANGUAGE))
        .matches(":en_gb")
        .matches(":nl_nl")
        .matches(":REMOVE")
        .notMatches(":1")
        .notMatches("en_gb");
  }

  @Test
  void testRemove() {
    assertThat(this.grammar.rule(MessageGrammar.REMOVE))
        .matches(":|REMOVE|")
        .notMatches(":REMOVE")
        .notMatches(":|remove|")
        .notMatches(":|REMOVE|x");
  }

  @Test
  void testComment() {
    assertThat(this.grammar.rule(MessageGrammar.COMMENT))
        .matches("# comment")
        .matches("## comment")
        .matches("#")
        .matches("#% text_encoding = utf8")
        .matches("#comment")
        .notMatches("not a comment");
  }

  @Test
  void testParameter() {
    assertThat(this.grammar.rule(MessageGrammar.PARAMETER))
        .matches("#1")
        .matches("#9")
        .matches("#@1")
        .matches("#@12")
        .matches("#h4")
        .notMatches("#0")
        .notMatches("#10")
        .notMatches("#")
        .notMatches("#n")
        .notMatches("#@")
        .notMatches("# 1")
        .notMatches("1");
  }

  @Test
  void testEscape() {
    assertThat(this.grammar.rule(MessageGrammar.ESCAPE))
        .matches("##")
        .matches("#n")
        .matches("#t")
        .matches("#s")
        .notMatches("#")
        .notMatches("#1")
        .notMatches("#x");
  }

  @Test
  void testInvalidEscape() {
    assertThat(this.grammar.rule(MessageGrammar.INVALID_ESCAPE))
        .matches("#x")
        .matches("#0")
        .matches("# ")
        .matches("#@")
        .matches("#h")
        .notMatches("#");
  }

  @Test
  void testTextLine() {
    assertThat(this.grammar.rule(MessageGrammar.TEXT_LINE))
        .matches("Text")
        .matches("#1")
        .matches("#1#2")
        .matches("Text #1 text #2")
        .matches("#1 text")
        .matches("Text with a # and a #n")
        .matches("##1 is not a parameter")
        .notMatches("")
        .notMatches("  ")
        .notMatches("Text\nText");
  }

  @Test
  void testUses() {
    assertThat(this.grammar.rule(MessageGrammar.USES))
        .matches("+model")
        .matches("+ model")
        .matches("+model plugin")
        .notMatches("+")
        .notMatches("model")
        .notMatches("+=====+");
  }

  @Test
  void testMessage() {
    assertThat(this.grammar.rule(MessageGrammar.MESSAGE))
        .matches(":id")
        .matches(":id Text")
        .matches(":id\tText of #1 with a parameter")
        .matches(":id\t\t  Text")
        .matches(":id :en_gb Text")
        .matches(":id\t:nl_nl\tTekst")
        .matches(":|id with spaces| Text")
        .matches(":id:en_gb Text")
        .matches(":id-1 Text")
        .matches(":id(text)")
        .matches(":id :|REMOVE|")
        .matches(":id:|REMOVE| ignored text")
        .notMatches(":id :|REMOVE|\nText")
        .notMatches(":1id Text")
        .matches(":id # not a comment")
        .matches(":id\nText")
        .matches(":id\nText line 1\nText line 2")
        .matches(":id Text line 1\n#1 text line 2")
        .matches(":id Text line 1\n## text line 2\n# text line 3")
        .matches(":id Text line 1\n\nText line 3")
        .matches(":id\n  indented text\n  :not_an_id")
        .matches(":id\n+=====+\n+not_uses")
        .matches(":id\r\nText line 1\r\nText line 2")
        .notMatches(":id Text\n:other_id Text")
        .notMatches(":id Text\n");
  }

  @Test
  void testMessages() {
    assertThat(this.grammar.rule(MessageGrammar.MESSAGES))
        .matches("")
        .matches("#% text_encoding = utf8\n")
        .matches(":id Text")
        .matches(":id Text\n")
        .matches("+model # comment\n:id Text")
        .matches(
            """
            #% text_encoding = utf8
            +model
            # Comment
            :id_1\tText 1
            :id_2\t\tText 2

            :|id 3|
            Text 3, line 1
            #1 Text 3, line 2

            ## Text 3, line 4
            :id_4 :en_gb Text 4
            """);
  }
}
