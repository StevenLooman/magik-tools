package nl.ramsolutions.sw.magik.languageserver.semantictokens;

import static org.assertj.core.api.Assertions.assertThat;

import nl.ramsolutions.sw.message.MessageFile;
import org.eclipse.lsp4j.SemanticTokens;
import org.junit.jupiter.api.Test;

/** Test SemanticTokenProvider for message (.msg) files. */
@SuppressWarnings("checkstyle:MagicNumber")
class MessageSemanticTokenProviderTest {

  private SemanticTokens getSemanticTokens(final String code) {
    final MessageFile messageFile = new MessageFile(MessageFile.DEFAULT_URI, code);
    final SemanticTokenProvider provider = new SemanticTokenProvider();
    return provider.provideSemanticTokensFull(messageFile);
  }

  @Test
  void testMessage() {
    final String code =
        """
        # Comment
        :id :en_gb Text
        #1 more text
        """;
    final SemanticTokens semanticTokens = this.getSemanticTokens(code);
    assertThat(semanticTokens.getData())
        .containsExactly(
            0,
            0,
            "# Comment".length(),
            SemanticToken.Type.COMMENT.getTokenType(),
            0,
            1,
            0,
            ":id".length(),
            SemanticToken.Type.STRING.getTokenType(),
            0,
            0,
            4,
            ":en_gb".length(),
            SemanticToken.Type.STRING.getTokenType(),
            0,
            1,
            0,
            "#1".length(),
            SemanticToken.Type.PARAMETER.getTokenType(),
            0);
  }

  @Test
  void testRemoveAndEscape() {
    final String code = ":id :|REMOVE|\n:other Line#nNext";
    final SemanticTokens semanticTokens = this.getSemanticTokens(code);
    assertThat(semanticTokens.getData())
        .containsExactly(
            0,
            0,
            ":id".length(),
            SemanticToken.Type.STRING.getTokenType(),
            0,
            0,
            4,
            ":|REMOVE|".length(),
            SemanticToken.Type.KEYWORD.getTokenType(),
            0,
            1,
            0,
            ":other".length(),
            SemanticToken.Type.STRING.getTokenType(),
            0,
            0,
            11,
            "#n".length(),
            SemanticToken.Type.REGEXP.getTokenType(),
            0);
  }

  @Test
  void testUses() {
    final String code = "+model";
    final SemanticTokens semanticTokens = this.getSemanticTokens(code);
    assertThat(semanticTokens.getData())
        .containsExactly(0, 1, "model".length(), SemanticToken.Type.STRING.getTokenType(), 0);
  }

  @Test
  void testInvalidEscape() {
    final String code = ":id A#xB";
    final SemanticTokens semanticTokens = this.getSemanticTokens(code);
    assertThat(semanticTokens.getData())
        .containsExactly(
            0,
            0,
            ":id".length(),
            SemanticToken.Type.STRING.getTokenType(),
            0,
            0,
            5,
            "#x".length(),
            SemanticToken.Type.REGEXP.getTokenType(),
            SemanticToken.Modifier.DEPRECATED.getModifierType());
  }

  @Test
  void testSyntaxError() {
    final String code = "# Comment\nNo message #1\n:2id #2\n#3 more\n:id #4";
    final SemanticTokens semanticTokens = this.getSemanticTokens(code);
    assertThat(semanticTokens.getData())
        .containsExactly(
            0,
            0,
            "# Comment".length(),
            SemanticToken.Type.COMMENT.getTokenType(),
            0,
            4,
            0,
            ":id".length(),
            SemanticToken.Type.STRING.getTokenType(),
            0,
            0,
            4,
            "#4".length(),
            SemanticToken.Type.PARAMETER.getTokenType(),
            0);
  }
}
