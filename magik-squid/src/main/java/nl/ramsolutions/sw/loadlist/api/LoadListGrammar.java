package nl.ramsolutions.sw.loadlist.api;

import com.sonar.sslr.api.GenericTokenType;
import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.grammar.LexerlessGrammarBuilder;
import org.sonar.sslr.parser.LexerlessGrammar;

/** Load list grammar. */
@SuppressWarnings("checkstyle:JavadocVariable")
public enum LoadListGrammar implements GrammarRuleKey {
  LOAD_LIST,

  FILE_ENTRY,
  FILE_PATH,

  SPACING,
  NEWLINE,
  WHITESPACE,
  COMMENT,

  SYNTAX_ERROR;

  /**
   * Create LoadListGrammar.
   *
   * @return The grammar.
   */
  public static LexerlessGrammar create() {
    final LexerlessGrammarBuilder builder = LexerlessGrammarBuilder.create();

    builder
        .rule(LOAD_LIST)
        .is(
            builder.zeroOrMore(builder.firstOf(FILE_ENTRY, NEWLINE, SPACING, SYNTAX_ERROR)),
            builder.token(GenericTokenType.EOF, builder.endOfInput()));

    builder
        .rule(SYNTAX_ERROR)
        .is(builder.regexp(".+"), builder.optional(builder.regexp("[\r\n]+")));

    builder.rule(FILE_ENTRY).is(FILE_PATH, builder.optional(COMMENT));

    builder.rule(FILE_PATH).is(builder.regexp("(?!#)[^#\\r\\n]+"));

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

    builder.setRootRule(LOAD_LIST);

    return builder.build();
  }
}
