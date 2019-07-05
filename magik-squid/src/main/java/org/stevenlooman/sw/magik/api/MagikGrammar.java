package org.stevenlooman.sw.magik.api;

import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.grammar.LexerlessGrammarBuilder;
import org.sonar.sslr.parser.LexerlessGrammar;

import java.util.List;

public enum MagikGrammar implements GrammarRuleKey {
  WHITESPACE,
  SPACING,
  SPACING_NO_LB,
  NEXT_NOT_LB,
  EOS,
  EOS_NO_LB,

  PARSER_ERROR,

  KEYWORDS,

  // root
  MAGIK,
  PRAGMA,
  PACKAGE_SPECIFICATION,
  METHOD_DEFINITION,
  TRANSMIT,

  // pragma
  PRAGMA_PARAMS,
  PRAGMA_PARAM,
  PRAGMA_VALUE,

  // constructs
  PARAMETERS,
  PARAMETER,
  ASSIGNMENT_PARAMETER,
  BODY,
  OPERATOR,
  EXPRESSIONS,
  IDENTIFIERS,
  IDENTIFIERS_WITH_GATHER,
  MULTI_VARIABLE_DECLARATION,
  VARIABLE_DECLARATION,
  METHOD_INVOCATION,
  PROCEDURE_INVOCATION,
  INDEXED_INVOCATION,

  // statements
  STATEMENT,
  STATEMENT_SEPARATOR,
  VARIABLE_DECLARATION_STATEMENT,
  RETURN_STATEMENT,
  EMIT_STATEMENT,
  EXPRESSION_STATEMENT,
  HANDLING,
  THROW_STATEMENT,
  BLOCK,
  PROTECT_BLOCK,
  TRY_BLOCK,
  CATCH_BLOCK,
  LOCK_BLOCK,
  IF, ELIF, ELSE,
  FOR, WHILE, OVER, LOOP,
  LEAVE_STATEMENT,
  CONTINUE_STATEMENT,
  LOOPBODY,

  // expressions
  EXPRESSION,
  ASSIGNMENT_EXPRESSION,
  AUGMENTED_ASSIGNMENT_EXPRESSION,
  OR_EXPRESSION,
  XOR_EXPRESSION,
  AND_EXPRESSION,
  EQUALITY_EXPRESSION,
  RELATIONAL_EXPRESSION,
  ADDITIVE_EXPRESSION,
  MULTIPLICATIVE_EXPRESSION,
  EXPONENTIAL_EXPRESSION,
  UNARY_EXPRESSION,
  POSTFIX_EXPRESSION,
  ATOM,
  ARGUMENTS,
  ARGUMENT,
  PROC_DEFINITION,

  // atoms
  STRING,
  NUMBER,
  CHARACTER,
  SYMBOL,
  IDENTIFIER,
  LABEL,
  SLOT,
  GLOBAL_REF,
  SIMPLE_VECTOR,
  GATHER,
  CLASS,
  ;

  // CHECKSTYLE.OFF: LineLength
  private static final String STRING_DOUBLE_REGEXP = "(\"[^\"]*\")";
  private static final String STRING_SINGLE_REGEXP = "('[^']*')";
  private static final String STRING_REGEXP = "(?s)(" + STRING_DOUBLE_REGEXP + "|" + STRING_SINGLE_REGEXP + ")";

  private static final String DIGITS_REGEXP = "[0-9]+";
  private static final String HEX_DIGITS_REGEXP = "[0-9a-f]+";
  private static final String RADIX_REGEXP = "(?is)r" + HEX_DIGITS_REGEXP;
  private static final String EXPONENT_REGEXP = "(?is)(e|&)[+-]?" + DIGITS_REGEXP;
  private static final String DECIMAL_REGEXP = "\\." + DIGITS_REGEXP;
  private static final String NUMBER_REGEXP = DIGITS_REGEXP + "(" + RADIX_REGEXP + "|" + EXPONENT_REGEXP + "|" + DECIMAL_REGEXP + ")*";

