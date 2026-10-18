package nl.ramsolutions.sw.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.net.URI;
import java.util.List;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.analysis.definitions.MessageHandlerUseDefinition;
import org.junit.jupiter.api.Test;

/** Tests for {@link MessageFileDefinitionParser}. */
@SuppressWarnings("checkstyle:MagicNumber")
class MessageFileDefinitionParserTest {

  private static final URI FILE_URI =
      URI.create("file:///product/modules/m/resources/en_gb/messages/pipe_editor.msg");

  private MessageFileDefinition parse(final URI uri, final String code) {
    final MessageFile messageFile = new MessageFile(uri, code);
    return messageFile.getMessageFileDefinition();
  }

  @Test
  void testFile() {
    final String code =
        """
        #% text_encoding = utf8
        + gen_graphics model
        :title Pipes
        """;
    final MessageFileDefinition definition = this.parse(FILE_URI, code);

    assertThat(definition.getLocation().getUri()).isEqualTo(FILE_URI);
    assertThat(definition.getHandlerName()).isEqualTo("pipe_editor");
    assertThat(definition.getLanguage()).isEqualTo("en_gb");
    assertThat(definition.getModuleName()).isNull();
    assertThat(definition.getMessages()).hasSize(1);
  }

  @Test
  void testUseDefinitions() {
    final String code =
        """
        #% text_encoding = utf8
        + gen_graphics model
        :title Pipes
        """;
    final MessageFile messageFile = new MessageFile(FILE_URI, code);
    final List<MessageHandlerUseDefinition> definitions =
        messageFile.getMessageHandlerUseDefinitions();

    assertThat(definitions)
        .extracting(
            MessageHandlerUseDefinition::getHandlerName,
            MessageHandlerUseDefinition::getUsedHandlerName)
        .containsExactly(tuple("pipe_editor", "gen_graphics"), tuple("pipe_editor", "model"));
    assertThat(definitions.get(1).getLocation().getUri()).isEqualTo(FILE_URI);
    assertThat(definitions.get(1).getLocation().getRange())
        .isEqualTo(new Range(new Position(2, 15), new Position(2, 20)));
  }

  @Test
  void testFileNotInMessagesDirectory() {
    final MessageFileDefinition definition = this.parse(MessageFile.DEFAULT_URI, ":title Pipes");

    assertThat(definition.getHandlerName()).isEqualTo("source");
    assertThat(definition.getLanguage()).isNull();
    final MessageFile messageFile = new MessageFile(MessageFile.DEFAULT_URI, ":title Pipes");
    assertThat(messageFile.getMessageHandlerUseDefinitions()).isEmpty();
  }

  @Test
  void testMessages() {
    final String code =
        """
        :title Pipes
        :Count :nl_nl #1 of #3,   #@12 and #1
        :|Multi Line|
        Line 1

          Line 3
        :old :|REMOVE|
        """;
    final MessageFileDefinition definition = this.parse(FILE_URI, code);

    final List<MessageDefinition> messages = definition.getMessages();
    assertThat(messages).hasSize(4);

    final MessageDefinition titleMessage = messages.get(0);
    assertThat(titleMessage.getId()).isEqualTo("title");
    assertThat(titleMessage.getLanguage()).isNull();
    assertThat(titleMessage.isRemoval()).isFalse();
    assertThat(titleMessage.getText()).isEqualTo("Pipes");
    assertThat(titleMessage.getParameters()).isEmpty();
    assertThat(titleMessage.getParameterCount()).isZero();
    assertThat(titleMessage.getLocation().getUri()).isEqualTo(FILE_URI);
    assertThat(titleMessage.getLocation().getRange())
        .isEqualTo(new Range(new Position(1, 0), new Position(1, 6)));

    final MessageDefinition countMessage = messages.get(1);
    assertThat(countMessage.getId()).isEqualTo("count");
    assertThat(countMessage.getLanguage()).isEqualTo("nl_nl");
    assertThat(countMessage.getText()).isEqualTo("#1 of #3,   #@12 and #1");
    assertThat(countMessage.getParameters()).containsExactly(1, 3, 12);
    assertThat(countMessage.getParameterCount()).isEqualTo(12);

    final MessageDefinition multiLineMessage = messages.get(2);
    assertThat(multiLineMessage.getId()).isEqualTo("Multi Line");
    assertThat(multiLineMessage.getText()).isEqualTo("Line 1\n\nLine 3");

    final MessageDefinition removedMessage = messages.get(3);
    assertThat(removedMessage.getId()).isEqualTo("old");
    assertThat(removedMessage.isRemoval()).isTrue();
    assertThat(removedMessage.getLanguage()).isNull();
    assertThat(removedMessage.getText()).isEmpty();
  }

  @Test
  void testGetMessagesById() {
    final String code =
        """
        :title Pipes
        :other Other
        :Title Valves
        """;
    final MessageFileDefinition definition = this.parse(FILE_URI, code);

    assertThat(definition.getMessages("title"))
        .extracting(MessageDefinition::getText)
        .containsExactly("Pipes", "Valves");
    assertThat(definition.getMessages("unknown")).isEmpty();
  }

  @Test
  void testHugeParameterNumber() {
    final MessageFileDefinition definition = this.parse(FILE_URI, ":a See #@12345678901 here\n");

    assertThat(definition.getMessages().get(0).getParameters()).containsExactly(Integer.MAX_VALUE);
  }

  @Test
  void testLanguageOfUpperCaseDirectory() {
    final URI uri =
        URI.create("file:///product/modules/m/resources/EN_GB/messages/pipe_editor.msg");

    assertThat(MessageFileDefinitionParser.getLanguage(uri)).isEqualTo("en_gb");
  }
}
