package nl.ramsolutions.sw.moduledef.api;

import com.sonar.sslr.api.GenericTokenType;
import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.grammar.LexerlessGrammarBuilder;
import org.sonar.sslr.parser.LexerlessGrammar;

/** Module definition grammar. */
@SuppressWarnings("checkstyle:JavadocVariable")
public enum ModuleDefinitionGrammar implements GrammarRuleKey {
  MODULE_DEFINITION,

  MODULE_IDENTIFICATION,
  MODULE_NAME,
  VERSION,

  CONDITION_MESSAGE_ACCESSOR,
  DESCRIPTION,
  DO_NOT_TRANSLATE,
  HIDDEN,
  LANGUAGE,
  MESSAGES,
  OPTIONAL,
  REQUIRED_BY,
  REQUIRES,
  REQUIRES_DATAMODEL,
  REQUIRES_JAVA,
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
  JAVA_MODULE_REFS,
  JAVA_MODULE_REF,
  REQUIRES_DATAMODEL_ENTRIES,
  REQUIRES_DATAMODEL_ENTRY,
  TEST_ENTRIES,
  TEST_ENTRY,
  FREE_LINES,
  FREE_LINE,

  SPACING,
  NEWLINE,
  WHITESPACE,
  COMMENT,
  IDENTIFIER,
  NUMBER,
  REST_OF_LINE,
  VERSION_NUMBER,

  SYNTAX_ERROR_SECTION,
  SYNTAX_ERROR_LINE,
  SYNTAX_ERROR_END,
  SYNTAX_ERROR,

  IDENTIFIERS,
  IDENTIFIER_LIST;

