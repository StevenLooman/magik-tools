package nl.ramsolutions.sw.productdef.api;

import static org.sonar.sslr.tests.Assertions.assertThat;

import com.sonar.sslr.api.Grammar;
import org.junit.jupiter.api.Test;

/** Tests for {@link ProductDefinitionGrammar}. */
class ProductDefinitionGrammarTest {
  private final Grammar grammar = ProductDefinitionGrammar.create();

  @Test
  void testNumber() {
    assertThat(grammar.rule(ProductDefinitionGrammar.NUMBER)).matches("1");
  }

  @Test
  void testIdentifier() {
    assertThat(grammar.rule(ProductDefinitionGrammar.IDENTIFIER)).matches("test_product");
  }

  @Test
  void testProductIdentifictaion() {
    assertThat(grammar.rule(ProductDefinitionGrammar.PRODUCT_IDENTIFICATION))
        .matches("a_product layered_product")
        .matches("a_product customisation_product")
        .matches("a_product config_product");
  }

  @Test
  void testComment() {
    assertThat(grammar.rule(ProductDefinitionGrammar.COMMENT)).matches("# commented");
  }

  @Test
  void testDescription() {
    assertThat(grammar.rule(ProductDefinitionGrammar.FREE_LINE))
        .matches("abc\n")
        .matches("etc etc etc\n")
        .matches("this is the end\n")
        .notMatches("end\n");
    assertThat(grammar.rule(ProductDefinitionGrammar.DESCRIPTION))
        .matches("description\nend")
        .matches("description\n\t\nend")
        .matches("description\nabc\nend")
        .matches("description\netc\netc\netc\nend")
        .matches("description\n\n\netc\nend")
        .matches("description\nthis is the end\nend");
  }

  @Test
  void testDoNotTranslate() {
    assertThat(grammar.rule(ProductDefinitionGrammar.DO_NOT_TRANSLATE)).matches("do_not_translate");
  }

  @Test
  void testRequires() {
    assertThat(grammar.rule(ProductDefinitionGrammar.REQUIRES))
        .matches("requires\nend")
        .matches("requires\nx\nend")
        .matches("requires\nx 1\nend")
        .matches("requires\nx 1\ny 2\nend");
  }

  @Test
  void testTitle() {
    assertThat(grammar.rule(ProductDefinitionGrammar.TITLE))
        .matches("title\nend")
        .matches("title\n\t\nend")
        .matches("title\nabc\nend")
        .matches("title\netc\netc\netc\nend");
  }

  @Test
  void testVersion() {
    assertThat(grammar.rule(ProductDefinitionGrammar.VERSION))
        .matches("version 1.0")
        .matches("version 1.0.1")
        .matches("version 1.0.1 RC1")
        .matches("version 1.0 RC1")
        .matches("version 1.1.0.1-1 Beta")
        .matches("version 1.2.3.4-5");
  }

  @Test
  void testProductsDefinition() {
    assertThat(grammar.rule(ProductDefinitionGrammar.PRODUCT_DEFINITION))
        .matches("id layered_product\n#comment");
  }

  @Test
  void testSyntaxError() {
    assertThat(grammar.rule(ProductDefinitionGrammar.SYNTAX_ERROR_SECTION)).matches("abc\nend");
  }

  @Test
  void testMethodDefinition() {
    assertThat(grammar.rule(ProductDefinitionGrammar.PRODUCT_DEFINITION))
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
            """) // Syntax error.
        .matches(
            """
            test_product_a layered_product
            requires
              test_product_b
            end

            end
            """); // Syntax error.
  }
}
