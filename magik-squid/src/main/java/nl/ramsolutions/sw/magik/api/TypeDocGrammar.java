package nl.ramsolutions.sw.magik.api;

import com.sonar.sslr.api.GenericTokenType;
import java.util.Arrays;
import java.util.Locale;
import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.grammar.LexerlessGrammarBuilder;
import org.sonar.sslr.parser.LexerlessGrammar;

/** TypeDoc grammar. */
@SuppressWarnings({"checkstyle:JavadocVariable", "checkstyle:MethodLength"})
public enum TypeDocGrammar implements GrammarRuleKey {

  // spacing
  WHITESPACE,
  SPACING,
  SPACING_NO_LB,
  NEXT_NOT_LB,
  DOC_START,

  // specials
  SYNTAX_ERROR,
  ANY_ELEMENT,

  // doc elements
  FUNCTION,
  PARAM,
  RETURN,
  LOOP,
  SLOT,
  GENERIC,
  INVOKES_METHOD,

  // parts
  NAME,
  TYPE,
  TYPE_VALUE,
  DESCRIPTION,
  METHOD_INVOCATION,
  METHOD_INVOCATION_TYPE,
  METHOD_INVOCATION_PACKAGE,
  METHOD_INVOCATION_TYPE_NAME,
  METHOD_INVOCATION_METHOD_NAME,

  // root
  TYPE_DOC;

  /** TypeDoc Doc elements. */
  public enum Element implements GrammarRuleKey {
    PARAM,
    RETURN,
    LOOP,
    SLOT,
    GENERIC,
    INVOKES_METHOD;

    static final String START_CHAR = "@";

    /**
     * Get all element values.
     *
     * @return Element values
     */
    public static String[] elementValues() {
      final String[] keywordsValue = new String[Element.values().length];
      int idx = 0;
      for (final Element keyword : Element.values()) {
        keywordsValue[idx] = keyword.getValue();
        idx++;
      }
      return keywordsValue;
    }

    /**
     * Get value of keyword, prefixed with <code>_</code>.
     *
     * @return Value of keyword
     */
    public String getValue() {
      return START_CHAR + toString().toLowerCase(Locale.ENGLISH);
    }
  }

  /** TypeDoc Punctuators. */
  public enum Punctuator implements GrammarRuleKey {
    TYPE_OPEN("{"),
    TYPE_CLOSE("}");

    private final String value;