  /**
   * Create ModuleDefinitionGrammar.
   *
   * @return The grammar.
   */
  public static LexerlessGrammar create() {
    final LexerlessGrammarBuilder builder = LexerlessGrammarBuilder.create();

    ModuleDefinitionGrammar.keywords(builder);

    builder
        .rule(MODULE_DEFINITION)
        .is(
            builder.optional(SPACING),
            builder.optional(MODULE_IDENTIFICATION),
            builder.zeroOrMore(
                builder.firstOf(
                    CONDITION_MESSAGE_ACCESSOR,
                    DESCRIPTION,
                    DO_NOT_TRANSLATE,
                    HIDDEN,
                    INSTALL_REQUIRES,
                    LANGUAGE,
                    MESSAGES,
                    OPTIONAL,
                    REQUIRED_BY,
                    REQUIRES,
                    REQUIRES_DATAMODEL,
                    REQUIRES_JAVA,
                    TEMPLATES,
                    TEST,
                    TESTS_MODULES,
                    ACE_INSTALLATION,
                    AUTH_INSTALLATION,
                    CASE_INSTALLATION,
                    STYLE_INSTALLATION,
                    SYSTEM_INSTALLATION,
                    SPACING,
                    SYNTAX_ERROR_SECTION,
                    SYNTAX_ERROR_LINE,
                    SYNTAX_ERROR_END)),
            builder.token(GenericTokenType.EOF, builder.endOfInput()));

    builder
        .rule(SYNTAX_ERROR_SECTION)
        .is(
            builder.regexp(ModuleDefinitionGrammar.syntaxErrorRegexp(ModuleDefinitionKeyword.END)),
            ModuleDefinitionKeyword.END);
    builder
        .rule(SYNTAX_ERROR_LINE)
        .is(
            builder.regexp("(?!" + ModuleDefinitionKeyword.END.getValue() + ").+"),
            builder.optional(builder.regexp("[\r\n]+")));
    builder.rule(SYNTAX_ERROR_END).is(ModuleDefinitionKeyword.END);

    builder.rule(MODULE_IDENTIFICATION).is(MODULE_NAME, WHITESPACE, VERSION);
    builder.rule(MODULE_NAME).is(IDENTIFIER);
    builder.rule(VERSION).is(VERSION_NUMBER, builder.optional(WHITESPACE, VERSION_NUMBER));
    builder
        .rule(CONDITION_MESSAGE_ACCESSOR)
        .is(ModuleDefinitionKeyword.CONDITION_MESSAGE_ACCESSOR, WHITESPACE, IDENTIFIER);
    builder
        .rule(DESCRIPTION)
        .is(ModuleDefinitionKeyword.DESCRIPTION, SPACING, FREE_LINES, ModuleDefinitionKeyword.END);
    builder.rule(DO_NOT_TRANSLATE).is(ModuleDefinitionKeyword.DO_NOT_TRANSLATE);
    builder.rule(HIDDEN).is(ModuleDefinitionKeyword.HIDDEN);
    builder
        .rule(INSTALL_REQUIRES)
        .is(
            ModuleDefinitionKeyword.INSTALL_REQUIRES,
            SPACING,
            MODULE_REFS,
            ModuleDefinitionKeyword.END);
    builder.rule(LANGUAGE).is(ModuleDefinitionKeyword.LANGUAGE, WHITESPACE, IDENTIFIER);
    builder.rule(MESSAGES).is(ModuleDefinitionKeyword.MESSAGES, WHITESPACE, IDENTIFIERS);
    builder
        .rule(OPTIONAL)
        .is(ModuleDefinitionKeyword.OPTIONAL, SPACING, MODULE_REFS, ModuleDefinitionKeyword.END);
    builder
        .rule(REQUIRED_BY)
        .is(ModuleDefinitionKeyword.REQUIRED_BY, SPACING, MODULE_REFS, ModuleDefinitionKeyword.END);
    builder
        .rule(REQUIRES)
        .is(ModuleDefinitionKeyword.REQUIRES, SPACING, MODULE_REFS, ModuleDefinitionKeyword.END);
    builder
        .rule(REQUIRES_JAVA)
        .is(
            ModuleDefinitionKeyword.REQUIRES_JAVA,
            SPACING,
            JAVA_MODULE_REFS,
            ModuleDefinitionKeyword.END);
    builder
        .rule(REQUIRES_DATAMODEL)
        .is(
            ModuleDefinitionKeyword.REQUIRES_DATAMODEL,
            SPACING,
            REQUIRES_DATAMODEL_ENTRIES,
            ModuleDefinitionKeyword.END);
    builder
        .rule(TEMPLATES)
        .is(ModuleDefinitionKeyword.TEMPLATES, SPACING, FREE_LINES, ModuleDefinitionKeyword.END);
    builder
        .rule(TEST)
        .is(ModuleDefinitionKeyword.TEST, SPACING, TEST_ENTRIES, ModuleDefinitionKeyword.END);
    builder
        .rule(TESTS_MODULES)
        .is(
            ModuleDefinitionKeyword.TESTS_MODULES,
            SPACING,
            MODULE_REFS,
            ModuleDefinitionKeyword.END);

    builder
        .rule(ACE_INSTALLATION)
        .is(
            ModuleDefinitionKeyword.ACE_INSTALLATION,
            SPACING,
            FREE_LINES,
            ModuleDefinitionKeyword.END);
    builder
        .rule(AUTH_INSTALLATION)
        .is(
            ModuleDefinitionKeyword.AUTH_INSTALLATION,
            SPACING,
            FREE_LINES,
            ModuleDefinitionKeyword.END);
    builder
        .rule(CASE_INSTALLATION)
        .is(
            ModuleDefinitionKeyword.CASE_INSTALLATION,
            SPACING,
            FREE_LINES,
            ModuleDefinitionKeyword.END);
    builder
        .rule(STYLE_INSTALLATION)
        .is(
            ModuleDefinitionKeyword.STYLE_INSTALLATION,
            SPACING,
            FREE_LINES,
            ModuleDefinitionKeyword.END);
    builder
        .rule(SYSTEM_INSTALLATION)
        .is(
            ModuleDefinitionKeyword.SYSTEM_INSTALLATION,
            SPACING,
            FREE_LINES,
            ModuleDefinitionKeyword.END);

    builder
        .rule(REQUIRES_DATAMODEL_ENTRIES)
        .is(builder.zeroOrMore(REQUIRES_DATAMODEL_ENTRY, SPACING));
    builder
        .rule(REQUIRES_DATAMODEL_ENTRY)
        .is(
            IDENTIFIER,
            builder.optional(
                WHITESPACE,
                IDENTIFIER,
                builder.optional(
                    WHITESPACE,
                    IDENTIFIER,
                    builder.optional(WHITESPACE, NUMBER, builder.optional(NUMBER)))));
    builder.rule(MODULE_REFS).is(builder.zeroOrMore(MODULE_REF, SPACING));
    builder.rule(MODULE_REF).is(MODULE_NAME, builder.optional(WHITESPACE, VERSION));
    builder.rule(JAVA_MODULE_REFS).is(builder.zeroOrMore(JAVA_MODULE_REF, SPACING));
    builder
        .rule(JAVA_MODULE_REF)
        .is(
            MODULE_NAME,
            builder.zeroOrMore(".", IDENTIFIER),
            builder.optional(WHITESPACE, VERSION));
    builder.rule(TEST_ENTRIES).is(builder.zeroOrMore(TEST_ENTRY, SPACING));
    builder
        .rule(TEST_ENTRY)
        .is(
            builder.firstOf(
                builder.sequence(
                    ModuleDefinitionKeyword.NAME, builder.optional(WHITESPACE, IDENTIFIER)),
                builder.sequence(
                    ModuleDefinitionKeyword.FRAMEWORK, builder.optional(WHITESPACE, IDENTIFIER)),
                builder.sequence(
                    ModuleDefinitionKeyword.TOPICS, builder.optional(WHITESPACE, IDENTIFIER_LIST)),
                builder.sequence(
                    ModuleDefinitionKeyword.ARGS, builder.optional(WHITESPACE, REST_OF_LINE)),
                builder.sequence(
                    ModuleDefinitionKeyword.DESCRIPTION,
                    builder.optional(WHITESPACE, REST_OF_LINE)),
                builder.sequence(
                    ModuleDefinitionKeyword.LABEL, builder.optional(WHITESPACE, IDENTIFIER)),
                builder.sequence(
                    ModuleDefinitionKeyword.TOPIC, builder.optional(WHITESPACE, IDENTIFIER)),
                builder.sequence(
                    ModuleDefinitionKeyword.ARG, builder.optional(WHITESPACE, IDENTIFIER))));
    builder.rule(FREE_LINES).is(builder.zeroOrMore(FREE_LINE));
    builder
        .rule(FREE_LINE)
        .is(builder.regexp("(?!" + ModuleDefinitionKeyword.END.getValue() + ").*[\r\n]+"));

    builder.rule(IDENTIFIERS).is(builder.zeroOrMore(IDENTIFIER, builder.optional(WHITESPACE)));
    builder
        .rule(IDENTIFIER_LIST)
        .is(
            builder.optional(
                IDENTIFIER, builder.zeroOrMore(builder.optional(WHITESPACE), ",", IDENTIFIER)));

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
        .is(builder.regexp("(?!" + ModuleDefinitionKeyword.END.getValue() + ")[a-zA-Z0-9_!]+"));
    builder.rule(NUMBER).is(builder.regexp("[0-9]+"));
    builder.rule(VERSION_NUMBER).is(builder.regexp("[0-9]+"));
    builder.rule(REST_OF_LINE).is(builder.regexp("[^\r\n]*"));

    builder.setRootRule(MODULE_DEFINITION);

    return builder.build();
  }

  private static void keywords(final LexerlessGrammarBuilder builder) {
    for (final ModuleDefinitionKeyword keyword : ModuleDefinitionKeyword.values()) {
      builder.rule(keyword).is(builder.regexp("(?i)" + keyword.getValue() + "(?!\\w)")).skip();
    }
  }

  static String syntaxErrorRegexp(final ModuleDefinitionKeyword keyword) {
    // Eat up anything to keyword.
    return "(?s).+?(?=(?i)" + keyword.getValue() + ")";
  }
}