  private static final String CHARACTER_REGEXP = "%(\\W|\\w+)";
  private static final String COMMENT_REGEXP = "(?s)#[^\r\n]*";

  private static final String SIMPLE_IDENTIFIER_REGEXP = "([a-z!?]|\\\\.)([a-z0-9_!?]|\\\\.)*";
  private static final String PIPED_IDENTIFIER_REGEXP = "\\|[^\\|]*\\|";
  private static final String BARE_IDENTIFIER_REGEXP = "(" + SIMPLE_IDENTIFIER_REGEXP + "|" + PIPED_IDENTIFIER_REGEXP + ")";
  private static final String LABEL_REGEXP = "(?is)" + BARE_IDENTIFIER_REGEXP + "+";
  private static final String IDENTIFIER_REGEXP = "(?is)(" + BARE_IDENTIFIER_REGEXP + ":" + ")?(" + BARE_IDENTIFIER_REGEXP + ")+";

  private static final String SIMPLE_SYMBOL_REGEXP = "([a-z0-9_!?]|\\\\.)+";
  private static final String PIPED_SYMBOL_REGEXP = "(\\|[^\\|]*\\|)";
  private static final String SYMBOL_REGEXP = "(?is):(" + SIMPLE_SYMBOL_REGEXP + "|" + PIPED_SYMBOL_REGEXP + ")+";

  private static final String LINE_TERMINATOR_REGEXP = "\\n\\r";
  private static final String WHITESPACE_REGEXP = "\\t\\v\\f\\u0020\\u00A0\\uFEFF";
  private static final String NEWLINE_REGEXP = "(?:\\n|\\r\\n|\\r)";

  // CHECKSTYLE.OFF: LocalVariableName
  // CHECKSTYLE.OFF: ParameterName

  /**
   * Create a new LexerlessGrammar for the Magik language.
   * @return Grammar for the Magik language
   */
  public static LexerlessGrammar create() {
    LexerlessGrammarBuilder b = LexerlessGrammarBuilder.create();

    b.rule(WHITESPACE).is(b.regexp("[" + LINE_TERMINATOR_REGEXP + WHITESPACE_REGEXP + "]*+"));
    b.rule(SPACING).is(
        b.skippedTrivia(WHITESPACE),
        b.zeroOrMore(
            b.commentTrivia(b.regexp(COMMENT_REGEXP)),
            b.skippedTrivia(WHITESPACE))).skip();
    b.rule(SPACING_NO_LB).is(b.zeroOrMore(b.firstOf(
        b.skippedTrivia(b.regexp("[\\s&&[^\n\r]]++")),
        b.commentTrivia(b.regexp(COMMENT_REGEXP))))).skip();
    b.rule(NEXT_NOT_LB).is(b.nextNot(b.regexp("(?:" + "[\n\r]" + ")"))).skip();

    b.rule(EOS).is(b.firstOf(
        SPACING,
        b.sequence(SPACING_NO_LB, b.regexp(NEWLINE_REGEXP)),
        b.sequence(SPACING, b.endOfInput()))).skip();
    b.rule(EOS_NO_LB).is(b.firstOf(
        b.sequence(SPACING_NO_LB, b.regexp(NEWLINE_REGEXP)),
        b.sequence(SPACING_NO_LB, b.endOfInput()))).skip();

    b.rule(MAGIK).is(
//        b.zeroOrMore(
        b.oneOrMore(
            b.firstOf(
                PACKAGE_SPECIFICATION,
                PRAGMA,
                METHOD_DEFINITION,
                STATEMENT,
                TRANSMIT)));
    b.rule(TRANSMIT).is(MagikPunctuator.DOLLAR);

    punctuators(b);
    keywords(b);
    literals(b);
    expressions(b);
    statements(b);
    constructs(b);

    b.setRootRule(MAGIK);
    return b.build();
  }

