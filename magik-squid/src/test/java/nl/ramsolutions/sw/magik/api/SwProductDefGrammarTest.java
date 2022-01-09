package nl.ramsolutions.sw.magik.api;

import com.sonar.sslr.api.Grammar;
import nl.ramsolutions.sw.definitions.api.SwProductDefGrammar;
import org.junit.jupiter.api.Test;

import static org.sonar.sslr.tests.Assertions.assertThat;

/**
 * Test ProductDefinitionGrammar.
 */
class SwProductDefGrammarTest {
    private Grammar g = SwProductDefGrammar.create();

    @Test
    void testNumber() {
        assertThat(g.rule(SwProductDefGrammar.NUMBER))
            .matches("1");
    }

    @Test
    void testIdentifier() {
        assertThat(g.rule(SwProductDefGrammar.IDENTIFIER))
            .matches("test_product");
    }

    @Test
    void testProductIdentifictaion() {
        assertThat(g.rule(SwProductDefGrammar.PRODUCT_IDENTIFICATION))
            .matches("a_product layered_product")
            .matches("a_product customisation_product")
            .matches("a_product config_product");
    }

    @Test
    void testComment() {
        assertThat(g.rule(SwProductDefGrammar.COMMENT))
            .matches("# commented");
    }

    @Test
    void testDescription() {
        assertThat(g.rule(SwProductDefGrammar.FREE_LINE))
            .matches("abc\n")
            .matches("etc etc etc\n")
            .matches("this is the end\n")
            .notMatches("end\n");
        assertThat(g.rule(SwProductDefGrammar.DESCRIPTION))
            .matches("description\nend")
            .matches("description\n\t\nend")
            .matches("description\nabc\nend")
            .matches("description\netc\netc\netc\nend")
            .matches("description\n\n\netc\nend")
            .matches("description\nthis is the end\nend");
    }

    @Test
    void testRequires() {
        assertThat(g.rule(SwProductDefGrammar.REQUIRES))
            .matches("requires\nend")
            .matches("requires\nx\nend")
            .matches("requires\nx 1\nend")
            .matches("requires\nx 1\ny 2\nend");
    }

    @Test
    void testTitle() {
        assertThat(g.rule(SwProductDefGrammar.TITLE))
            .matches("title\nend")
            .matches("title\n\t\nend")
            .matches("title\nabc\nend")
            .matches("title\netc\netc\netc\nend");
    }

    @Test
    void testVersion() {
        assertThat(g.rule(SwProductDefGrammar.VERSION))
            .matches("version 1.0")
            .matches("version 1.0.1")
            .matches("version 1.0.1 RC1")
            .matches("version 1.0 RC1");
    }

    @Test
    void testProductsDefinitions() {
        assertThat(g.rule(SwProductDefGrammar.PRODUCT_DEFINITION))
            .matches("id layered_product\n#comment");
    }
}