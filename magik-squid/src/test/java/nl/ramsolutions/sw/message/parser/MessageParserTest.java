package nl.ramsolutions.sw.message.parser;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.message.api.MessageGrammar;
import org.junit.jupiter.api.Test;

/** Tests for {@link MessageParser}. */
class MessageParserTest {

  private static final URI DEFAULT_URI = URI.create("memory://source.msg");

  private static String text(final AstNode node) {
    return node.getTokens().stream().map(Token::getValue).collect(Collectors.joining(" "));
  }

  private AstNode parse(final String code) {
    final MessageParser parser = new MessageParser();
    return parser.parse(code, DEFAULT_URI);
  }

  @Test
  void testMessages() {
    final String code =
        """
        #% text_encoding = utf8
        :id_1\tText 1
        :id_2 :en_gb Text 2
        :|id 3|
        Text 3, line 1

        #1 Text 3, line 3
        """;
    final AstNode node = this.parse(code);

    assertThat(node.getDescendants(MessageGrammar.SYNTAX_ERROR)).isEmpty();

    final List<AstNode> messageNodes = node.getChildren(MessageGrammar.MESSAGE);
    assertThat(messageNodes).hasSize(3);

    final AstNode message1Node = messageNodes.get(0);
    assertThat(message1Node.getFirstChild(MessageGrammar.MESSAGE_ID).getTokenValue())
        .isEqualTo(":id_1");
    assertThat(message1Node.getFirstChild(MessageGrammar.LANGUAGE)).isNull();
    assertThat(message1Node.getDescendants(MessageGrammar.TEXT_LINE))
        .extracting(MessageParserTest::text)
        .containsExactly("Text 1");

    final AstNode message2Node = messageNodes.get(1);
    assertThat(message2Node.getFirstChild(MessageGrammar.LANGUAGE).getTokenValue())
        .isEqualTo(":en_gb");
    assertThat(message2Node.getDescendants(MessageGrammar.TEXT_LINE))
        .extracting(MessageParserTest::text)
        .containsExactly("Text 2");

    final AstNode message3Node = messageNodes.get(2);
    assertThat(message3Node.getFirstChild(MessageGrammar.MESSAGE_ID).getTokenValue())
        .isEqualTo(":|id 3|");
    assertThat(message3Node.getDescendants(MessageGrammar.TEXT_LINE))
        .extracting(MessageParserTest::text)
        .containsExactly("Text 3, line 1", "#1 Text 3, line 3");
    assertThat(message3Node.getDescendants(MessageGrammar.TEXT_LINE))
        .extracting(AstNode::getTokenLine)
        .containsExactly(5, 7);
  }

  @Test
  void testSyntaxErrorTextBeforeFirstMessage() {
    final String code =
        """
        Text without a message
        :id Text
        """;
    final AstNode node = this.parse(code);

    final List<AstNode> syntaxErrorNodes = node.getChildren(MessageGrammar.SYNTAX_ERROR);
    assertThat(syntaxErrorNodes).hasSize(1);
    assertThat(syntaxErrorNodes.get(0).getTokenValue()).isEqualTo("Text without a message");
    assertThat(node.getChildren(MessageGrammar.MESSAGE)).hasSize(1);
  }

  @Test
  void testSyntaxErrorInvalidMessageId() {
    final String code =
        """
        :id_1 Text 1
        :2id Text 2
        :id_3 Text 3
        """;
    final AstNode node = this.parse(code);

    final List<AstNode> syntaxErrorNodes = node.getChildren(MessageGrammar.SYNTAX_ERROR);
    assertThat(syntaxErrorNodes).hasSize(1);
    assertThat(syntaxErrorNodes.get(0).getTokenLine()).isEqualTo(2);
    assertThat(node.getChildren(MessageGrammar.MESSAGE)).hasSize(2);
  }

  @Test
  void testNoCommentsAfterFirstMessage() {
    final String code =
        """
        # Comment
        :id_1 Text 1

        ## Text 1, line 3
        +text_1_line_4
        :id_2 Text 2
        """;
    final AstNode node = this.parse(code);

    assertThat(node.getDescendants(MessageGrammar.SYNTAX_ERROR)).isEmpty();
    assertThat(node.getDescendants(MessageGrammar.USES)).isEmpty();

    final List<AstNode> messageNodes = node.getChildren(MessageGrammar.MESSAGE);
    assertThat(messageNodes).hasSize(2);
    assertThat(messageNodes.get(0).getDescendants(MessageGrammar.TEXT_LINE))
        .extracting(MessageParserTest::text)
        .containsExactly("Text 1", "## Text 1, line 3", "+text_1_line_4");
  }

