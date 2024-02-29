package nl.ramsolutions.sw.magik.api;

import org.junit.jupiter.api.Test;
import org.sonar.sslr.parser.LexerlessGrammar;
import org.sonar.sslr.tests.Assertions;

/** Tests for TypeStringGrammar. */
class TypeStringGrammarTest {

  private final LexerlessGrammar grammarTypeString =
      TypeStringGrammar.create(TypeStringGrammar.TYPE_STRING);
  private final LexerlessGrammar grammarExpressionResultString =
      TypeStringGrammar.create(TypeStringGrammar.EXPRESSION_RESULT_STRING);

  @Test
  void testTypeString() {
    Assertions.assertThat(grammarTypeString.rule(TypeStringGrammar.TYPE_STRING))
        .matches("integer")
        .matches(" sw:integer")
        .matches("_self")
        .matches(" _clone")
        .matches("_undefined")
        .matches("_self|sw:unset")
        .matches("integer|float")
        .matches("sw:integer| sw:float")
        .matches("<E>")
        .matches("sw:rope<E=sw:float>")
        .matches("sw:property_list<K=symbol, E=float>")
        .matches("sw:rope<E=sw:property_list<K=sw:symbol, E=sw:float>>");
  }

  @Test
  void testExpressionResultString() {
    Assertions.assertThat(
            grammarExpressionResultString.rule(TypeStringGrammar.EXPRESSION_RESULT_STRING))
        .matches("integer")
        .matches("_self")
        .matches("_self|sw:unset")
        .matches("integer,integer")
        .matches("integer, integer")
        .matches("integer, sw:integer")
        .matches("integer, sw:integer|sw:float")
        .matches("sw:rope<E=sw:float>, integer")
        .matches("<E>, integer")
        .matches("sw:property_list<K=symbol, E=float>, sw:property_list<K=symbol, E=float>")
        .matches("__UNDEFINED_RESULT__");
  }
}
