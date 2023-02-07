package nl.ramsolutions.sw.magik.api;

import org.junit.jupiter.api.Test;
import org.sonar.sslr.parser.LexerlessGrammar;
import org.sonar.sslr.tests.Assertions;

/**
 * Tests for TypeDocGrammar.
 */
class TypeDocGrammarTest {

    private final LexerlessGrammar g = TypeDocGrammar.create();

    @Test
    void testFunction() {
        Assertions.assertThat(g.rule(TypeDocGrammar.FUNCTION))
            .matches("## This is an example function")
            .matches("## This is spread\n## over multiple lines.")
            .matches("## This is spread\n## over multiple lines with a phony @param.")
            .notMatches("## @param a");
    }

    @Test
    void testParam() {
        Assertions.assertThat(g.rule(TypeDocGrammar.PARAM))
            .matches("## @param a")
            .matches("## @param a Aaaaa aaa.")
            .matches("## @param {sw:rope} p1")
            .matches("## @param {sw:rope<sw:symbol>} p1")
            .matches("## @param {sw:rope<sw:symbol>|sw:simple_vector<sw:symbol>} p1")
            .matches("## @param {sw:property_list<sw:symbol,sw:integer>}")
            .matches("## @param {user:thing} p1 Aaaaa aaa.");
    }

    @Test
    void testReturn() {
        Assertions.assertThat(g.rule(TypeDocGrammar.RETURN))
            .matches("## @return Aaaaa aaa.")
            .matches("## @return {sw:rope}")
            .matches("## @return {sw:rope<sw:symbol>}")
            .matches("## @return {user:thing} Aaaaa aaa.")
            .matches("## @return {_self}")
            .matches("## @return {_parameter(p1)}");
    }

    @Test
    void testLoop() {
        Assertions.assertThat(g.rule(TypeDocGrammar.LOOP))
            .matches("## @loop Aaaaa aaa.")
            .matches("## @loop {sw:rope}")
            .matches("## @loop {sw:rope<sw:symbol>}")
            .matches("## @loop {user:thing} Aaaaa aaa.");
    }

    @Test
    void testSlot() {
        Assertions.assertThat(g.rule(TypeDocGrammar.SLOT))
            .matches("## @slot slot1")
            .matches("## @slot slot1 Aaaaa aaa.")
            .matches("## @slot {sw:rope} slot1")
            .matches("## @slot {sw:rope<sw:symbol|sw:char16_vector>} slot1")
            .matches("## @slot {user:thing} slot1 Aaaaa aaa.");
    }

    @Test
    void testGeneric() {
        Assertions.assertThat(g.rule(TypeDocGrammar.GENERIC))
            .matches("## @generic slot1")
            .matches("## @generic slot1 Aaaaa aaa.")
            .matches("## @generic {sw:rope} slot1")
            .matches("## @generic {user:thing} slot1 Aaaaa aaa.");
    }

    @Test
    void testType() {
        Assertions.assertThat(g.rule(TypeDocGrammar.TYPE))
            .matches("{integer}")
            .matches("{ integer}")
            .matches("{sw:integer}")
            .matches("{ sw:integer}")
            .matches("{float|integer}")
            .matches("{sw:rope<sw:integer>}")
            .matches("{sw:property_list<sw:symbol, sw:object>}");
    }

    @Test
    void testTypeDoc() {
        Assertions.assertThat(g.rule(TypeDocGrammar.TYPE_DOC))
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
