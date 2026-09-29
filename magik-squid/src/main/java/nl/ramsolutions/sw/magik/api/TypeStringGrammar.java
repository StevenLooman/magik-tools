package nl.ramsolutions.sw.magik.api;

import com.sonar.sslr.api.GenericTokenType;
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
  TYPE_STRING_INPUT,
  EXPRESSION_RESULT_STRING_INPUT,
  TYPE_STRING,
  EXPRESSION_RESULT_STRING,

  // Elements.
  TYPE_UNDEFINED,
  TYPE_SELF,
  TYPE_CLONE,
  TYPE_PARAMETER_REFERENCE,
  TYPE_SLOT_REFERENCE,
  TYPE_IDENTIFIER,

  TYPE_INVOKABLE,

  TYPE_GENERICS,
  TYPE_TUPLE,
  TYPE_GENERIC_DEFINITION_SINGLE,
  TYPE_GENERIC_REFERENCE_SINGLE,
  TYPE_GENERIC_DEFINITION,
  TYPE_GENERIC_REFERENCE,

  SIMPLE_IDENTIFIER,

  EXPRESSION_RESULT_STRING_UNDEFINED;

  /** TypeString Punctuators. */
  public enum Punctuator implements GrammarRuleKey {
    TYPE_SEPARATOR(","),
    TYPE_VARIADIC("..."),
    TYPE_COMBINATOR("|"),
    TYPE_ARG_OPEN("("),
    TYPE_ARG_CLOSE(")"),
    TYPE_GENERIC_OPEN("<"),
    TYPE_GENERIC_CLOSE(">"),
    TYPE_GENERIC_SEPARATOR(","),
    TYPE_GENERIC_ASSIGN("="),
    TYPE_TUPLE_OPEN("["),
    TYPE_TUPLE_CLOSE("]");

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
    TYPE_STRING_SLOT("_slot"),
    TYPE_STRING_GENERIC("_generic"),
    TYPE_STRING_SELF("_self"),
    TYPE_STRING_CLONE("_clone"),
    TYPE_STRING_INVOKABLE("_invokable"),
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
  // Only an explicit `_anon:` package admits a leading `_`, so a mistyped marker stays an error.
  private static final String ANONYMOUS_IDENTIFIER_REGEXP =
      "_anon:([a-zA-Z_!?]|\\\\.)([a-zA-Z0-9_!?]|\\\\.)*";
  private static final String TYPE_IDENTIFIER_REGEXP =
      ANONYMOUS_IDENTIFIER_REGEXP
          + "|("
          + SIMPLE_IDENTIFIER_REGEXP
          + ":)?"
          + SIMPLE_IDENTIFIER_REGEXP;

  /**
   * Create a new LexerlessGrammar for TypeDoc.
   *
   * @param rootRule Root rule. Production uses the end-of-input-anchored {@code TYPE_STRING_INPUT}
   *     / {@code EXPRESSION_RESULT_STRING_INPUT}; the bare {@code TYPE_STRING} / {@code
   *     EXPRESSION_RESULT_STRING} are available for rule-level tests.
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
    b.rule(TYPE_SELF).is(Keyword.TYPE_STRING_SELF, b.optional(TYPE_GENERICS));
    b.rule(TYPE_CLONE).is(Keyword.TYPE_STRING_CLONE);
    b.rule(TYPE_INVOKABLE).is(Keyword.TYPE_STRING_INVOKABLE, b.optional(TYPE_GENERICS));

    b.rule(TYPE_PARAMETER_REFERENCE)
        .is(
            Keyword.TYPE_STRING_PARAMETER,
            Punctuator.TYPE_ARG_OPEN,
            SIMPLE_IDENTIFIER,
            Punctuator.TYPE_ARG_CLOSE,
            b.optional(TYPE_GENERIC_REFERENCE_SINGLE));
    b.rule(TYPE_SLOT_REFERENCE)
        .is(
            Keyword.TYPE_STRING_SLOT,
            Punctuator.TYPE_ARG_OPEN,
            SIMPLE_IDENTIFIER,
            Punctuator.TYPE_ARG_CLOSE,
            b.optional(TYPE_GENERIC_REFERENCE_SINGLE));

    b.rule(TYPE_GENERIC_DEFINITION_SINGLE)
        .is(Punctuator.TYPE_GENERIC_OPEN, TYPE_GENERIC_DEFINITION, Punctuator.TYPE_GENERIC_CLOSE)
        .skip();
    b.rule(TYPE_GENERIC_REFERENCE_SINGLE)
        .is(Punctuator.TYPE_GENERIC_OPEN, TYPE_GENERIC_REFERENCE, Punctuator.TYPE_GENERIC_CLOSE)
        .skip();
    b.rule(TYPE_GENERIC_DEFINITION)
        .is(TYPE_IDENTIFIER, Punctuator.TYPE_GENERIC_ASSIGN, b.firstOf(TYPE_TUPLE, TYPE_STRING));
    b.rule(TYPE_GENERIC_REFERENCE).is(SIMPLE_IDENTIFIER);

    b.rule(TYPE_IDENTIFIER)
        .is(SPACING_NO_LB, b.regexp(TYPE_IDENTIFIER_REGEXP), b.optional(TYPE_GENERICS));

    b.rule(TYPE_TUPLE)
        .is(
            Punctuator.TYPE_TUPLE_OPEN,
            b.optional(
                b.nextNot(Punctuator.TYPE_TUPLE_CLOSE),
                TYPE_STRING,
                b.zeroOrMore(Punctuator.TYPE_SEPARATOR, TYPE_STRING),
                b.optional(Punctuator.TYPE_VARIADIC)),
            Punctuator.TYPE_TUPLE_CLOSE);

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
                        TYPE_INVOKABLE,
                        TYPE_GENERIC_DEFINITION_SINGLE,
                        TYPE_GENERIC_REFERENCE_SINGLE,
                        TYPE_PARAMETER_REFERENCE,
                        TYPE_SLOT_REFERENCE,
                        TYPE_IDENTIFIER),
                    b.zeroOrMore(
                        Punctuator.TYPE_COMBINATOR,
                        b.firstOf(
                            TYPE_UNDEFINED,
                            TYPE_SELF,
                            TYPE_CLONE,
                            TYPE_INVOKABLE,
                            TYPE_GENERIC_DEFINITION_SINGLE,
                            TYPE_GENERIC_REFERENCE_SINGLE,
                            TYPE_PARAMETER_REFERENCE,
                            TYPE_SLOT_REFERENCE,
                            TYPE_IDENTIFIER))),
                SYNTAX_ERROR));

    b.rule(EXPRESSION_RESULT_STRING)
        .is(
            b.firstOf(
                EXPRESSION_RESULT_STRING_UNDEFINED,
                b.sequence(
                    TYPE_STRING,
                    b.zeroOrMore(Punctuator.TYPE_SEPARATOR, TYPE_STRING),
                    b.optional(Punctuator.TYPE_VARIADIC))));

    b.rule(EXPRESSION_RESULT_STRING_UNDEFINED).is(Keyword.EXPRESSION_RESULT_UNDEFINED);

    b.rule(SYNTAX_ERROR).is(b.regexp(".*"));

    b.rule(TYPE_STRING_INPUT)
        .is(
            b.firstOf(
                b.sequence(TYPE_STRING, SPACING, b.token(GenericTokenType.EOF, b.endOfInput())),
                SYNTAX_ERROR));

    b.rule(EXPRESSION_RESULT_STRING_INPUT)
        .is(
            b.firstOf(
                b.sequence(
                    EXPRESSION_RESULT_STRING,
                    SPACING,
                    b.token(GenericTokenType.EOF, b.endOfInput())),
                SYNTAX_ERROR));

    b.setRootRule(rootRule);

    return b.build();
  }

  private static void punctuators(final LexerlessGrammarBuilder b) {
    for (final Punctuator p : Punctuator.values()) {
      if (p == Punctuator.TYPE_VARIADIC) {
        b.rule(p).is(SPACING_NO_LB, p.getValue());
      } else {
        b.rule(p).is(SPACING_NO_LB, p.getValue()).skip();
      }
    }
  }

  private static void keywords(final LexerlessGrammarBuilder b) {
    for (final Keyword k : Keyword.values()) {
      b.rule(k).is(SPACING, b.regexp("(?i)" + k.getValue() + "(?!\\w)")).skip();
    }
  }
}
