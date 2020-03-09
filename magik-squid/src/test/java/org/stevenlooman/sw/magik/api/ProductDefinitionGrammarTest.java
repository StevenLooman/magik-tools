package org.stevenlooman.sw.magik.api;

import com.sonar.sslr.api.Grammar;
import org.junit.Test;

import static org.sonar.sslr.tests.Assertions.assertThat;

public class ProductDefinitionGrammarTest {
  private Grammar g = ProductDefinitionGrammar.create();

  @Test
  public void testNumber() {
    assertThat(g.rule(ProductDefinitionGrammar.NUMBER))
        .matches("1");
  }

  @Test
  public void testIdentifier() {
    assertThat(g.rule(ProductDefinitionGrammar.IDENTIFIER))
        .matches("test_product");
  }

  @Test
  public void testProductIdentifictaion() {
    assertThat(g.rule(ProductDefinitionGrammar.PRODUCT_IDENTIFICATION))
        .matches("a_product layered_product")
        .matches("a_product customisation_product")
        .matches("a_product config_product");
  }

  @Test
  public void testComment() {
    assertThat(g.rule(ProductDefinitionGrammar.COMMENT))
        .matches("# commented");
  }

  @Test
  public void testDescription() {
    assertThat(g.rule(ProductDefinitionGrammar.FREE_LINE))
        .matches("abc\n")
        .matches("etc etc etc\n")
        .matches("this is the end\n")
        .notMatches("end\n");
    assertThat(g.rule(ProductDefinitionGrammar.DESCRIPTION))
        .matches("description\nend")
        .matches("description\n\t\nend")
        .matches("description\nabc\nend")
        .matches("description\netc\netc\netc\nend")
        .matches("description\n\n\netc\nend")
        .matches("description\nthis is the end\nend");
  }

  @Test
  public void testRequires() {
    assertThat(g.rule(ProductDefinitionGrammar.REQUIRES))
        .matches("requires\nend")
        .matches("requires\nx\nend")
        .matches("requires\nx 1\nend")
        .matches("requires\nx 1\ny 2\nend");
  }

  @Test
  public void testTitle() {
    assertThat(g.rule(ProductDefinitionGrammar.TITLE))
        .matches("title\nend")
        .matches("title\n\t\nend")
        .matches("title\nabc\nend")
        .matches("title\netc\netc\netc\nend");
  }

  @Test
  public void testVersion() {
    assertThat(g.rule(ProductDefinitionGrammar.VERSION))
        .matches("version 1.0")
        .matches("version 1.0.1")
        .matches("version 1.0.1 RC1")
        .matches("version 1.0 RC1");
  }

  @Test
  public void testProductsDefinitions() {
    assertThat(g.rule(ProductDefinitionGrammar.PRODUCT_DEFINITION))
        .matches("id layered_product\n#comment");
  }
}
