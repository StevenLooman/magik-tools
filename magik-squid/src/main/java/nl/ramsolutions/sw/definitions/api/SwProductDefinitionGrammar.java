package nl.ramsolutions.sw.definitions.api;

import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.grammar.LexerlessGrammarBuilder;
import org.sonar.sslr.parser.LexerlessGrammar;

/**
 * Product definition grammar.
 */
@SuppressWarnings("checkstyle:JavadocVariable")
public enum SwProductDefinitionGrammar implements GrammarRuleKey {

    PRODUCT_DEFINITION,

    PRODUCT_IDENTIFICATION,
    PRODUCT_TYPE,

    CLAUSE,
    DESCRIPTION,
    REQUIRES,
    TITLE,
    VERSION,

    PRODUCT_REFS,
    PRODUCT_REF,
    VERSION_NUMBER,
    VERSION_COMMENT,
    FREE_LINES,
    FREE_LINE,

    SPACING,
    WHITESPACE_NEWLINE,
    WHITESPACE,
    OPTIONAL_WHITESPACE,
    COMMENT,
    IDENTIFIER,
    NUMBER,
    REST_OF_LINE;

    /**
     * Create ProductDefinitionGrammar.
     * @return The grammar.
     */
    public static LexerlessGrammar create() {
        LexerlessGrammarBuilder builder = LexerlessGrammarBuilder.create();

        builder.rule(PRODUCT_DEFINITION).is(builder.zeroOrMore(CLAUSE));

        builder.rule(CLAUSE).is(builder.firstOf(
            PRODUCT_IDENTIFICATION,
            DESCRIPTION,
            REQUIRES,
            TITLE,
            VERSION,
            SPACING
        )).skip();

        builder.rule(PRODUCT_IDENTIFICATION).is(IDENTIFIER, WHITESPACE, PRODUCT_TYPE);
        builder.rule(DESCRIPTION).is("description", SPACING, FREE_LINES, "end");
        builder.rule(REQUIRES).is("requires", SPACING, PRODUCT_REFS, "end");
        builder.rule(TITLE).is("title", SPACING, FREE_LINES, "end");
        builder.rule(VERSION).is("version", WHITESPACE, VERSION_NUMBER, builder.optional(WHITESPACE, REST_OF_LINE));

        builder.rule(PRODUCT_TYPE).is(builder.firstOf(
            "layered_product",
            "customisation_product",
            "config_product"));
        builder.rule(PRODUCT_REFS).is(builder.zeroOrMore(PRODUCT_REF, SPACING));
        builder.rule(PRODUCT_REF).is(IDENTIFIER, builder.optional(WHITESPACE, VERSION_NUMBER));
        builder.rule(VERSION_NUMBER).is(
            NUMBER, builder.zeroOrMore(".", NUMBER));
        builder.rule(FREE_LINES).is(builder.zeroOrMore(FREE_LINE));
        builder.rule(FREE_LINE).is(builder.regexp("(?!end).*[\r\n]+"));

        builder.rule(SPACING).is(builder.oneOrMore(builder.firstOf(WHITESPACE_NEWLINE, COMMENT))).skip();

        builder.rule(WHITESPACE_NEWLINE).is(builder.skippedTrivia(builder.regexp("[ \n\r\t\f]+"))).skip();
        builder.rule(WHITESPACE).is(builder.skippedTrivia(builder.regexp("[ \t]+"))).skip();
        builder.rule(OPTIONAL_WHITESPACE).is(builder.skippedTrivia(builder.regexp("[ \n\r\t\f]*"))).skip();
        builder.rule(COMMENT).is(builder.commentTrivia(builder.regexp("#[^\r\n]*"))).skip();
        builder.rule(IDENTIFIER).is(builder.regexp("(?!end)\\w+"));
        builder.rule(NUMBER).is(builder.regexp("[0-9]+"));
        builder.rule(REST_OF_LINE).is(builder.regexp("[^\r\n]*+"));

        builder.setRootRule(PRODUCT_DEFINITION);

        return builder.build();
    }
}
