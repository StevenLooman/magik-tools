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
  WHITESPACE,
  EOS,
  ;

  private static final String SYMBOL_REGEXP = "(?is):(([a-z0-9_\\?\\!])|(\\|[^\\|]*\\|))+";
  private static final String WHITESPACE_REGEXP = "[ \t]+";
  private static final String WHITESPACE_NEWLINE_REGEXP = "[ \n\r\t\f]+";
  private static final String REST_OF_LINE_REGEXP = "(?!:)[^\r\n]*";

  public static final String END_OF_MESSAGE_PATCH = "$";

  // CHECKSTYLE.OFF: LocalVariableName

  /**
   * Create MessagePatchGrammar.
   * @param endOfPatchString End of patch string, usually "$".
   * @return The grammar.
   */
  public static LexerlessGrammar create(String endOfPatchString) {
    String terminator = endOfPatchString;
    if (endOfPatchString == null
        || endOfPatchString.trim().equals("")) {
      terminator = END_OF_MESSAGE_PATCH;
    }
    LexerlessGrammarBuilder b = LexerlessGrammarBuilder.create();

    //CHECKSTYLE.OFF: LineLength
    b.rule(READ_MESSAGE_PATCH).is(
        b.zeroOrMore(b.firstOf(WHITESPACE_NEWLINE, MESSAGE_PATCH)),
        b.optional(END_MESSAGE_PATCH));
    b.rule(END_MESSAGE_PATCH).is(terminator);

    b.rule(MESSAGE_PATCH).is(
        MESSAGE_IDENTIFIER,
        b.optional(WHITESPACE, LANGUAGE_IDENTIFIER, WHITESPACE),
        b.firstOf(REMOVE, MESSAGE));
    b.rule(MESSAGE_IDENTIFIER).is(SYMBOL);
    b.rule(LANGUAGE_IDENTIFIER).is(SYMBOL);

    b.rule(REMOVE).is(":REMOVE", SPACING);
    b.rule(SYMBOL).is(b.regexp(SYMBOL_REGEXP)).skip();
    b.rule(MESSAGE).is(b.oneOrMore(REST_OF_LINE, SPACING));

    b.rule(SPACING).is(b.oneOrMore(WHITESPACE_NEWLINE)).skip();
    b.rule(WHITESPACE_NEWLINE).is(b.skippedTrivia(b.regexp(WHITESPACE_NEWLINE_REGEXP))).skip();
    b.rule(WHITESPACE).is(b.skippedTrivia(b.regexp(WHITESPACE_REGEXP))).skip();
    b.rule(REST_OF_LINE).is(b.regexp(REST_OF_LINE_REGEXP));
    b.rule(EOS).is(
        b.firstOf(SPACING, WHITESPACE_NEWLINE_REGEXP)).skip();

    b.setRootRule(READ_MESSAGE_PATCH);
    //CHECKSTYLE.ON: LineLength

    return b.build();
  }

  // CHECKSTYLE.ON: LocalVariableName

}