  private static void punctuators(LexerlessGrammarBuilder b) {
    for (MagikPunctuator p : MagikPunctuator.values()) {
      b.rule(p).is(SPACING, p.getValue());
    }
  }

  private static void keywords(LexerlessGrammarBuilder b) {
    for (MagikKeyword k : MagikKeyword.values()) {
      b.rule(k).is(SPACING, b.regexp("(?i)" + k.getValue() + "(?!\\w)"));
    }

    List<MagikKeyword> keywords = MagikKeyword.keywords();
    Object[] rest = new Object[keywords.size() - 2];
    for (int i = 2; i < keywords.size(); i++) {
      rest[i - 2] = keywords.get(i);
    }
    b.rule(KEYWORDS).is(b.firstOf(keywords.get(0), keywords.get(1), rest));
  }

  private static void literals(LexerlessGrammarBuilder b) {
    b.rule(STRING).is(SPACING, b.regexp(STRING_REGEXP));
    b.rule(NUMBER).is(SPACING, b.regexp(NUMBER_REGEXP));
    b.rule(CHARACTER).is(SPACING, b.regexp(CHARACTER_REGEXP));
    b.rule(IDENTIFIER).is(SPACING, b.regexp(IDENTIFIER_REGEXP));
    b.rule(SYMBOL).is(SPACING, b.regexp(SYMBOL_REGEXP));
    b.rule(LABEL).is(SPACING, MagikPunctuator.AT, SPACING, b.regexp(LABEL_REGEXP));
    b.rule(GLOBAL_REF).is(SPACING, MagikPunctuator.AT, SPACING, b.regexp(IDENTIFIER_REGEXP));

    b.rule(PROC_DEFINITION).is(
        b.optional(MagikKeyword.ITER), MagikKeyword.PROC, b.optional(LABEL),
        MagikPunctuator.PAREN_L, b.optional(PARAMETERS), MagikPunctuator.PAREN_R,
        BODY,
        MagikKeyword.ENDPROC
    );

  }

