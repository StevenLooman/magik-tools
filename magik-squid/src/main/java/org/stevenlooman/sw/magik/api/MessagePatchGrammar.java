package org.stevenlooman.sw.magik.api;

import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.grammar.LexerlessGrammarBuilder;
import org.sonar.sslr.parser.LexerlessGrammar;

public enum MessagePatchGrammar implements GrammarRuleKey {

  READ_MESSAGE_PATCH,

  MESSAGE_PATCH,
  END_MESSAGE_PATCH,

  MESSAGE_IDENTIFIER,
  LANGUAGE_IDENTIFIER,
  REMOVE,

  SYMBOL,
  MESSAGE,

  REST_OF_LINE,

  SPACING,
  WHITESPACE_NEWLINE,
  WHITESPACE,;

  /**
   * Create MessagePatchGrammar.
   * @param endOfPatchString End of patch string, usually "$".
   * @return The grammar.
   */
  public static LexerlessGrammar create(String endOfPatchString) {
    LexerlessGrammarBuilder builder = LexerlessGrammarBuilder.create();

    //CHECKSTYLE.OFF: LineLength
    builder.rule(READ_MESSAGE_PATCH).is(builder.zeroOrMore(builder.firstOf(WHITESPACE_NEWLINE, MESSAGE_PATCH)), END_MESSAGE_PATCH);
    builder.rule(END_MESSAGE_PATCH).is(endOfPatchString);

    builder.rule(MESSAGE_PATCH).is(MESSAGE_IDENTIFIER, builder.optional(WHITESPACE, LANGUAGE_IDENTIFIER, WHITESPACE), builder.firstOf(REMOVE, MESSAGE));
    builder.rule(MESSAGE_IDENTIFIER).is(SYMBOL);
    builder.rule(LANGUAGE_IDENTIFIER).is(SYMBOL);

    builder.rule(REMOVE).is(":REMOVE", SPACING);
    builder.rule(SYMBOL).is(builder.regexp("(?is):(([a-z0-9_\\?\\!])|(\\|[^\\|]*\\|))+")).skip();
    builder.rule(MESSAGE).is(builder.oneOrMore(REST_OF_LINE, SPACING));

    builder.rule(SPACING).is(builder.oneOrMore(WHITESPACE_NEWLINE)).skip();
    builder.rule(WHITESPACE_NEWLINE).is(builder.skippedTrivia(builder.regexp("[ \n\r\t\f]+"))).skip();
    builder.rule(WHITESPACE).is(builder.skippedTrivia(builder.regexp("[ \t]+"))).skip();
    builder.rule(REST_OF_LINE).is(builder.regexp("(?!:)[^\r\n]*"));

    builder.setRootRule(READ_MESSAGE_PATCH);
    //CHECKSTYLE.ON: LineLength

    return builder.build();
  }
}