    Punctuator(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

  private static final String LINE_TERMINATOR_REGEXP = "\\n\\r";
  private static final String WHITESPACE_REGEXP = "\\t\\v\\f\\u0020\\u00A0\\uFEFF";
  private static final String DOC_START_REGEXP = "##";
  private static final String FUNCTION_LINE_REGEXP = ".*";
  private static final String DESCRIPTION_REGEXP = ".*";

  private static final String SIMPLE_IDENTIFIER_REGEXP =
      "([a-zA-Z!?]|\\\\.)([a-zA-Z0-9_!?]|\\\\.)*";

  // Method call component patterns.
  private static final String PACKAGE_NAME_REGEXP = "[a-z_][a-z0-9_]*";
  private static final String TYPE_NAME_REGEXP = "([a-zA-Z!?]|\\\\.)([a-zA-Z0-9_!?]|\\\\.)*";
  private static final String DOT_METHOD_NAME_REGEXP =
      "([a-zA-Z!?]|\\\\.)([a-zA-Z0-9_!?]|\\\\.|\\(|\\))*";
  private static final String BRACKET_METHOD_REGEXP = "\\[[^\\]]*\\]";

  /**
   * Create a new LexerlessGrammar for TypeDoc.
   *
   * @return TypeDoc grammar.
   */
  public static LexerlessGrammar create() {
    final LexerlessGrammarBuilder b = LexerlessGrammarBuilder.create();

    b.rule(WHITESPACE).is(b.regexp("[" + LINE_TERMINATOR_REGEXP + WHITESPACE_REGEXP + "]*+"));
    b.rule(SPACING).is(b.skippedTrivia(WHITESPACE)).skip();
    b.rule(SPACING_NO_LB).is(b.zeroOrMore(b.skippedTrivia(b.regexp("[\\s&&[^\n\r]]++")))).skip();
    b.rule(NEXT_NOT_LB).is(b.nextNot(b.regexp("[\n\r]"))).skip();
    b.rule(DOC_START).is(SPACING, b.regexp(DOC_START_REGEXP));

    TypeDocGrammar.elements(b);
    TypeDocGrammar.punctuators(b);

    b.rule(TYPE_DOC)
        .is(
            b.optional(FUNCTION),
            b.zeroOrMore(b.firstOf(PARAM, RETURN, LOOP, SLOT, GENERIC, INVOKES_METHOD)),
            SPACING,
            b.token(GenericTokenType.EOF, b.endOfInput()));

    b.rule(FUNCTION)
        .is(
            b.oneOrMore(
                DOC_START, SPACING_NO_LB, b.nextNot(ANY_ELEMENT), b.regexp(FUNCTION_LINE_REGEXP)));
    b.rule(NAME).is(SPACING_NO_LB, b.optional(b.regexp(SIMPLE_IDENTIFIER_REGEXP)));
    b.rule(DESCRIPTION)
        .is(
            SPACING_NO_LB,
            b.regexp(DESCRIPTION_REGEXP),
            b.zeroOrMore(
                DOC_START, SPACING_NO_LB, b.nextNot(ANY_ELEMENT), b.regexp(DESCRIPTION_REGEXP)));

    b.rule(SYNTAX_ERROR).is(SPACING_NO_LB, b.regexp(".*"));
    b.rule(PARAM).is(DOC_START, Element.PARAM, b.optional(TYPE), NAME, DESCRIPTION);
    b.rule(RETURN).is(DOC_START, Element.RETURN, b.optional(TYPE), DESCRIPTION);
    b.rule(LOOP).is(DOC_START, Element.LOOP, b.optional(TYPE), DESCRIPTION);
    b.rule(SLOT).is(DOC_START, Element.SLOT, b.optional(TYPE), NAME, DESCRIPTION);
    b.rule(GENERIC).is(DOC_START, Element.GENERIC, b.optional(TYPE), NAME, DESCRIPTION);

    b.rule(METHOD_INVOCATION_PACKAGE).is(b.regexp(PACKAGE_NAME_REGEXP), ":");
    b.rule(METHOD_INVOCATION_TYPE_NAME).is(b.regexp(TYPE_NAME_REGEXP));
    b.rule(METHOD_INVOCATION_METHOD_NAME)
        .is(
            b.firstOf(
                b.sequence(".", b.regexp(DOT_METHOD_NAME_REGEXP)), // dot notation: .method()
                b.regexp(BRACKET_METHOD_REGEXP))); // bracket notation: []
    b.rule(METHOD_INVOCATION_TYPE)
        .is(
            b.optional(METHOD_INVOCATION_PACKAGE),
            METHOD_INVOCATION_TYPE_NAME,
            METHOD_INVOCATION_METHOD_NAME);
    b.rule(METHOD_INVOCATION)
        .is(SPACING_NO_LB, Punctuator.TYPE_OPEN, METHOD_INVOCATION_TYPE, Punctuator.TYPE_CLOSE);

    b.rule(INVOKES_METHOD)
        .is(DOC_START, Element.INVOKES_METHOD, b.firstOf(METHOD_INVOCATION, SYNTAX_ERROR));

    b.rule(TYPE_VALUE).is(b.regexp(TypeDocGrammar.anythingButPunctuator(Punctuator.TYPE_CLOSE)));
    b.rule(TYPE).is(Punctuator.TYPE_OPEN, TYPE_VALUE, Punctuator.TYPE_CLOSE);

    b.setRootRule(TYPE_DOC);

    return b.build();
  }

  private static void elements(final LexerlessGrammarBuilder b) {
    for (final Element e : Element.values()) {
      b.rule(e).is(SPACING_NO_LB, b.regexp("(?i)" + e.getValue() + "(?!\\w)")).skip();
    }

    // ANY_ELEMENT.
    final Object[] elementRegexs =
        Arrays.stream(Element.values())
            .map(e -> b.regexp("(?i)" + e.getValue() + "(?!\\w)"))
            .toList()
            .toArray();
    b.rule(ANY_ELEMENT)
        .is(
            b.firstOf(
                elementRegexs[0],
                elementRegexs[1],
                Arrays.copyOfRange(elementRegexs, 2, elementRegexs.length)));
  }

  private static void punctuators(final LexerlessGrammarBuilder b) {
    for (final Punctuator p : Punctuator.values()) {
      b.rule(p).is(SPACING_NO_LB, p.getValue()).skip();
    }
  }

  private static String anythingButPunctuator(final Punctuator punctuator) {
    return "(?s).+?(?=\\" + punctuator.getValue() + ")";
  }
}