  private static void expressions(LexerlessGrammarBuilder b) {
    b.rule(SLOT).is(MagikPunctuator.DOT, SPACING_NO_LB, NEXT_NOT_LB, IDENTIFIER);
    b.rule(SIMPLE_VECTOR).is(
        MagikPunctuator.BRACE_L,
        b.optional(EXPRESSION, b.zeroOrMore(MagikPunctuator.COMMA, EXPRESSION)),
        MagikPunctuator.BRACE_R);
    b.rule(CLASS).is(
        MagikKeyword.CLASS, IDENTIFIER);
    b.rule(GATHER).is(MagikKeyword.GATHER, EXPRESSIONS);

    b.rule(EXPRESSION).is(ASSIGNMENT_EXPRESSION);
    b.rule(ASSIGNMENT_EXPRESSION).is(AUGMENTED_ASSIGNMENT_EXPRESSION, b.zeroOrMore(SPACING_NO_LB, NEXT_NOT_LB, b.firstOf(MagikPunctuator.CHEVRON, MagikPunctuator.BOOT_CHEVRON), AUGMENTED_ASSIGNMENT_EXPRESSION)).skipIfOneChild();
    b.rule(AUGMENTED_ASSIGNMENT_EXPRESSION).is(OR_EXPRESSION, b.zeroOrMore(SPACING_NO_LB, NEXT_NOT_LB, OPERATOR, b.firstOf(MagikPunctuator.CHEVRON, MagikPunctuator.BOOT_CHEVRON), OR_EXPRESSION)).skipIfOneChild();
    b.rule(OR_EXPRESSION).is(XOR_EXPRESSION, b.zeroOrMore(SPACING_NO_LB, NEXT_NOT_LB, b.firstOf(MagikKeyword.ORIF, MagikKeyword.OR), XOR_EXPRESSION)).skipIfOneChild();
    b.rule(XOR_EXPRESSION).is(AND_EXPRESSION, b.zeroOrMore(SPACING_NO_LB, NEXT_NOT_LB, MagikKeyword.XOR, AND_EXPRESSION)).skipIfOneChild();
    b.rule(AND_EXPRESSION).is(EQUALITY_EXPRESSION, b.zeroOrMore(SPACING_NO_LB, NEXT_NOT_LB, b.firstOf(MagikKeyword.ANDIF, MagikKeyword.AND), EQUALITY_EXPRESSION)).skipIfOneChild();
    b.rule(EQUALITY_EXPRESSION).is(RELATIONAL_EXPRESSION, b.zeroOrMore(SPACING_NO_LB, NEXT_NOT_LB, b.firstOf(MagikKeyword.ISNT, MagikKeyword.IS, MagikKeyword.CF, MagikPunctuator.EQ, MagikPunctuator.NEQ, MagikPunctuator.NE), RELATIONAL_EXPRESSION)).skipIfOneChild();
    b.rule(RELATIONAL_EXPRESSION).is(ADDITIVE_EXPRESSION, b.zeroOrMore(SPACING_NO_LB, NEXT_NOT_LB, b.firstOf(MagikPunctuator.GE, MagikPunctuator.GT, MagikPunctuator.LE, MagikPunctuator.LT), ADDITIVE_EXPRESSION)).skipIfOneChild();
    b.rule(ADDITIVE_EXPRESSION).is(MULTIPLICATIVE_EXPRESSION, b.zeroOrMore(SPACING_NO_LB, NEXT_NOT_LB, b.firstOf(MagikPunctuator.PLUS, MagikPunctuator.MINUS), MULTIPLICATIVE_EXPRESSION)).skipIfOneChild();
    b.rule(MULTIPLICATIVE_EXPRESSION).is(EXPONENTIAL_EXPRESSION, b.zeroOrMore(SPACING_NO_LB, NEXT_NOT_LB, b.firstOf(MagikPunctuator.STAR, MagikPunctuator.DIV, MagikKeyword.DIV, MagikKeyword.MOD), EXPONENTIAL_EXPRESSION)).skipIfOneChild();
    b.rule(EXPONENTIAL_EXPRESSION).is(UNARY_EXPRESSION, b.zeroOrMore(SPACING_NO_LB, NEXT_NOT_LB, MagikPunctuator.EXP, UNARY_EXPRESSION)).skipIfOneChild();
    b.rule(UNARY_EXPRESSION).is(b.firstOf(
        b.sequence(b.firstOf(MagikPunctuator.NOT, MagikKeyword.NOT, MagikPunctuator.PLUS, MagikPunctuator.MINUS, MagikKeyword.ALLRESULTS, MagikKeyword.SCATTER), UNARY_EXPRESSION),
        POSTFIX_EXPRESSION)).skipIfOneChild();
    b.rule(POSTFIX_EXPRESSION).is(
        ATOM,
        b.zeroOrMore(
            SPACING_NO_LB, NEXT_NOT_LB,
            b.firstOf(
                METHOD_INVOCATION,
                INDEXED_INVOCATION,
                PROCEDURE_INVOCATION))).skipIfOneChild();

    b.rule(ATOM).is(
        b.firstOf(
            b.sequence(MagikPunctuator.PAREN_L, EXPRESSIONS, MagikPunctuator.PAREN_R),
            NUMBER,
            STRING,
            SYMBOL,
            CHARACTER,
            IDENTIFIER,
            GLOBAL_REF,
            SLOT,
            SIMPLE_VECTOR,
            CLASS,
            GATHER,
            LOOPBODY,
            PROC_DEFINITION,
            IF,
            FOR,
            OVER,
            LOOP,
            BLOCK,
            PROTECT_BLOCK,
            TRY_BLOCK,
            CATCH_BLOCK,
            LOCK_BLOCK,
            MagikKeyword.SELF,
            MagikKeyword.CLONE,
            MagikKeyword.SUPER,
            MagikKeyword.UNSET,
            MagikKeyword.TRUE,
            MagikKeyword.FALSE,
            MagikKeyword.MAYBE,
            MagikKeyword.PRIMITIVE,
            MagikKeyword.THISTHREAD
            ));

    b.rule(METHOD_INVOCATION).is(
        MagikPunctuator.DOT, IDENTIFIER,
        b.optional(SPACING_NO_LB, NEXT_NOT_LB, MagikPunctuator.PAREN_L, ARGUMENTS, MagikPunctuator.PAREN_R),
        b.optional(SPACING_NO_LB, NEXT_NOT_LB, b.firstOf(MagikPunctuator.CHEVRON, MagikPunctuator.BOOT_CHEVRON), EXPRESSION));
    b.rule(INDEXED_INVOCATION).is(
        MagikPunctuator.SQUARE_L, ARGUMENTS, MagikPunctuator.SQUARE_R,
        b.optional(SPACING_NO_LB, NEXT_NOT_LB, b.firstOf(MagikPunctuator.CHEVRON, MagikPunctuator.BOOT_CHEVRON), EXPRESSION));
    b.rule(PROCEDURE_INVOCATION).is(MagikPunctuator.PAREN_L, ARGUMENTS, MagikPunctuator.PAREN_R);
  }