  @Test
  void testUses() {
    final String code =
        """
        #% text_encoding = utf8
        + model plugin
        :id_1 Text 1
        """;
    final AstNode node = this.parse(code);

    assertThat(node.getDescendants(MessageGrammar.SYNTAX_ERROR)).isEmpty();
    assertThat(node.getDescendants(MessageGrammar.USE_NAME))
        .extracting(AstNode::getTokenValue)
        .containsExactly("model", "plugin");
  }

  @Test
  void testSyntaxErrorIncludesText() {
    final String code =
        """
        :1id Text 1
        Text 1, line 2
        :id_2 Text 2
        """;
    final AstNode node = this.parse(code);

    assertThat(node.getChildren(MessageGrammar.SYNTAX_ERROR)).hasSize(1);
    assertThat(node.getChildren(MessageGrammar.MESSAGE)).hasSize(1);
  }

  @Test
  void testParameters() {
    final String code =
        """
        :id_1 #1 cannot be connected to #2: #@3
        :id_2 Price: #1#2
        :id_3 No parameters: ##1, a #
        :id_4 Line 1#nLine 2#t#110
        """;
    final AstNode node = this.parse(code);

    assertThat(node.getDescendants(MessageGrammar.SYNTAX_ERROR)).isEmpty();
    assertThat(node.getDescendants(MessageGrammar.INVALID_ESCAPE)).isEmpty();

    final List<AstNode> messageNodes = node.getChildren(MessageGrammar.MESSAGE);
    assertThat(messageNodes).hasSize(4);

    final AstNode message1Node = messageNodes.get(0);
    assertThat(message1Node.getDescendants(MessageGrammar.PARAMETER))
        .extracting(AstNode::getTokenValue)
        .containsExactly("#1", "#2", "#@3");
    assertThat(message1Node.getDescendants(MessageGrammar.TEXT))
        .extracting(AstNode::getTokenValue)
        .containsExactly("cannot be connected to", ":");

    final AstNode message2Node = messageNodes.get(1);
    assertThat(message2Node.getDescendants(MessageGrammar.PARAMETER))
        .extracting(AstNode::getTokenValue)
        .containsExactly("#1", "#2");
    assertThat(message2Node.getDescendants(MessageGrammar.TEXT))
        .extracting(AstNode::getTokenValue)
        .containsExactly("Price:");

    final AstNode message3Node = messageNodes.get(2);
    assertThat(message3Node.getDescendants(MessageGrammar.PARAMETER)).isEmpty();
    assertThat(message3Node.getDescendants(MessageGrammar.ESCAPE))
        .extracting(AstNode::getTokenValue)
        .containsExactly("##");
    assertThat(message3Node.getDescendants(MessageGrammar.TEXT))
        .extracting(AstNode::getTokenValue)
        .containsExactly("No parameters:", "1, a #");

    final AstNode message4Node = messageNodes.get(3);
    assertThat(message4Node.getDescendants(MessageGrammar.ESCAPE))
        .extracting(AstNode::getTokenValue)
        .containsExactly("#n", "#t");
    assertThat(message4Node.getDescendants(MessageGrammar.PARAMETER))
        .extracting(AstNode::getTokenValue)
        .containsExactly("#1");
    assertThat(message4Node.getDescendants(MessageGrammar.TEXT))
        .extracting(AstNode::getTokenValue)
        .containsExactly("Line 1", "Line 2", "10");
  }

  @Test
  void testParameterAtStartOfLine() {
    final String code =
        """
        :id_1
        Text
        #1
          #2 text
        """;
    final AstNode node = this.parse(code);

    assertThat(node.getDescendants(MessageGrammar.SYNTAX_ERROR)).isEmpty();
    assertThat(node.getDescendants(MessageGrammar.TEXT_LINE)).hasSize(3);
    assertThat(node.getDescendants(MessageGrammar.PARAMETER))
        .extracting(AstNode::getTokenValue)
        .containsExactly("#1", "#2");
  }

  @Test
  void testSyntaxErrorSecondUses() {
    final String code =
        """
        # Comment
        +model
        # Comment
        +plugin
        :id_1 Text 1
        """;
    final AstNode node = this.parse(code);

    final List<AstNode> syntaxErrorNodes = node.getChildren(MessageGrammar.SYNTAX_ERROR);
    assertThat(syntaxErrorNodes).hasSize(1);
    assertThat(syntaxErrorNodes.get(0).getTokenLine()).isEqualTo(4);
    assertThat(node.getDescendants(MessageGrammar.USE_NAME))
        .extracting(AstNode::getTokenValue)
        .containsExactly("model");
    assertThat(node.getChildren(MessageGrammar.MESSAGE)).hasSize(1);
  }

