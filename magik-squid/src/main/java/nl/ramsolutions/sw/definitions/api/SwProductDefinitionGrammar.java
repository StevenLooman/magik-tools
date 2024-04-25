package nl.ramsolutions.sw.definitions.api;

import com.sonar.sslr.api.GenericTokenType;
import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.grammar.LexerlessGrammarBuilder;
import org.sonar.sslr.parser.LexerlessGrammar;

/** Product definition grammar. */
@SuppressWarnings("checkstyle:JavadocVariable")
public enum SwProductDefinitionGrammar implements GrammarRuleKey {
  PRODUCT_DEFINITION,

  PRODUCT_IDENTIFICATION,
  PRODUCT_NAME,
  PRODUCT_TYPE,

  DESCRIPTION,
  DO_NOT_TRANSLATE,
  REQUIRES,
  OPTIONAL,
  TITLE,
  VERSION,

  PRODUCT_REFS,
  PRODUCT_REF,
  VERSION_NUMBER,
  VERSION_COMMENT,
  FREE_LINES,
  FREE_LINE,

  SYNTAX_ERROR_SECTION,
  SYNTAX_ERROR_LINE,

  SPACING,
  NEWLINE,
  WHITESPACE,
  COMMENT,
  IDENTIFIER,
  NUMBER,
  REST_OF_LINE;

  /**
   * Create ProductDefinitionGrammar.
   *
   * @return The grammar.
   */
  public static LexerlessGrammar create() {
    final LexerlessGrammarBuilder builder = LexerlessGrammarBuilder.create();

    SwProductDefinitionGrammar.keywords(builder);

    builder
        .rule(PRODUCT_DEFINITION)
        .is(
            builder.optional(SPACING),
            builder.optional(PRODUCT_IDENTIFICATION),
            builder.zeroOrMore(
                builder.firstOf(
                    DESCRIPTION,
                    DO_NOT_TRANSLATE,
                    REQUIRES,
                    OPTIONAL,
                    TITLE,
                    VERSION,
                    SPACING,
                    SYNTAX_ERROR_SECTION,
                    SYNTAX_ERROR_LINE)),
            builder.token(GenericTokenType.EOF, builder.endOfInput()));

    builder
        .rule(SYNTAX_ERROR_SECTION)
        .is(
            builder.regexp(
                SwProductDefinitionGrammar.syntaxErrorRegexp(SwProductDefinitionKeyword.END)),
            SwProductDefinitionKeyword.END);
    builder
        .rule(SYNTAX_ERROR_LINE)
        .is(
            builder.regexp("(?!" + SwProductDefinitionKeyword.END.getValue() + ").+"),
            builder.optional(builder.regexp("[\r\n]+")));

    builder.rule(PRODUCT_IDENTIFICATION).is(PRODUCT_NAME, WHITESPACE, PRODUCT_TYPE);
    builder.rule(PRODUCT_NAME).is(IDENTIFIER);
    builder
        .rule(DESCRIPTION)
        .is(
            SwProductDefinitionKeyword.DESCRIPTION,
            SPACING,
            FREE_LINES,
            SwProductDefinitionKeyword.END);
    builder.rule(DO_NOT_TRANSLATE).is(SwProductDefinitionKeyword.DO_NOT_TRANSLATE);
    builder
        .rule(REQUIRES)
        .is(
            SwProductDefinitionKeyword.REQUIRES,
            SPACING,
            PRODUCT_REFS,
            SwProductDefinitionKeyword.END);
    builder
        .rule(OPTIONAL)
        .is(
            SwProductDefinitionKeyword.OPTIONAL,
            SPACING,
            PRODUCT_REFS,
            SwProductDefinitionKeyword.END);
    builder
        .rule(TITLE)
        .is(SwProductDefinitionKeyword.TITLE, SPACING, FREE_LINES, SwProductDefinitionKeyword.END);
    builder
        .rule(VERSION)
        .is(
            SwProductDefinitionKeyword.VERSION,
            WHITESPACE,
            VERSION_NUMBER,
            builder.optional(WHITESPACE, REST_OF_LINE));

    builder
        .rule(PRODUCT_TYPE)
        .is(
            builder.firstOf(
                SwProductDefinitionKeyword.LAYERED_PRODUCT,
                SwProductDefinitionKeyword.CUSTOMISATION_PRODUCT,
                SwProductDefinitionKeyword.CONFIG_PRODUCT));
    builder.rule(PRODUCT_REFS).is(builder.zeroOrMore(PRODUCT_REF, SPACING));
    builder.rule(PRODUCT_REF).is(PRODUCT_NAME, builder.optional(WHITESPACE, VERSION));
    builder.rule(VERSION_NUMBER).is(NUMBER, builder.zeroOrMore(".", NUMBER));
    builder.rule(FREE_LINES).is(builder.zeroOrMore(FREE_LINE));
    builder
        .rule(FREE_LINE)
        .is(builder.regexp("(?!" + SwProductDefinitionKeyword.END.getValue() + ").*[\r\n]+"));

    builder
        .rule(SPACING)
        .is(builder.oneOrMore(builder.firstOf(WHITESPACE, NEWLINE, COMMENT)))
        .skip();

    builder.rule(NEWLINE).is(builder.skippedTrivia(builder.regexp("(?:\\n|\\r\\n|\\r)"))).skip();
    builder
        .rule(WHITESPACE)
        .is(builder.skippedTrivia(builder.regexp("[\\t\\u0020\\u00A0\\uFEFF]+")))
        .skip();
    builder.rule(COMMENT).is(builder.commentTrivia(builder.regexp("(?s)#[^\r\n]*"))).skip();
    builder
        .rule(IDENTIFIER)
        .is(builder.regexp("(?!" + SwProductDefinitionKeyword.END.getValue() + ")[a-zA-Z0-9_!]+"));
    builder.rule(NUMBER).is(builder.regexp("[0-9]+"));
    builder.rule(REST_OF_LINE).is(builder.regexp("[^\r\n]*+"));

    builder.setRootRule(PRODUCT_DEFINITION);

    return builder.build();
  }

  private static void keywords(final LexerlessGrammarBuilder builder) {
    for (final SwProductDefinitionKeyword keyword : SwProductDefinitionKeyword.values()) {
      builder.rule(keyword).is(builder.regexp("(?i)" + keyword.getValue() + "(?!\\w)")).skip();
    }
  }

  static String syntaxErrorRegexp(final SwProductDefinitionKeyword keyword) {
    // Eat up anything to keyword.
    return "(?s).+?(?=(?i)" + keyword.getValue() + ")";
  }
}
