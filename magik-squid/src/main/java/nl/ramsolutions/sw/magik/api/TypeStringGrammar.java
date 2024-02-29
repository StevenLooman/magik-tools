package nl.ramsolutions.sw.magik.api;

import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.grammar.LexerlessGrammarBuilder;
import org.sonar.sslr.parser.LexerlessGrammar;

/** TypeString grammar. */
@SuppressWarnings("checkstyle:JavadocVariable")
public enum TypeStringGrammar implements GrammarRuleKey {

  // Spacing.
  WHITESPACE,
  SPACING,
  SPACING_NO_LB,

  // Syntax error.
  SYNTAX_ERROR,

  // Root.
  TYPE_STRING,
  EXPRESSION_RESULT_STRING,

  // Elements.
  TYPE_UNDEFINED,
  TYPE_SELF,
  TYPE_CLONE,
  TYPE_PARAMETER_REFERENCE,
  TYPE_IDENTIFIER,

  TYPE_GENERICS,
  TYPE_GENERIC_DEFINITION_SINGLE,
  TYPE_GENERIC_REFERENCE_SINGLE,
  TYPE_GENERIC_DEFINITION,
  TYPE_GENERIC_REFERENCE,

  SIMPLE_IDENTIFIER,

  EXPRESSION_RESULT_STRING_UNDEFINED;

  /** TypeString Punctuators. */
  public enum Punctuator implements GrammarRuleKey {
    TYPE_SEPARATOR(","),
    TYPE_COMBINATOR("|"),
    TYPE_ARG_OPEN("("),
    TYPE_ARG_CLOSE(")"),
    TYPE_GENERIC_OPEN("<"),
    TYPE_GENERIC_CLOSE(">"),
    TYPE_GENERIC_SEPARATOR(","),
    TYPE_GENERIC_ASSIGN("=");

    private final String value;