  @Test
  void testInvalidEscapes() {
    final String code =
        """
        :id_1 Number # 5, #x, #0, #@, a#b
        """;
    final AstNode node = this.parse(code);

    assertThat(node.getDescendants(MessageGrammar.SYNTAX_ERROR)).isEmpty();
    assertThat(node.getDescendants(MessageGrammar.INVALID_ESCAPE))
        .extracting(AstNode::getTokenValue)
        .containsExactly("# ", "#x", "#0", "#@", "#b");
    assertThat(node.getDescendants(MessageGrammar.TEXT))
        .extracting(AstNode::getTokenValue)
        .containsExactly("Number", "5,", ",", ",", ", a");
  }

  @Test
  void testLanguageAndTextDirectlyAfterMessageId() {
    final String code =
        """
        :id_1:en_gb Text 1
        :id-2 Text 2
        :|id 3|:|nl nl| Text 3
        """;
    final AstNode node = this.parse(code);

    assertThat(node.getDescendants(MessageGrammar.SYNTAX_ERROR)).isEmpty();

    final List<AstNode> messageNodes = node.getChildren(MessageGrammar.MESSAGE);
    assertThat(messageNodes).hasSize(3);
    assertThat(messageNodes)
        .extracting(messageNode -> messageNode.getFirstChild(MessageGrammar.MESSAGE_ID))
        .extracting(AstNode::getTokenValue)
        .containsExactly(":id_1", ":id", ":|id 3|");
    assertThat(messageNodes.get(0).getFirstChild(MessageGrammar.LANGUAGE).getTokenValue())
        .isEqualTo(":en_gb");
    assertThat(messageNodes.get(1).getFirstChild(MessageGrammar.LANGUAGE)).isNull();
    assertThat(messageNodes.get(1).getDescendants(MessageGrammar.TEXT_LINE))
        .extracting(MessageParserTest::text)
        .containsExactly("-2 Text 2");
    assertThat(messageNodes.get(2).getFirstChild(MessageGrammar.LANGUAGE).getTokenValue())
        .isEqualTo(":|nl nl|");
  }

  @Test
  void testRemove() {
    final String code =
        """
        :id_1 :|REMOVE|
        :id_2:|REMOVE| ignored text
        :id_3 :REMOVE Text 3
        """;
    final AstNode node = this.parse(code);

    assertThat(node.getDescendants(MessageGrammar.SYNTAX_ERROR)).isEmpty();

    final List<AstNode> messageNodes = node.getChildren(MessageGrammar.MESSAGE);
    assertThat(messageNodes).hasSize(3);
    assertThat(messageNodes.get(0).getFirstChild(MessageGrammar.REMOVE)).isNotNull();
    assertThat(messageNodes.get(0).getFirstChild(MessageGrammar.LANGUAGE)).isNull();
    assertThat(messageNodes.get(1).getFirstChild(MessageGrammar.REMOVE)).isNotNull();
    assertThat(messageNodes.get(1).getFirstChild(MessageGrammar.LANGUAGE)).isNull();
    assertThat(messageNodes.get(1).getFirstChild(MessageGrammar.IGNORED_TEXT).getTokenValue())
        .isEqualTo("ignored text");
    assertThat(messageNodes.get(1).getFirstChild(MessageGrammar.MESSAGE_TEXT)).isNull();

    assertThat(messageNodes.get(2).getFirstChild(MessageGrammar.REMOVE)).isNull();
    assertThat(messageNodes.get(2).getFirstChild(MessageGrammar.LANGUAGE).getTokenValue())
        .isEqualTo(":REMOVE");
  }

  @Test
  void testSyntaxErrorTextAfterRemove() {
    final String code =
        """
        :id_1 :|REMOVE|
        Text
        :id_2 Text 2
        """;
    final AstNode node = this.parse(code);

    final List<AstNode> syntaxErrorNodes = node.getChildren(MessageGrammar.SYNTAX_ERROR);
    assertThat(syntaxErrorNodes).hasSize(1);
    assertThat(syntaxErrorNodes.get(0).getTokenLine()).isEqualTo(2);
    assertThat(node.getChildren(MessageGrammar.MESSAGE)).hasSize(2);
  }
}
