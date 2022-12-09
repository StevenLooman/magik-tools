package nl.ramsolutions.sw.magik.api;

import org.junit.jupiter.api.Test;
import org.sonar.sslr.parser.LexerlessGrammar;
import org.sonar.sslr.tests.Assertions;

/**
 * Tests for NewDocGrammar.
 */
class NewDocGrammarTest {

    private final LexerlessGrammar g = NewDocGrammar.create();

    @Test
    void testFunction() {
        Assertions.assertThat(g.rule(NewDocGrammar.FUNCTION))
            .matches("## This is an example function")
            .matches("## This is spread\n## over multiple lines.")
            .matches("## This is spread\n## over multiple lines with a phony @param.")
            .notMatches("## @param a");
    }

    @Test
    void testParam() {
        Assertions.assertThat(g.rule(NewDocGrammar.PARAM))
            .matches("## @param a")
            .matches("## @param a Aaaaa aaa.")
            .matches("## @param {sw:rope} p1")
            .matches("## @param {sw:rope|sw:simple_vector} p1")
            .matches("## @param {user:thing} p1 Aaaaa aaa.");
    }

    @Test
    void testReturn() {
        Assertions.assertThat(g.rule(NewDocGrammar.RETURN))
            .matches("## @return Aaaaa aaa.")
            .matches("## @return {sw:rope}")
            .matches("## @return {user:thing} Aaaaa aaa.")
            .matches("## @return {_parameter(p1)}");
    }

    @Test
    void testLoop() {
        Assertions.assertThat(g.rule(NewDocGrammar.LOOP))
            .matches("## @loop Aaaaa aaa.")
            .matches("## @loop {sw:rope}")
            .matches("## @loop {user:thing} Aaaaa aaa.");
    }

    @Test
    void testSlot() {
        Assertions.assertThat(g.rule(NewDocGrammar.SLOT))
            .matches("## @slot slot1")
            .matches("## @slot slot1 Aaaaa aaa.")
            .matches("## @slot {sw:rope} slot1")
            .matches("## @slot {user:thing} slot1 Aaaaa aaa.");
    }

    @Test
    void testNewDoc() {
        Assertions.assertThat(g.rule(NewDocGrammar.NEW_DOC))
            .matches(""
                + "## This is a method\n"
                + "## @param {user:thing} p1 Param1 description\n"
                + "## @return {sw:rope|sw:unset}\n")
            .matches(""
                + "## This is spread\n"
                + "## over multiple lines with a phony @param.\n"
                + "## @param {user:thing} p1 Param1 description\n"
                + "## @return {sw:rope}\n")
            .matches(""
                + "## @param {user:thing} p1 Param1 description\n"
                + "## @return {sw:rope}\n");
    }

}
