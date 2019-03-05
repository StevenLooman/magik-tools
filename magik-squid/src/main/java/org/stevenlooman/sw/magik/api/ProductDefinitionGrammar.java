package org.stevenlooman.sw.magik.api;

import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.grammar.LexerlessGrammarBuilder;
import org.sonar.sslr.parser.LexerlessGrammar;

public enum ProductDefinitionGrammar implements GrammarRuleKey {

  PRODUCT_DEFINITION,

  PRODUCT_IDENTIFICATION,
  PRODUCT_TYPE,

  CLAUSE,
  DESCRIPTION,
  REQUIRES,
  TITLE,
  VERSION,

  MODULE_REFS,
  MODULE_REF,
  VERSION_NUMBER,
  FREE_LINES,
  FREE_LINE,

  SPACING,
  WHITESPACE_NEWLINE,
  WHITESPACE,
  OPTIONAL_WHITESPACE,
  COMMENT,
  IDENTIFIER,
  NUMBER,
  REST_OF_LINE,;

  /**
   * Create ProductDefinitionGrammar.
   * @return The grammar.
   */
  public static LexerlessGrammar create() {
    LexerlessGrammarBuilder builder = LexerlessGrammarBuilder.create();

    //CHECKSTYLE.OFF: LineLength
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
    builder.rule(REQUIRES).is("requires", SPACING, MODULE_REFS, "end");
    builder.rule(TITLE).is("title", SPACING, FREE_LINES, "end");
    builder.rule(VERSION).is("version", WHITESPACE, VERSION_NUMBER);

    builder.rule(PRODUCT_TYPE).is(builder.firstOf(
        "layered_product",
        "customisation_product",
        "config_product"));
    builder.rule(MODULE_REFS).is(builder.zeroOrMore(MODULE_REF, SPACING));
    builder.rule(MODULE_REF).is(IDENTIFIER, builder.optional(WHITESPACE, VERSION));
    builder.rule(VERSION_NUMBER).is(
        NUMBER, ".", NUMBER, builder.optional(".", NUMBER),
        builder.optional(WHITESPACE, REST_OF_LINE));
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
    //CHECKSTYLE.ON: LineLength

    return builder.build();
  }
}
