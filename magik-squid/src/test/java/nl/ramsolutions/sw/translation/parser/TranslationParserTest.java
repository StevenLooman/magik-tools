package nl.ramsolutions.sw.translation.parser;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonar.sslr.api.AstNode;
import java.net.URI;
import java.util.List;
import nl.ramsolutions.sw.translation.api.TranslationGrammar;
import org.junit.jupiter.api.Test;

/** Tests for {@link TranslationParser}. */
class TranslationParserTest {

  private static final URI DEFAULT_URI = URI.create("memory://source.trn");

  private static final URI BASE_URI =
      URI.create("file:///product/modules/m/resources/base/translations/pipe.trn");
  private static final URI LANGUAGE_URI =
      URI.create("file:///product/modules/m/resources/en_gb/translations/pipe.trn");

  private AstNode parse(final String code) {
    return this.parse(code, DEFAULT_URI);
  }

  private AstNode parse(final String code, final URI uri) {
    final TranslationParser parser = new TranslationParser();
    return parser.parse(code, uri);
  }

  @Test
  void testTranslations() {
    final String code =
        """
        #% text_encoding = utf8
        external_name\tpipe\tunset\ten_gb\tPipe\t\tWater pipe
        external_name\tpipe\tlabel_map\tnl_nl\tLabel Map
        """;
    final AstNode node = this.parse(code);

    assertThat(node.getDescendants(TranslationGrammar.SYNTAX_ERROR)).isEmpty();

    final List<AstNode> translationNodes = node.getChildren(TranslationGrammar.TRANSLATION);
    assertThat(translationNodes).hasSize(2);

    final AstNode translation1Node = translationNodes.get(0);
    assertThat(translation1Node.getFirstChild(TranslationGrammar.TYPE).getTokenValue())
        .isEqualTo("external_name");
    assertThat(translation1Node.getFirstChild(TranslationGrammar.NAME).getTokenValue())
        .isEqualTo("pipe");
    assertThat(translation1Node.getFirstChild(TranslationGrammar.CONTEXT).getTokenValue())
        .isEqualTo("unset");
    assertThat(translation1Node.getFirstChild(TranslationGrammar.LANGUAGE).getTokenValue())
        .isEqualTo("en_gb");
    assertThat(translation1Node.getFirstChild(TranslationGrammar.SOURCE_TEXT).getTokenValue())
        .isEqualTo("Pipe");
    assertThat(translation1Node.getFirstChild(TranslationGrammar.TRANSLATED_TEXT).getTokenValue())
        .isEqualTo("Water pipe");

    final AstNode translation2Node = translationNodes.get(1);
    assertThat(translation2Node.getFirstChild(TranslationGrammar.CONTEXT).getTokenValue())
        .isEqualTo("label_map");
    assertThat(translation2Node.getFirstChild(TranslationGrammar.SOURCE_TEXT).getTokenValue())
        .isEqualTo("Label Map");
    assertThat(translation2Node.getFirstChild(TranslationGrammar.TRANSLATED_TEXT)).isNull();
  }

  @Test
  void testSyntaxErrorSpacesInsteadOfTabs() {
    final String code =
        """
        external_name\tpipe\tunset\ten_gb\tPipe\tWater pipe
        external_name   pipe    unset   nl_nl   Pipe    Waterleiding
        external_name\tvalve\tunset\ten_gb\tValve\tWater valve
        """;
    final AstNode node = this.parse(code);

    final List<AstNode> syntaxErrorNodes = node.getChildren(TranslationGrammar.SYNTAX_ERROR);
    assertThat(syntaxErrorNodes).hasSize(1);
    assertThat(syntaxErrorNodes.get(0).getTokenLine()).isEqualTo(2);
    assertThat(node.getChildren(TranslationGrammar.TRANSLATION)).hasSize(2);
  }

  @Test
  void testSyntaxErrorLineOfSpaces() {
    final String code = "external_name\tpipe\tunset\ten_gb\tPipe\tWater pipe\n\t\t\n   \n";
    final AstNode node = this.parse(code);

    final List<AstNode> syntaxErrorNodes = node.getChildren(TranslationGrammar.SYNTAX_ERROR);
    assertThat(syntaxErrorNodes).hasSize(1);
    assertThat(syntaxErrorNodes.get(0).getTokenLine()).isEqualTo(3);
  }

  @Test
  void testSyntaxErrorIndentedComment() {
    final String code = "# Comment\n  # No comment\n";
    final AstNode node = this.parse(code);

    final List<AstNode> syntaxErrorNodes = node.getChildren(TranslationGrammar.SYNTAX_ERROR);
    assertThat(syntaxErrorNodes).hasSize(1);
    assertThat(syntaxErrorNodes.get(0).getTokenLine()).isEqualTo(2);
  }

  @Test
  void testSyntaxErrorTooFewFields() {
    final String code = "external_name\tpipe\n";
    final AstNode node = this.parse(code);

    assertThat(node.getChildren(TranslationGrammar.SYNTAX_ERROR)).hasSize(1);
  }

