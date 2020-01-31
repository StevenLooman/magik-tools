package org.stevenlooman.sw.magik.api;

import com.sonar.sslr.api.Grammar;
import org.junit.Test;
import org.stevenlooman.sw.magik.api.MessagePatchGrammar;

import static org.sonar.sslr.tests.Assertions.assertThat;

public class MessagePatchGrammarTest {
  private Grammar g = MessagePatchGrammar.create(MessagePatchGrammar.END_OF_MESSAGE_PATCH);

  @Test
  public void testEndMessagePatch() {
    assertThat(g.rule(MessagePatchGrammar.END_MESSAGE_PATCH))
        .matches("$");
  }

  @Test
  public void testMessageIdentifier() {
    assertThat(g.rule(MessagePatchGrammar.MESSAGE_IDENTIFIER))
        .matches(":test")
        .matches(":test_123");
  }

  @Test
  public void testLanguageIdentifier() {
    assertThat(g.rule(MessagePatchGrammar.LANGUAGE_IDENTIFIER))
        .matches(":en_gb")
        .matches(":de_de")
        .matches(":nl_nl");
  }

  @Test
  public void testRemove() {
    assertThat(g.rule(MessagePatchGrammar.REMOVE))
        .matches(":REMOVE\n");
  }

  @Test
  public void testMessage() {
    assertThat(g.rule(MessagePatchGrammar.MESSAGE))
        .matches("rest of the line\n")
        .matches("rest of the line\nmulti line\n")
        .matches("aB\n");
  }

  @Test
  public void testMessagePatch() {
    assertThat(g.rule(MessagePatchGrammar.MESSAGE_PATCH))
        .matches(":test this is a patch\n")
        .matches(":test this is a patch\nnext line\n")
        .matches(":test\nthis is a patch\n")
        .matches(":test\nthis is a patch\nnext line\n")
        .matches(":test :en_gb this is a patch\n")
        .matches(":test :en_gb this is a patch\nnext line\n")
        .matches(":test :en_gb\nthis is a patch\n")
        .matches(":test :en_gb\nthis is a patch\nnext line\n")
        .matches(":test :REMOVE\n")
        .matches(":test :en_gb :REMOVE\n");
  }

  @Test
  public void testReadMessagePatch() {
    assertThat(g.rule(MessagePatchGrammar.READ_MESSAGE_PATCH))
        .matches(":test this is a patch\n")
        .matches(":test this is a patch\n$")
        .matches(":test this is a patch\netc\n$")
        .matches(":test this is a patch\n:test2 this is another patch\n$")
        .matches(":test this is a patch\netc\n\n\n:test2 this is another patch\n$");
  }
}
