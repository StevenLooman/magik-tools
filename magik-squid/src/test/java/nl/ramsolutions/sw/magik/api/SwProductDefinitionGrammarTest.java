package nl.ramsolutions.sw.magik.api;

import static org.sonar.sslr.tests.Assertions.assertThat;

import com.sonar.sslr.api.Grammar;
import nl.ramsolutions.sw.definitions.api.SwProductDefinitionGrammar;
import org.junit.jupiter.api.Test;

/** Tests for SwProductDefinitionGrammar. */
class SwProductDefinitionGrammarTest {
  private Grammar g = SwProductDefinitionGrammar.create();

  @Test
  void testNumber() {
    assertThat(g.rule(SwProductDefinitionGrammar.NUMBER)).matches("1");
  }

  @Test
  void testIdentifier() {
    assertThat(g.rule(SwProductDefinitionGrammar.IDENTIFIER)).matches("test_product");
  }

  @Test
  void testProductIdentifictaion() {
    assertThat(g.rule(SwProductDefinitionGrammar.PRODUCT_IDENTIFICATION))
        .matches("a_product layered_product")
        .matches("a_product customisation_product")
        .matches("a_product config_product");
  }

  @Test
  void testComment() {
    assertThat(g.rule(SwProductDefinitionGrammar.COMMENT)).matches("# commented");
  }

  @Test
  void testDescription() {
    assertThat(g.rule(SwProductDefinitionGrammar.FREE_LINE))
        .matches("abc\n")
        .matches("etc etc etc\n")
        .matches("this is the end\n")
        .notMatches("end\n");
    assertThat(g.rule(SwProductDefinitionGrammar.DESCRIPTION))
        .matches("description\nend")
        .matches("description\n\t\nend")
        .matches("description\nabc\nend")
        .matches("description\netc\netc\netc\nend")
        .matches("description\n\n\netc\nend")
        .matches("description\nthis is the end\nend");
  }

  @Test
  void testDoNotTranslate() {
    assertThat(g.rule(SwProductDefinitionGrammar.DO_NOT_TRANSLATE)).matches("do_not_translate");
  }

  @Test
  void testRequires() {
    assertThat(g.rule(SwProductDefinitionGrammar.REQUIRES))
        .matches("requires\nend")
        .matches("requires\nx\nend")
        .matches("requires\nx 1\nend")
        .matches("requires\nx 1\ny 2\nend");
  }

  @Test
  void testTitle() {
    assertThat(g.rule(SwProductDefinitionGrammar.TITLE))
        .matches("title\nend")
        .matches("title\n\t\nend")
        .matches("title\nabc\nend")
        .matches("title\netc\netc\netc\nend");
  }

  @Test
  void testVersion() {
    assertThat(g.rule(SwProductDefinitionGrammar.VERSION))
        .matches("version 1.0")
        .matches("version 1.0.1")
        .matches("version 1.0.1 RC1")
        .matches("version 1.0 RC1");
  }

  @Test
  void testProductsDefinition() {
    assertThat(g.rule(SwProductDefinitionGrammar.PRODUCT_DEFINITION))
        .matches("id layered_product\n#comment");
  }

  @Test
  void testSyntaxError() {
    assertThat(g.rule(SwProductDefinitionGrammar.SYNTAX_ERROR_SECTION)).matches("abc\nend");
  }

  @Test
  void testMethodDefinition() {
    assertThat(g.rule(SwProductDefinitionGrammar.PRODUCT_DEFINITION))
        .matches("")
        .matches("test_product_a")
        .matches("test_product_a layered_p")
        .matches("test_product_a layered_product")
        .matches(
            """
            test_product_a layered_product
            requires
              test_product_b
            end
        """)
        .matches(
            """
          test_product_a layered_product
          reqs
            test_product_b
          end
        """) // Syntax error.
        .matches(
            """
            test_product_a layered_product
            some extra line""") // Syntax error.
        .matches(
            """
              test_product_a layered_product
            some extra line
            """); // Syntax error.
  }
}