  private static void statements(LexerlessGrammarBuilder b) {
    b.rule(STATEMENT).is(
        b.firstOf(
            EXPRESSION_STATEMENT,
            RETURN_STATEMENT,
            EMIT_STATEMENT,
            CONTINUE_STATEMENT,
            LEAVE_STATEMENT,
            THROW_STATEMENT,
            VARIABLE_DECLARATION_STATEMENT,
            STATEMENT_SEPARATOR));

    b.rule(BODY).is(
        b.zeroOrMore(HANDLING),
        b.zeroOrMore(STATEMENT));

    b.rule(VARIABLE_DECLARATION_STATEMENT).is(
        b.zeroOrMore(
            b.firstOf(
                MagikKeyword.LOCAL,
                MagikKeyword.CONSTANT,
                MagikKeyword.RECURSIVE,
                MagikKeyword.GLOBAL,
                MagikKeyword.DYNAMIC,
                MagikKeyword.IMPORT)),
        b.firstOf(
            MULTI_VARIABLE_DECLARATION,
            b.sequence(VARIABLE_DECLARATION, b.zeroOrMore(MagikPunctuator.COMMA, VARIABLE_DECLARATION))),
        EOS);
    b.rule(MULTI_VARIABLE_DECLARATION).is(
        b.sequence(MagikPunctuator.PAREN_L, IDENTIFIERS_WITH_GATHER, MagikPunctuator.PAREN_R,
            MagikPunctuator.CHEVRON,
            b.optional(MagikPunctuator.PAREN_L), EXPRESSIONS, b.optional(MagikPunctuator.PAREN_R)));
    b.rule(VARIABLE_DECLARATION).is(
        b.sequence(IDENTIFIER, b.optional(MagikPunctuator.CHEVRON, EXPRESSION)));

    b.rule(BLOCK).is(MagikKeyword.BLOCK, BODY, MagikKeyword.ENDBLOCK);

    b.rule(RETURN_STATEMENT).is(MagikKeyword.RETURN,
        b.optional(SPACING_NO_LB, NEXT_NOT_LB,
            b.optional(MagikPunctuator.PAREN_L), EXPRESSIONS, b.optional(MagikPunctuator.PAREN_R)),
        EOS);

    b.rule(EMIT_STATEMENT).is(
        MagikPunctuator.EMIT,
        b.firstOf(
            EXPRESSIONS,
            b.sequence(b.optional(MagikPunctuator.PAREN_L), EXPRESSIONS, b.optional(MagikPunctuator.PAREN_R))
        ),
        EOS);
    b.rule(STATEMENT_SEPARATOR).is(MagikPunctuator.SEMICOLON).skip();
    b.rule(EXPRESSION_STATEMENT).is(EXPRESSION, EOS);
    b.rule(HANDLING).is(
        MagikKeyword.HANDLING, b.firstOf(
            b.sequence(
                IDENTIFIERS,
                MagikKeyword.WITH,
                b.firstOf(EXPRESSION, MagikKeyword.DEFAULT)),
            MagikKeyword.DEFAULT));

    b.rule(THROW_STATEMENT).is(
        MagikKeyword.THROW, EXPRESSION,
        b.optional(SPACING_NO_LB, NEXT_NOT_LB, MagikKeyword.WITH, EXPRESSION));
    b.rule(PROTECT_BLOCK).is(MagikKeyword.PROTECT, BODY, MagikKeyword.PROTECTION, BODY, MagikKeyword.ENDPROTECT);
    b.rule(TRY_BLOCK).is(
        MagikKeyword.TRY, b.optional(MagikKeyword.WITH, IDENTIFIERS),
        BODY,
        b.oneOrMore(MagikKeyword.WHEN, IDENTIFIERS, BODY),
        MagikKeyword.ENDTRY);
    b.rule(CATCH_BLOCK).is(
        MagikKeyword.CATCH,
        b.optional(SPACING_NO_LB, NEXT_NOT_LB, EXPRESSION),
        BODY, MagikKeyword.ENDCATCH);
    b.rule(LOCK_BLOCK).is(MagikKeyword.LOCK, EXPRESSION, BODY, MagikKeyword.ENDLOCK);
    b.rule(IF).is(MagikKeyword.IF, EXPRESSION, MagikKeyword.THEN, BODY, b.zeroOrMore(ELIF), b.optional(ELSE), MagikKeyword.ENDIF);
    b.rule(ELIF).is(MagikKeyword.ELIF, EXPRESSION, MagikKeyword.THEN, BODY);
    b.rule(ELSE).is(MagikKeyword.ELSE, BODY);
    b.rule(FOR).is(MagikKeyword.FOR, IDENTIFIERS_WITH_GATHER, OVER);
    b.rule(WHILE).is(MagikKeyword.WHILE, EXPRESSION, LOOP);
    b.rule(OVER).is(MagikKeyword.OVER, EXPRESSION, LOOP);
    b.rule(LOOP).is(
        MagikKeyword.LOOP, b.optional(LABEL),
        BODY,
        b.optional(MagikKeyword.FINALLY, b.optional(MagikKeyword.WITH, IDENTIFIERS_WITH_GATHER), BODY),
        MagikKeyword.ENDLOOP);
    b.rule(LEAVE_STATEMENT).is(MagikKeyword.LEAVE, b.optional(LABEL), b.optional(SPACING_NO_LB, NEXT_NOT_LB, MagikKeyword.WITH, EXPRESSIONS));
    b.rule(CONTINUE_STATEMENT).is(MagikKeyword.CONTINUE, b.optional(LABEL), b.optional(SPACING_NO_LB, NEXT_NOT_LB, MagikKeyword.WITH, EXPRESSIONS));
    b.rule(LOOPBODY).is(MagikKeyword.LOOPBODY, MagikPunctuator.PAREN_L, EXPRESSIONS, MagikPunctuator.PAREN_R);
  }

