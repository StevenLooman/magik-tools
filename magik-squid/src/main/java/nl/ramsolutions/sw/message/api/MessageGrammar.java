package nl.ramsolutions.sw.message.api;

import com.sonar.sslr.api.GenericTokenType;
import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.grammar.LexerlessGrammarBuilder;
import org.sonar.sslr.parser.LexerlessGrammar;

/** Message (.msg) file grammar. */
@SuppressWarnings("checkstyle:JavadocVariable")
public enum MessageGrammar implements GrammarRuleKey {
  MESSAGES,

  USES,
  USE_NAME,

  MESSAGE,
  MESSAGE_ID,
  LANGUAGE,
  REMOVE,
  IGNORED_TEXT,
  MESSAGE_TEXT,
  TEXT_LINE,
  TEXT,
  PARAMETER,
  ESCAPE,
  INVALID_ESCAPE,

  HEADER_SPACING,
  SPACING,
  NEWLINE,
  WHITESPACE,
  COMMENT,

  HEADER_SYNTAX_ERROR,
  SYNTAX_ERROR;

  private static final String SYMBOL_PART_REGEXP = "\\|[^|\\r\\n]*\\||\\\\[^\\r\\n]";
  private static final String SYMBOL_REGEXP =
      ":(?:[\\p{L}_?!]|"
          + SYMBOL_PART_REGEXP
          + ")(?:[\\p{L}\\p{N}_?!]|"
          + SYMBOL_PART_REGEXP
          + ")*+(?!\\|)";

  private static final String PARAMETER_REGEXP = "#(?:[1-9]|[@h][0-9]+)";

  private static final String ESCAPE_REGEXP = "#[#nts]";

  private static final String SPECIAL_REGEXP = "#[^\\r\\n]";

  /**
   * Create MessageGrammar.
   *
   * @return The grammar.
   */
  public static LexerlessGrammar create() {
    final LexerlessGrammarBuilder builder = LexerlessGrammarBuilder.create();

    builder
        .rule(MESSAGES)
        .is(
            builder.optional(HEADER_SPACING),
            builder.optional(USES),
            builder.zeroOrMore(builder.firstOf(HEADER_SPACING, HEADER_SYNTAX_ERROR)),
            builder.zeroOrMore(builder.firstOf(MESSAGE, SPACING, SYNTAX_ERROR)),
            builder.token(GenericTokenType.EOF, builder.endOfInput()));

    final Object continuationLine =
        builder.sequence(
            builder.oneOrMore(builder.optional(WHITESPACE), NEWLINE),
            builder.nextNot(":"),
            builder.optional(WHITESPACE),
            TEXT_LINE);

    builder
        .rule(SYNTAX_ERROR)
        .is(builder.regexp("[^\\r\\n]+"), builder.zeroOrMore(continuationLine));
    builder.rule(HEADER_SYNTAX_ERROR).is(builder.nextNot(":"), builder.regexp("[^\\r\\n]+"));

    builder
        .rule(USES)
        .is(
            "+",
            builder.oneOrMore(builder.optional(WHITESPACE), USE_NAME),
            builder.next(builder.regexp("[\\t\\u0020]*(?:#|\\r|\\n|\\z)")));

    builder.rule(USE_NAME).is(builder.regexp("[\\p{L}\\p{N}_?!]+"));

    builder
        .rule(MESSAGE)
        .is(
            MESSAGE_ID,
            builder.firstOf(
                builder.sequence(
                    builder.optional(WHITESPACE),
                    REMOVE,
                    builder.optional(builder.optional(WHITESPACE), IGNORED_TEXT)),
                builder.sequence(
                    builder.optional(builder.optional(WHITESPACE), LANGUAGE),
                    builder.optional(MESSAGE_TEXT))));

    builder.rule(MESSAGE_ID).is(builder.regexp(SYMBOL_REGEXP));

    builder.rule(LANGUAGE).is(builder.regexp(SYMBOL_REGEXP));

    builder.rule(REMOVE).is(builder.regexp(":\\|REMOVE\\|(?![\\p{L}\\p{N}_?!|\\\\])"));
    builder.rule(IGNORED_TEXT).is(builder.regexp("\\S(?:[^\\r\\n]*\\S)?"));

    builder
        .rule(MESSAGE_TEXT)
        .is(
            builder.firstOf(
                builder.sequence(
                    builder.optional(WHITESPACE), TEXT_LINE, builder.zeroOrMore(continuationLine)),
                builder.oneOrMore(continuationLine)));

    builder
        .rule(TEXT_LINE)
        .is(
            builder.oneOrMore(
                builder.firstOf(ESCAPE, PARAMETER, INVALID_ESCAPE, TEXT),
                builder.optional(WHITESPACE)));

    builder.rule(ESCAPE).is(builder.regexp(ESCAPE_REGEXP));

    builder.rule(PARAMETER).is(builder.regexp(PARAMETER_REGEXP));

    builder.rule(INVALID_ESCAPE).is(builder.regexp(SPECIAL_REGEXP));

    builder
        .rule(TEXT)
        .is(
            builder.regexp(
                "(?=\\S)(?:(?!"
                    + SPECIAL_REGEXP
                    + ")[^\\r\\n])+?(?=[\\t\\u0020]*(?:[\\r\\n]|\\z|"
                    + SPECIAL_REGEXP
                    + "))"));

    builder
        .rule(HEADER_SPACING)
        .is(builder.oneOrMore(builder.firstOf(WHITESPACE, NEWLINE, COMMENT)))
        .skip();
    builder.rule(SPACING).is(builder.oneOrMore(builder.firstOf(WHITESPACE, NEWLINE))).skip();

    builder.rule(NEWLINE).is(builder.skippedTrivia(builder.regexp("(?:\\n|\\r\\n|\\r)"))).skip();
    builder
        .rule(WHITESPACE)
        .is(builder.skippedTrivia(builder.regexp("[\\t\\u0020\\u00A0\\uFEFF]+")))
        .skip();
    builder.rule(COMMENT).is(builder.commentTrivia(builder.regexp("#[^\\r\\n]*"))).skip();

    builder.setRootRule(MESSAGES);

    return builder.build();
  }
}
