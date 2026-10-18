package nl.ramsolutions.sw.magik.languageserver.semantictokens;

import static org.assertj.core.api.Assertions.assertThat;

import nl.ramsolutions.sw.translation.TranslationFile;
import org.eclipse.lsp4j.SemanticTokens;
import org.junit.jupiter.api.Test;

/** Test SemanticTokenProvider for translation (.trn) files. */
@SuppressWarnings("checkstyle:MagicNumber")
class TranslationSemanticTokenProviderTest {

  private SemanticTokens getSemanticTokens(final String code) {
    final TranslationFile translationFile = new TranslationFile(TranslationFile.DEFAULT_URI, code);
    final SemanticTokenProvider provider = new SemanticTokenProvider();
    return provider.provideSemanticTokensFull(translationFile);
  }

  @Test
  void testTranslation() {
    final String code = "enumerator\tstatus\tunset\ten_gb\tOn\tActive";
    final SemanticTokens semanticTokens = this.getSemanticTokens(code);
    assertThat(semanticTokens.getData())
        .containsExactly(
            0,
            0,
            "enumerator".length(),
            SemanticToken.Type.KEYWORD.getTokenType(),
            0,
            0,
            11,
            "status".length(),
            SemanticToken.Type.STRING.getTokenType(),
            0,
            0,
            7,
            "unset".length(),
            SemanticToken.Type.STRING.getTokenType(),
            0,
            0,
            6,
            "en_gb".length(),
            SemanticToken.Type.STRING.getTokenType(),
            0);
  }

  @Test
  void testSyntaxError() {
    final String code = "# Comment\nunknown\tstatus\tunset\ten_gb\tOn\tActive";
    final SemanticTokens semanticTokens = this.getSemanticTokens(code);
    assertThat(semanticTokens.getData())
        .containsExactly(0, 0, "# Comment".length(), SemanticToken.Type.COMMENT.getTokenType(), 0);
  }
}
