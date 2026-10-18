package nl.ramsolutions.sw.translation.api;

import com.sonar.sslr.api.GenericTokenType;
import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.grammar.LexerlessGrammarBuilder;
import org.sonar.sslr.parser.LexerlessGrammar;

/** Translation (.trn) file grammar. */
@SuppressWarnings("checkstyle:JavadocVariable")
public enum TranslationGrammar implements GrammarRuleKey {
  TRANSLATIONS,

  TRANSLATION,
  TYPE,
  NAME,
  CONTEXT,
  LANGUAGE,
  SOURCE_TEXT,
  TRANSLATED_TEXT,
  IGNORED_FIELD,
  EMPTY_FIELD,
  FIELD,

  SEPARATOR,
  NEWLINE,
  WHITESPACE,
  COMMENT,

  SYNTAX_ERROR;

  private static final String END_OF_LINE_REGEXP = "(?:\\r|\\n|\\z)";

  private static final String FIELD_REGEXP =
      "[^\\t\\r\\n\\u0020](?:[^\\t\\r\\n]*[^\\t\\r\\n\\u0020])?";

  /**
   * Create TranslationGrammar.
   *
   * @return The grammar.
   */
  public static LexerlessGrammar create() {
    final LexerlessGrammarBuilder builder = LexerlessGrammarBuilder.create();

    final Object blankLine =
        builder.sequence(
            builder.skippedTrivia(builder.regexp("\\t+")),
            builder.next(builder.regexp(END_OF_LINE_REGEXP)));
    builder
        .rule(TRANSLATIONS)
        .is(
            builder.optional(builder.skippedTrivia(builder.regexp("\\uFEFF"))),
            builder.zeroOrMore(
                builder.firstOf(COMMENT, TRANSLATION, NEWLINE, blankLine, SYNTAX_ERROR)),
            builder.token(GenericTokenType.EOF, builder.endOfInput()));

    builder.rule(SYNTAX_ERROR).is(builder.regexp("[^\\r\\n]+"));

    builder
        .rule(TRANSLATION)
        .is(
            builder.optional(builder.skippedTrivia(builder.regexp("\\t+"))),
            builder.optional(WHITESPACE),
            TYPE,
            SEPARATOR,
            builder.optional(WHITESPACE),
            NAME,
            SEPARATOR,
            builder.optional(WHITESPACE),
            CONTEXT,
            builder.zeroOrMore(
                SEPARATOR,
                builder.firstOf(
                    EMPTY_FIELD, builder.sequence(builder.optional(WHITESPACE), FIELD))),
            builder.optional(builder.skippedTrivia(builder.regexp("\\u0020*\\t*"))),
            builder.next(builder.regexp(END_OF_LINE_REGEXP)));

    builder.rule(TYPE).is(builder.regexp(FIELD_REGEXP));
    builder.rule(NAME).is(builder.regexp(FIELD_REGEXP));
    builder.rule(CONTEXT).is(builder.regexp(FIELD_REGEXP));
    builder.rule(FIELD).is(builder.regexp(FIELD_REGEXP));
    builder.rule(EMPTY_FIELD).is(builder.regexp("\\u0020+(?=\\t|" + END_OF_LINE_REGEXP + ")"));
    builder.rule(LANGUAGE).is(builder.regexp(FIELD_REGEXP));
    builder.rule(SOURCE_TEXT).is(builder.regexp(FIELD_REGEXP));
    builder.rule(TRANSLATED_TEXT).is(builder.regexp(FIELD_REGEXP));
    builder.rule(IGNORED_FIELD).is(builder.regexp(FIELD_REGEXP));

    builder.rule(SEPARATOR).is(builder.skippedTrivia(builder.regexp("\\u0020*\\t+"))).skip();

    builder.rule(NEWLINE).is(builder.skippedTrivia(builder.regexp("(?:\\n|\\r\\n|\\r)"))).skip();
    builder.rule(WHITESPACE).is(builder.skippedTrivia(builder.regexp("\\u0020+"))).skip();
    builder.rule(COMMENT).is(builder.commentTrivia(builder.regexp("#[^\\r\\n]*"))).skip();

    builder.setRootRule(TRANSLATIONS);

    return builder.build();
  }
}
