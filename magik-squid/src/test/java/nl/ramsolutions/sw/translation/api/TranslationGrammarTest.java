package nl.ramsolutions.sw.translation.api;

import static org.sonar.sslr.tests.Assertions.assertThat;

import com.sonar.sslr.api.Grammar;
import org.junit.jupiter.api.Test;

/** Tests for TranslationGrammar. */
class TranslationGrammarTest {
  private final Grammar grammar = TranslationGrammar.create();

  @Test
  void testComment() {
    assertThat(this.grammar.rule(TranslationGrammar.COMMENT))
        .matches("# comment")
        .matches("#")
        .matches("#% text_encoding = utf8")
        .notMatches("not a comment");
  }

  @Test
  void testTranslation() {
    assertThat(this.grammar.rule(TranslationGrammar.TRANSLATION))
        .matches("external_name\tpipe\tunset\ten_gb\tPipe\tWater pipe")
        .matches("field_value\tpipe\tstatus\ten_gb\tin_service\tIn service")
        .matches("enumerator\tpipe_status\tunset\tnl_nl\tIn service\tIn bedrijf")
        .matches("external_name\tpipe\tunset\ten_gb\tPipe\t\t\tWater pipe")
        .matches("external_name\t\tpipe\tunset\ten_gb\tPipe\tWater pipe")
        .matches("\texternal_name\tpipe\tunset\ten_gb\tPipe\tWater pipe\t")
        .matches("  external_name\tpipe \tunset\t en_gb\tPipe  \t  Water pipe  ")
        .matches("field_value\tpipe\tstatus\tin_service\tIn service")
        .matches("external_name\tpipe\tunset\ten_gb\tPipe")
        .matches("external_name\tpipe\tunset\ten_gb")
        .matches("external_name\tpipe\tunset")
        .matches("external_name\tpipe\tunset\ten_gb\tPipe\tWater pipe\textra")
        .matches("external_name\tpipe\tunset\ten_gb\tPipe\t  \tWater pipe")
        .notMatches("external_name\tpipe")
        .notMatches("external_name")
        .notMatches("external_name\tpipe\tunset\ten_gb\tPipe\tWater pipe\n");
  }

  @Test
  void testTranslations() {
    assertThat(this.grammar.rule(TranslationGrammar.TRANSLATIONS))
        .matches("")
        .matches("#% text_encoding = utf8\n")
        .matches("external_name\tpipe\tunset\ten_gb\tPipe\tWater pipe")
        .matches("external_name\tpipe\tunset\ten_gb\tPipe\tWater pipe\t\n")
        .matches(
            """
            #% text_encoding = utf8
            external_name\tpipe\tunset\ten_gb\tPipe\tWater pipe
            external_name\tpipe\tunset\tnl_nl\tPipe\tWaterleiding

            # Comment
            enumerator\tpipe_status\tunset\tnl_nl\tIn service\tIn bedrijf
            """);
  }
}
