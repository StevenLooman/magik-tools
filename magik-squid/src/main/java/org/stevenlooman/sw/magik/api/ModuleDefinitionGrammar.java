package org.stevenlooman.sw.magik.api;

import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.grammar.LexerlessGrammarBuilder;
import org.sonar.sslr.parser.LexerlessGrammar;

public enum ModuleDefinitionGrammar implements GrammarRuleKey {

  MODULE_DEFINITION,

  MODULE_IDENTIFICATION,

  CLAUSE,
  CONDITION_MESSAGE_ACCESSOR,
  DESCRIPTION,
  HIDDEN,
  LANGUAGE,
  MESSAGES,
  OPTIONAL,
  REQUIRES,
  REQUIRES_DATAMODEL,
  TEMPLATES,
  TEST,
  TESTS_MODULES,
  INSTALL_REQUIRES,

  ACE_INSTALLATION,
  AUTH_INSTALLATION,
  CASE_INSTALLATION,
  STYLE_INSTALLATION,
  SYSTEM_INSTALLATION,

  MODULE_REFS,
  MODULE_REF,
  REQUIRES_DATAMODEL_ENTRIES,
  REQUIRES_DATAMODEL_ENTRY,
  TEST_ENTRIES,
  TEST_ENTRY,
  FREE_LINES,
  FREE_LINE,

  SPACING,
  WHITESPACE_NEWLINE,
  WHITESPACE,
  COMMENT,
  IDENTIFIER,
  NUMBER,
  REST_OF_LINE,
  VERSION,

  IDENTIFIERS,
  IDENTIFIER_LIST,;

  /**
   * Create ModuleDefinitionGrammar.
   * @return The grammar.
   */
  public static LexerlessGrammar create() {
    LexerlessGrammarBuilder builder = LexerlessGrammarBuilder.create();

    //CHECKSTYLE.OFF: LineLength
    builder.rule(MODULE_DEFINITION).is(builder.zeroOrMore(CLAUSE));

    builder.rule(CLAUSE).is(builder.firstOf(
        MODULE_IDENTIFICATION,
        CONDITION_MESSAGE_ACCESSOR,
        DESCRIPTION,
        HIDDEN,
        INSTALL_REQUIRES,
        LANGUAGE,
        MESSAGES,
        OPTIONAL,
        REQUIRES,
        REQUIRES_DATAMODEL,
        TEMPLATES,
        TEST,
        TESTS_MODULES,

        ACE_INSTALLATION,
        AUTH_INSTALLATION,
        CASE_INSTALLATION,
        STYLE_INSTALLATION,
        SYSTEM_INSTALLATION,

        SPACING
    )).skip();

    builder.rule(MODULE_IDENTIFICATION).is(IDENTIFIER, WHITESPACE, NUMBER, builder.optional(WHITESPACE, NUMBER));
    builder.rule(CONDITION_MESSAGE_ACCESSOR).is("condition_message_accessor", WHITESPACE, IDENTIFIER);
    builder.rule(DESCRIPTION).is("description", SPACING, FREE_LINES, "end");
    builder.rule(HIDDEN).is("hidden");
    builder.rule(INSTALL_REQUIRES).is("install_requires", SPACING, MODULE_REFS, "end");
    builder.rule(LANGUAGE).is("language", WHITESPACE, IDENTIFIER);
    builder.rule(MESSAGES).is("messages", WHITESPACE, IDENTIFIERS);
    builder.rule(OPTIONAL).is("optional", SPACING, MODULE_REFS, "end");
    builder.rule(REQUIRES).is("requires", SPACING, MODULE_REFS, "end");
    builder.rule(REQUIRES_DATAMODEL).is("requires_datamodel", SPACING, REQUIRES_DATAMODEL_ENTRIES, "end");
    builder.rule(TEMPLATES).is("templates", WHITESPACE, IDENTIFIERS);
    builder.rule(TEST).is("test", SPACING, TEST_ENTRIES, "end");
    builder.rule(TESTS_MODULES).is("tests_modules", SPACING, MODULE_REFS, "end");

    builder.rule(ACE_INSTALLATION).is("ace_installation", SPACING, FREE_LINES, "end");
    builder.rule(AUTH_INSTALLATION).is("auth_installation", SPACING, FREE_LINES, "end");
    builder.rule(CASE_INSTALLATION).is("case_installation", SPACING, FREE_LINES, "end");
    builder.rule(STYLE_INSTALLATION).is("style_installation", SPACING, FREE_LINES, "end");
    builder.rule(SYSTEM_INSTALLATION).is("system_installation", SPACING, FREE_LINES, "end");

    builder.rule(REQUIRES_DATAMODEL_ENTRIES).is(builder.zeroOrMore(REQUIRES_DATAMODEL_ENTRY, SPACING));
    builder.rule(REQUIRES_DATAMODEL_ENTRY).is(
        IDENTIFIER,
        builder.optional(WHITESPACE, IDENTIFIER,
            builder.optional(WHITESPACE, IDENTIFIER,
                builder.optional(WHITESPACE, NUMBER,
                    builder.optional(NUMBER)))));
    builder.rule(MODULE_REFS).is(builder.zeroOrMore(MODULE_REF, SPACING));
    builder.rule(MODULE_REF).is(IDENTIFIER, builder.optional(WHITESPACE, VERSION));
    builder.rule(TEST_ENTRIES).is(builder.zeroOrMore(TEST_ENTRY, SPACING));
    builder.rule(TEST_ENTRY).is(builder.firstOf(
        builder.sequence("name", WHITESPACE, IDENTIFIER),
        builder.sequence("framework", WHITESPACE, IDENTIFIER),
        builder.sequence("topics", WHITESPACE, IDENTIFIER_LIST),
        builder.sequence("args", WHITESPACE, REST_OF_LINE),
        builder.sequence("description", WHITESPACE, REST_OF_LINE),
        builder.sequence("label", WHITESPACE, IDENTIFIER),
        builder.sequence("topic", WHITESPACE, IDENTIFIER),
        builder.sequence("arg", WHITESPACE, IDENTIFIER)
    ));
    builder.rule(FREE_LINES).is(builder.zeroOrMore(FREE_LINE));
    builder.rule(FREE_LINE).is(builder.regexp("(?!end).*[\r\n]+"));

    builder.rule(IDENTIFIERS).is(builder.zeroOrMore(IDENTIFIER, builder.optional(WHITESPACE)));
    builder.rule(IDENTIFIER_LIST).is(builder.optional(IDENTIFIER, builder.zeroOrMore(builder.optional(WHITESPACE), ",", IDENTIFIER)));

    builder.rule(SPACING).is(builder.oneOrMore(builder.firstOf(WHITESPACE_NEWLINE, COMMENT))).skip();

    builder.rule(WHITESPACE_NEWLINE).is(builder.skippedTrivia(builder.regexp("[ \n\r\t\f]+"))).skip();
    builder.rule(WHITESPACE).is(builder.skippedTrivia(builder.regexp("[ \t]+"))).skip();
    builder.rule(COMMENT).is(builder.commentTrivia(builder.regexp("#[^\r\n]*"))).skip();
    builder.rule(IDENTIFIER).is(builder.regexp("(?!end)\\w+"));
    builder.rule(NUMBER).is(builder.regexp("[0-9]+"));
    builder.rule(VERSION).is(builder.regexp("[0-9]+"));
    builder.rule(REST_OF_LINE).is(builder.regexp("[^\r\n]*"));

    builder.setRootRule(MODULE_DEFINITION);
    //CHECKSTYLE.ON: LineLength

    return builder.build();
  }


}
