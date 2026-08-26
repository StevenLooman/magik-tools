package nl.ramsolutions.sw.magik.api;

import org.junit.jupiter.api.Test;
import org.sonar.sslr.parser.LexerlessGrammar;
import org.sonar.sslr.tests.Assertions;

/** Tests for TypeDocGrammar. */
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
        .matches("## @generic E")
        .matches("## @generic E Elements elements.")
        .matches("## @generic {sw:rope} E")
        .matches("## @generic {user:thing} E Elements elements.");
  }

  @Test
  void testMethodInvocation() {
    Assertions.assertThat(g.rule(TypeDocGrammar.METHOD_INVOCATION))
        .matches("{rope.new()}")
        .matches("{rope.size}")
        .matches("{sw:rope.new()}")
        .matches("{sw:rope.fast_elements()}")
        .matches("{user:my_class.method()}")
        .matches("{rope[]}")
        .matches("{vector[]}")
        .matches("{sw:rope[]}")
        .matches("{sw:vector_key_equality_table[,]}")
        .matches("{user:table[,]}")
        .notMatches("rope.new()")
        .notMatches("{invalid}")
        .notMatches("{.method()}")
        .notMatches("{type.}")
        .notMatches("{:}");
  }

  @Test
  void testInvokesMethod() {
    Assertions.assertThat(g.rule(TypeDocGrammar.INVOKES_METHOD))
        .matches("## @invokes_method {rope.new()}")
        .matches("## @invokes_method {rope.size}")
        .matches("## @invokes_method {sw:rope.new()}")
        .matches("## @invokes_method {sw:rope.fast_elements()}")
        .matches("## @invokes_method {rope[]}")
        .matches("## @invokes_method {sw:vector_key_equality_table[,]}")
        .matches("## @invokes_method invalid_syntax"); // Falls back to SYNTAX_ERROR.
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
        .matches(
            """
                ## This is a method
                ## @param {user:thing} p1 Param1 description
                ## @return {sw:rope|sw:unset}
                """)
        .matches(
            """
                ## This is spread
                ## over multiple lines with a phony @param.
                ## @param {user:thing} p1 Param1 description
                ## @return {sw:rope}
                """)
        .matches(
            """
            ## @param {user:thing} p1 Param1 description
            ## @return {sw:rope}
            """)
        .matches(
            """
            ## Method with invokes_method annotation
            ## @param {sw:rope} data The data to process
            ## @invokes_method {sw:rope.new()}
            ## @invokes_method {sw:integer.write()}
            ## @return {sw:rope}
            """);
  }
}