    Punctuator(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

  /** TypeString Keywords. */
  public enum Keyword implements GrammarRuleKey {
    TYPE_STRING_UNDEFINED("_undefined"),
    TYPE_STRING_PARAMETER("_parameter"),
    TYPE_STRING_GENERIC("_generic"),
    TYPE_STRING_SELF("_self"),
    TYPE_STRING_CLONE("_clone"),
    EXPRESSION_RESULT_UNDEFINED("__undefined_result__");

    private final String value;

    Keyword(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

  private static final String LINE_TERMINATOR_REGEXP = "\\n\\r";
  private static final String WHITESPACE_REGEXP = "\\t\\v\\f\\u0020\\u00A0\\uFEFF";

  private static final String SIMPLE_IDENTIFIER_REGEXP =
      "([a-zA-Z!?]|\\\\.)([a-zA-Z0-9_!?]|\\\\.)*";
  private static final String TYPE_IDENTIFIER_REGEXP =
      "(" + SIMPLE_IDENTIFIER_REGEXP + ":)?" + SIMPLE_IDENTIFIER_REGEXP;

  /**
   * Create a new LexerlessGrammar for TypeDoc.
   *
   * @param rootRule Root rules. Either {@code TYPE_STRING} or {@code EXPRESSION_RESULT_STRING}.
   * @return TypeDoc grammar.
   */
  public static LexerlessGrammar create(final TypeStringGrammar rootRule) {
    final LexerlessGrammarBuilder b = LexerlessGrammarBuilder.create();

    b.rule(WHITESPACE).is(b.regexp("[" + LINE_TERMINATOR_REGEXP + WHITESPACE_REGEXP + "]*+"));
    b.rule(SPACING).is(b.skippedTrivia(WHITESPACE)).skip();
    b.rule(SPACING_NO_LB).is(b.zeroOrMore(b.skippedTrivia(b.regexp("[\\s&&[^\n\r]]++")))).skip();

    TypeStringGrammar.punctuators(b);
    TypeStringGrammar.keywords(b);

    b.rule(SIMPLE_IDENTIFIER).is(SPACING_NO_LB, b.regexp(SIMPLE_IDENTIFIER_REGEXP));
    b.rule(TYPE_UNDEFINED).is(Keyword.TYPE_STRING_UNDEFINED);
    b.rule(TYPE_SELF).is(Keyword.TYPE_STRING_SELF);
    b.rule(TYPE_CLONE).is(Keyword.TYPE_STRING_CLONE);

    b.rule(TYPE_PARAMETER_REFERENCE)
        .is(
            Keyword.TYPE_STRING_PARAMETER,
            Punctuator.TYPE_ARG_OPEN,
            SIMPLE_IDENTIFIER,
            Punctuator.TYPE_ARG_CLOSE);

    b.rule(TYPE_GENERIC_DEFINITION_SINGLE)
        .is(Punctuator.TYPE_GENERIC_OPEN, TYPE_GENERIC_DEFINITION, Punctuator.TYPE_GENERIC_CLOSE)
        .skip();
    b.rule(TYPE_GENERIC_REFERENCE_SINGLE)
        .is(Punctuator.TYPE_GENERIC_OPEN, TYPE_GENERIC_REFERENCE, Punctuator.TYPE_GENERIC_CLOSE)
        .skip();
    b.rule(TYPE_GENERIC_DEFINITION)
        .is(TYPE_IDENTIFIER, Punctuator.TYPE_GENERIC_ASSIGN, TYPE_STRING);
    b.rule(TYPE_GENERIC_REFERENCE).is(SIMPLE_IDENTIFIER);

    b.rule(TYPE_IDENTIFIER)
        .is(SPACING_NO_LB, b.regexp(TYPE_IDENTIFIER_REGEXP), b.optional(TYPE_GENERICS));

    b.rule(TYPE_GENERICS)
        .is(
            Punctuator.TYPE_GENERIC_OPEN,
            b.firstOf(TYPE_GENERIC_DEFINITION, TYPE_GENERIC_REFERENCE),
            b.zeroOrMore(
                Punctuator.TYPE_GENERIC_SEPARATOR,
                b.firstOf(TYPE_GENERIC_DEFINITION, TYPE_GENERIC_REFERENCE)),
            Punctuator.TYPE_GENERIC_CLOSE)
        .skip();

    b.rule(TYPE_STRING)
        .is(
            b.firstOf(
                b.sequence(
                    b.firstOf(
                        TYPE_UNDEFINED,
                        TYPE_SELF,
                        TYPE_CLONE,
                        TYPE_GENERIC_DEFINITION_SINGLE,
                        TYPE_GENERIC_REFERENCE_SINGLE,
                        TYPE_PARAMETER_REFERENCE,
                        TYPE_IDENTIFIER),
                    b.zeroOrMore(
                        Punctuator.TYPE_COMBINATOR,
                        b.firstOf(
                            TYPE_UNDEFINED,
                            TYPE_SELF,
                            TYPE_CLONE,
                            TYPE_GENERIC_DEFINITION_SINGLE,
                            TYPE_GENERIC_REFERENCE_SINGLE,
                            TYPE_PARAMETER_REFERENCE,
                            TYPE_IDENTIFIER))),
                SYNTAX_ERROR));

    b.rule(EXPRESSION_RESULT_STRING)
        .is(
            b.firstOf(
                EXPRESSION_RESULT_STRING_UNDEFINED,
                b.sequence(TYPE_STRING, b.zeroOrMore(Punctuator.TYPE_SEPARATOR, TYPE_STRING))));

    b.rule(EXPRESSION_RESULT_STRING_UNDEFINED).is(Keyword.EXPRESSION_RESULT_UNDEFINED);

    b.rule(SYNTAX_ERROR).is(b.regexp(".*"));

    b.setRootRule(rootRule);

    return b.build();
  }

  private static void punctuators(final LexerlessGrammarBuilder b) {
    for (final Punctuator p : Punctuator.values()) {
      b.rule(p).is(SPACING_NO_LB, p.getValue()).skip();
    }
  }

  private static void keywords(final LexerlessGrammarBuilder b) {
    for (final Keyword k : Keyword.values()) {
      b.rule(k).is(SPACING, b.regexp("(?i)" + k.getValue() + "(?!\\w)")).skip();
    }
  }
}