  @Test
  void testIgnoredFields() {
    final String code =
        "external_name\tpipe\tunset\ten_gb\tPipe\tWater pipeexternal_name\tvalve\tunset\ten_gb"
            + "\tValve\tWater valve\n";
    final AstNode node = this.parse(code);

    assertThat(node.getChildren(TranslationGrammar.SYNTAX_ERROR)).isEmpty();
    assertThat(node.getDescendants(TranslationGrammar.FIELD)).isEmpty();

    final AstNode translationNode = node.getFirstChild(TranslationGrammar.TRANSLATION);
    assertThat(translationNode.getFirstChild(TranslationGrammar.TRANSLATED_TEXT).getTokenValue())
        .isEqualTo("Water pipeexternal_name");
    assertThat(translationNode.getChildren(TranslationGrammar.IGNORED_FIELD))
        .extracting(AstNode::getTokenValue)
        .containsExactly("valve", "unset", "en_gb", "Valve", "Water valve");
  }

  @Test
  void testEmptyField() {
    final String code = "external_name\tpipe\tunset\ten_gb\tPipe\t  \t\tWater pipe\n";
    final AstNode node = this.parse(code);

    assertThat(node.getChildren(TranslationGrammar.SYNTAX_ERROR)).isEmpty();

    final AstNode translationNode = node.getFirstChild(TranslationGrammar.TRANSLATION);
    assertThat(translationNode.getFirstChild(TranslationGrammar.SOURCE_TEXT).getTokenValue())
        .isEqualTo("Pipe");
    assertThat(translationNode.getChildren(TranslationGrammar.EMPTY_FIELD)).hasSize(1);
    assertThat(translationNode.getFirstChild(TranslationGrammar.TRANSLATED_TEXT)).isNull();
    assertThat(translationNode.getChildren(TranslationGrammar.IGNORED_FIELD))
        .extracting(AstNode::getTokenValue)
        .containsExactly("Water pipe");
  }

  @Test
  void testWithoutTexts() {
    final String code = "external_name\tpipe\tunset\ten_gb\n";
    final AstNode node = this.parse(code);

    assertThat(node.getChildren(TranslationGrammar.SYNTAX_ERROR)).isEmpty();

    final AstNode translationNode = node.getFirstChild(TranslationGrammar.TRANSLATION);
    assertThat(translationNode.getFirstChild(TranslationGrammar.LANGUAGE).getTokenValue())
        .isEqualTo("en_gb");
    assertThat(translationNode.getFirstChild(TranslationGrammar.SOURCE_TEXT)).isNull();
    assertThat(translationNode.getFirstChild(TranslationGrammar.TRANSLATED_TEXT)).isNull();
  }

  @Test
  void testSyntaxErrorWithoutLanguage() {
    final String code = "external_name\tpipe\tunset\n";

    final AstNode node = this.parse(code, BASE_URI);
    assertThat(node.getChildren(TranslationGrammar.SYNTAX_ERROR)).hasSize(1);
    assertThat(node.getChildren(TranslationGrammar.TRANSLATION)).isEmpty();

    final AstNode languageNode = this.parse(code, LANGUAGE_URI);
    assertThat(languageNode.getChildren(TranslationGrammar.SYNTAX_ERROR)).isEmpty();
    assertThat(languageNode.getChildren(TranslationGrammar.TRANSLATION)).hasSize(1);
  }

  @Test
  void testIsLanguageSpecific() {
    assertThat(TranslationParser.isLanguageSpecific(DEFAULT_URI)).isFalse();
    assertThat(TranslationParser.isLanguageSpecific(BASE_URI)).isFalse();
    assertThat(TranslationParser.isLanguageSpecific(LANGUAGE_URI)).isTrue();
  }

  @Test
  void testLanguageSpecificFileWithoutLanguage() {
    final String code = "field_value\tpipe\tstatus\tin_service\tIn service\n";
    final AstNode node = this.parse(code, LANGUAGE_URI);

    assertThat(node.getDescendants(TranslationGrammar.SYNTAX_ERROR)).isEmpty();
    assertThat(node.getDescendants(TranslationGrammar.FIELD)).isEmpty();

    final AstNode translationNode = node.getFirstChild(TranslationGrammar.TRANSLATION);
    assertThat(translationNode.getFirstChild(TranslationGrammar.LANGUAGE)).isNull();
    assertThat(translationNode.getFirstChild(TranslationGrammar.SOURCE_TEXT).getTokenValue())
        .isEqualTo("in_service");
    assertThat(translationNode.getFirstChild(TranslationGrammar.TRANSLATED_TEXT).getTokenValue())
        .isEqualTo("In service");
  }

  @Test
  void testLanguageSpecificFileWithLanguage() {
    final String code = "field_value\tpipe\tstatus\tnl_nl\tin_service\tIn bedrijf\n";
    final AstNode node = this.parse(code, LANGUAGE_URI);

    final AstNode translationNode = node.getFirstChild(TranslationGrammar.TRANSLATION);
    assertThat(translationNode.getFirstChild(TranslationGrammar.LANGUAGE).getTokenValue())
        .isEqualTo("nl_nl");
    assertThat(translationNode.getFirstChild(TranslationGrammar.SOURCE_TEXT).getTokenValue())
        .isEqualTo("in_service");
    assertThat(translationNode.getFirstChild(TranslationGrammar.TRANSLATED_TEXT).getTokenValue())
        .isEqualTo("In bedrijf");
  }

