package nl.ramsolutions.sw.magik.api;

import com.sonar.sslr.api.GenericTokenType;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;
import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.grammar.LexerlessGrammarBuilder;
import org.sonar.sslr.parser.LexerlessGrammar;

/**
 * NewDoc grammar.
 */
@SuppressWarnings({"checkstyle:JavadocVariable", "checkstyle:MethodLength"})
public enum NewDocGrammar implements GrammarRuleKey {

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

    NAME,
    TYPE,
    TYPE_NAME,
    DESCRIPTION,

    TYPE_SELF,
    TYPE_CLONE,
    TYPE_PARAMETER_REFERENCE,

    // root
    NEW_DOC;

    /**
     * NewDoc Doc elements.
     */
    public enum Element implements GrammarRuleKey {

        PARAM,
        RETURN,
        LOOP,
        SLOT;

        static final String START_CHAR = "@";

        /**
         * Get all element values.
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
         * @return Value of keyword
         */
        public String getValue() {
            return START_CHAR + toString().toLowerCase(Locale.ENGLISH);
        }

    }

    /**
     * NewDoc Punctuators.
     */
    public enum Punctuator implements GrammarRuleKey {

        TYPE_SEPARATOR(","),
        TYPE_COMBINATOR("|"),
        TYPE_OPEN("{"),
        TYPE_CLOSE("}"),
        TYPE_ARG_OPEN("("),
        TYPE_ARG_CLOSE(")"),
        TYPE_PARAM_REF("_parameter");

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

    private static final String SIMPLE_IDENTIFIER_REGEXP = "([a-zA-Z!?]|\\\\.)([a-zA-Z0-9_!?]|\\\\.)*";
    private static final String TYPE_IDENTIFIER_REGEXP =
        "(" + SIMPLE_IDENTIFIER_REGEXP + ":)?" + SIMPLE_IDENTIFIER_REGEXP;
    private static final String SELF_REGEXP = "(?i)" + MagikKeyword.SELF.getValue() + "(?!\\w)";
    private static final String CLONE_REGEXP = "(?i)" + MagikKeyword.CLONE.getValue() + "(?!\\w)";

    /**
     * Create a new LexerlessGrammar for the Magik language.
     * @return Grammar for the Magik language
     */
    public static LexerlessGrammar create() {
        final LexerlessGrammarBuilder b = LexerlessGrammarBuilder.create();

        b.rule(WHITESPACE).is(
            b.regexp("[" + LINE_TERMINATOR_REGEXP + WHITESPACE_REGEXP + "]*+"));
        b.rule(SPACING).is(
            b.skippedTrivia(WHITESPACE)).skip();
        b.rule(SPACING_NO_LB).is(
            b.zeroOrMore(
                b.skippedTrivia(b.regexp("[\\s&&[^\n\r]]++")))).skip();
        b.rule(NEXT_NOT_LB).is(
            b.nextNot(b.regexp("[\n\r]"))).skip();
        b.rule(DOC_START).is(
            SPACING,
            b.regexp(DOC_START_REGEXP));

        NewDocGrammar.elements(b);
        NewDocGrammar.punctuators(b);

        b.rule(NEW_DOC).is(
            b.optional(FUNCTION),
            b.zeroOrMore(
                b.firstOf(
                    PARAM,
                    RETURN,
                    LOOP,
                    SLOT)),
            SPACING,
            b.token(GenericTokenType.EOF, b.endOfInput()));

        b.rule(FUNCTION).is(
            b.oneOrMore(
                DOC_START,
                SPACING_NO_LB,
                b.nextNot(ANY_ELEMENT),
                b.regexp(FUNCTION_LINE_REGEXP)));
        b.rule(NAME).is(
            SPACING_NO_LB,
            b.optional(
                b.regexp(SIMPLE_IDENTIFIER_REGEXP)));
        b.rule(DESCRIPTION).is(
            SPACING_NO_LB,
            b.regexp(DESCRIPTION_REGEXP),
            b.zeroOrMore(
                DOC_START,
                SPACING_NO_LB,
                b.nextNot(ANY_ELEMENT),
                b.regexp(DESCRIPTION_REGEXP)));

        b.rule(PARAM).is(
            DOC_START,
            Element.PARAM,
            b.optional(TYPE),
            NAME,
            DESCRIPTION);
        b.rule(RETURN).is(
            DOC_START,
            Element.RETURN,
            b.optional(TYPE),
            DESCRIPTION);
        b.rule(LOOP).is(
            DOC_START,
            Element.LOOP,
            b.optional(TYPE),
            DESCRIPTION);
        b.rule(SLOT).is(
            DOC_START,
            Element.SLOT,
            b.optional(TYPE),
            NAME,
            DESCRIPTION);

        b.rule(TYPE).is(
            Punctuator.TYPE_OPEN,
            b.optional(TYPE_NAME),
            b.zeroOrMore(
                Punctuator.TYPE_COMBINATOR,
                TYPE_NAME),
            Punctuator.TYPE_CLOSE);
        b.rule(TYPE_NAME).is(
            b.firstOf(
                TYPE_SELF,
                TYPE_CLONE,
                TYPE_PARAMETER_REFERENCE,
                b.regexp(TYPE_IDENTIFIER_REGEXP)));

        b.rule(TYPE_SELF).is(
            b.regexp(SELF_REGEXP));
        b.rule(TYPE_CLONE).is(
            b.regexp(CLONE_REGEXP));
        b.rule(TYPE_PARAMETER_REFERENCE).is(
            Punctuator.TYPE_PARAM_REF,
            Punctuator.TYPE_ARG_OPEN,
            b.regexp(SIMPLE_IDENTIFIER_REGEXP),
            Punctuator.TYPE_ARG_CLOSE);

        b.setRootRule(NEW_DOC);

        return b.build();
    }

    private static void elements(final LexerlessGrammarBuilder b) {
        for (final Element e : Element.values()) {
            b.rule(e).is(SPACING_NO_LB, b.regexp("(?i)" + e.getValue() + "(?!\\w)")).skip();
        }

        // ANY_ELEMENT.
        final Object[] elementRegexs = Arrays.stream(Element.values())
            .map(e -> b.regexp("(?i)" + e.getValue() + "(?!\\w)"))
            .collect(Collectors.toList())
            .toArray();
        b.rule(ANY_ELEMENT).is(
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

}