  private static void constructs(LexerlessGrammarBuilder b) {
    b.rule(METHOD_DEFINITION).is(
        b.zeroOrMore(b.firstOf(MagikKeyword.ITER, MagikKeyword.PRIVATE, MagikKeyword.ABSTRACT)),
        MagikKeyword.METHOD, IDENTIFIER,
        b.firstOf(
            b.sequence(MagikPunctuator.DOT, IDENTIFIER,
                b.optional(SPACING_NO_LB, NEXT_NOT_LB, MagikPunctuator.PAREN_L, PARAMETERS, MagikPunctuator.PAREN_R),
                b.optional(SPACING_NO_LB, NEXT_NOT_LB, b.firstOf(MagikPunctuator.CHEVRON, MagikPunctuator.BOOT_CHEVRON), ASSIGNMENT_PARAMETER)),
            b.sequence(MagikPunctuator.SQUARE_L, PARAMETERS, MagikPunctuator.SQUARE_R,
                b.optional(SPACING_NO_LB, NEXT_NOT_LB, b.firstOf(MagikPunctuator.CHEVRON, MagikPunctuator.BOOT_CHEVRON), ASSIGNMENT_PARAMETER))),
        BODY,
        MagikKeyword.ENDMETHOD, EOS);

    b.rule(PARAMETERS).is(
        b.optional(PARAMETER, b.zeroOrMore(b.optional(MagikPunctuator.COMMA), PARAMETER)));
    b.rule(PARAMETER).is(
        b.optional(b.firstOf(MagikKeyword.GATHER, MagikKeyword.OPTIONAL)), IDENTIFIER);
    b.rule(ASSIGNMENT_PARAMETER).is(
        PARAMETER
    );

    b.rule(ARGUMENTS).is(b.optional(ARGUMENT, b.zeroOrMore(b.optional(MagikPunctuator.COMMA), ARGUMENT)));
    b.rule(ARGUMENT).is(EXPRESSION);

    b.rule(EXPRESSIONS).is(
        EXPRESSION,
        b.zeroOrMore(SPACING_NO_LB, NEXT_NOT_LB, MagikPunctuator.COMMA, EXPRESSION));

    b.rule(IDENTIFIERS).is(
        IDENTIFIER, b.zeroOrMore(SPACING_NO_LB, NEXT_NOT_LB, MagikPunctuator.COMMA, IDENTIFIER));
    b.rule(IDENTIFIERS_WITH_GATHER).is(
        b.optional(MagikKeyword.GATHER), IDENTIFIER, b.zeroOrMore(MagikPunctuator.COMMA, b.optional(MagikKeyword.GATHER), IDENTIFIER));

    b.rule(OPERATOR).is(
        b.firstOf(
            MagikKeyword.ANDIF, MagikKeyword.AND,
            MagikKeyword.ORIF, MagikKeyword.OR,
            MagikKeyword.XOR,
            MagikKeyword.DIV, MagikKeyword.MOD,
            MagikPunctuator.PLUS, MagikPunctuator.MINUS,
            MagikPunctuator.STAR, MagikPunctuator.DIV,
            MagikPunctuator.EXP
            )).skip();

    b.rule(PRAGMA).is(MagikKeyword.PRAGMA, SPACING, MagikPunctuator.PAREN_L, PRAGMA_PARAMS, MagikPunctuator.PAREN_R);
    b.rule(PRAGMA_PARAMS).is(PRAGMA_PARAM, b.zeroOrMore(MagikPunctuator.COMMA, PRAGMA_PARAM));
    b.rule(PRAGMA_PARAM).is(IDENTIFIER, SPACING, MagikPunctuator.EQ, PRAGMA_VALUE);
    b.rule(PRAGMA_VALUE).is(b.firstOf(
        IDENTIFIER,
        b.sequence(MagikPunctuator.BRACE_L, IDENTIFIER, b.zeroOrMore(MagikPunctuator.COMMA, IDENTIFIER), MagikPunctuator.BRACE_R)));

    b.rule(PACKAGE_SPECIFICATION).is(MagikKeyword.PACKAGE, IDENTIFIER, EOS);
  }

  // CHECKSTYLE.ON: LineLength
  // CHECKSTYLE.ON: LocalVariableName
  // CHECKSTYLE.ON: ParameterName

}