  @Test
  void testBaseFileWithoutTranslatedText() {
    final String code = "field_value\tpipe\tstatus\ten_gb\tin_service\n";
    final AstNode node = this.parse(code, BASE_URI);

    final AstNode translationNode = node.getFirstChild(TranslationGrammar.TRANSLATION);
    assertThat(translationNode.getFirstChild(TranslationGrammar.LANGUAGE).getTokenValue())
        .isEqualTo("en_gb");
    assertThat(translationNode.getFirstChild(TranslationGrammar.SOURCE_TEXT).getTokenValue())
        .isEqualTo("in_service");
    assertThat(translationNode.getFirstChild(TranslationGrammar.TRANSLATED_TEXT)).isNull();
  }

  @Test
  void testLanguageSpecificFileTabAtEndOfLine() {
    final String code = "field_value\tpipe\tstatus\tnl_nl\tin_service\t\n";
    final AstNode node = this.parse(code, LANGUAGE_URI);

    final AstNode translationNode = node.getFirstChild(TranslationGrammar.TRANSLATION);
    assertThat(translationNode.getFirstChild(TranslationGrammar.LANGUAGE).getTokenValue())
        .isEqualTo("nl_nl");
    assertThat(translationNode.getFirstChild(TranslationGrammar.SOURCE_TEXT).getTokenValue())
        .isEqualTo("in_service");
    assertThat(translationNode.getFirstChild(TranslationGrammar.TRANSLATED_TEXT)).isNull();
  }

  @Test
  void testSyntaxErrorUnknownType() {
    final String code = "unknown_type\tpipe\tunset\ten_gb\tPipe\tWater pipe\n";
    final AstNode node = this.parse(code);

    assertThat(node.getChildren(TranslationGrammar.SYNTAX_ERROR)).hasSize(1);
    assertThat(node.getChildren(TranslationGrammar.TRANSLATION)).isEmpty();
  }

  @Test
  void testUnknownTypeWithoutTranslatedText() {
    final String code = "external_name   pipe\tunset\ten_gb\tPipe\tWater pipe\n";
    final AstNode node = this.parse(code);

    assertThat(node.getChildren(TranslationGrammar.SYNTAX_ERROR)).isEmpty();

    final AstNode translationNode = node.getFirstChild(TranslationGrammar.TRANSLATION);
    assertThat(translationNode.getFirstChild(TranslationGrammar.TYPE).getTokenValue())
        .isEqualTo("external_name   pipe");
    assertThat(translationNode.getFirstChild(TranslationGrammar.TRANSLATED_TEXT)).isNull();
  }

  @Test
  void testCommentWithTabs() {
    final String code =
        """
        #field_value\tpipe\tstatus\ten_gb\tin_service\tIn service
        # type\tkey1\tkey2
        field_value\tpipe\tstatus\ten_gb\tin_service\tIn service
        """;

    for (final URI uri : List.of(BASE_URI, LANGUAGE_URI)) {
      final AstNode node = this.parse(code, uri);
      assertThat(node.getChildren(TranslationGrammar.SYNTAX_ERROR)).isEmpty();
      assertThat(node.getChildren(TranslationGrammar.TRANSLATION)).hasSize(1);
      assertThat(node.getFirstChild(TranslationGrammar.TRANSLATION).getTokenLine()).isEqualTo(3);
    }
  }

  @Test
  void testIndentedHashIsNotAComment() {
    final String code = "\t#field_value\tpipe\tstatus\ten_gb\tin_service\tIn service\n";
    final AstNode node = this.parse(code, BASE_URI);

    assertThat(node.getChildren(TranslationGrammar.SYNTAX_ERROR)).hasSize(1);
  }

  @Test
  void testFieldWithNonBreakingSpace() {
    final String code = "field_value\tpipe\tstatus\ten_gb\tIn service\u00A0\tIn bedrijf\u00A0\n";
    final AstNode node = this.parse(code, BASE_URI);

    assertThat(node.getChildren(TranslationGrammar.SYNTAX_ERROR)).isEmpty();
    final AstNode translationNode = node.getFirstChild(TranslationGrammar.TRANSLATION);
    assertThat(translationNode.getFirstChild(TranslationGrammar.SOURCE_TEXT).getTokenValue())
        .isEqualTo("In service\u00A0");
    assertThat(translationNode.getFirstChild(TranslationGrammar.TRANSLATED_TEXT).getTokenValue())
        .isEqualTo("In bedrijf\u00A0");
  }

  @Test
  void testByteOrderMark() {
    final String code = "\uFEFF#% text_encoding = utf8\nfield_value\tpipe\tstatus\ten_gb\ta\tb\n";
    final AstNode node = this.parse(code, BASE_URI);

    assertThat(node.getChildren(TranslationGrammar.SYNTAX_ERROR)).isEmpty();
    assertThat(node.getChildren(TranslationGrammar.TRANSLATION)).hasSize(1);
  }
}
