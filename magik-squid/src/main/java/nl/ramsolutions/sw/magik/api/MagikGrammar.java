package nl.ramsolutions.sw.magik.api;

import com.sonar.sslr.api.GenericTokenType;
import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.grammar.LexerlessGrammarBuilder;
import org.sonar.sslr.parser.LexerlessGrammar;

/**
 * Magik grammar.
 */
@SuppressWarnings({"checkstyle:JavadocVariable", "checkstyle:LineLength", "checkstyle:MethodLength"})
public enum MagikGrammar implements GrammarRuleKey {

    // spacing
    WHITESPACE,
    NEWLINE,
    COMMENT,
    SPACING,
    SPACING_NO_LB,
    NEXT_NOT_LB,

    // specials
    SYNTAX_ERROR,

    // root
    MAGIK,
    PACKAGE_SPECIFICATION,
    METHOD_DEFINITION,
    METHOD_DEFINITION_SYNTAX_ERROR,
    TRANSMIT,

    // pragma
    PRAGMA,
    PRAGMA_PARAMS,
    PRAGMA_PARAM,
    PRAGMA_VALUE,

    // constructs
    PARAMETERS,
    PARAMETERS_PAREN,
    PARAMETERS_PAREN_SYNTAX_ERROR,
    PARAMETERS_SQUARE,
    PARAMETERS_SQUARE_SYNTAX_ERROR,
    PARAMETER,
    ASSIGNMENT_PARAMETER,
    ARGUMENTS,
    ARGUMENTS_PAREN,
    ARGUMENTS_PAREN_SYNTAX_ERROR,
    ARGUMENTS_SQUARE,
    ARGUMENTS_SQUARE_SYNTAX_ERROR,
    ARGUMENT,
    ASSIGNMENT_ARGUMENT,
    BODY,
    OPERATOR,
    TUPLE,
    IDENTIFIERS,
    IDENTIFIERS_WITH_GATHER,
    METHOD_INVOCATION,
    PROCEDURE_INVOCATION,
    METHOD_MODIFIERS,
    PARAMETER_MODIFIER,
    VARIABLE_DEFINITION_MODIFIER,
    VARIABLE_DEFINITION_MULTI,
    VARIABLE_DEFINITION,
    MULTIPLE_ASSIGNMENT_ASSIGNABLES,

    TRY_VARIABLE,
    FOR_VARIABLES,
    CONDITIONAL_EXPRESSION,
    ITERABLE_EXPRESSION,

    // statements
    STATEMENT,
    STATEMENT_SYNTAX_ERROR,
    STATEMENT_SEPARATOR,
    VARIABLE_DEFINITION_STATEMENT,
    MULTIPLE_ASSIGNMENT_STATEMENT,
    RETURN_STATEMENT,
    EMIT_STATEMENT,
    EXPRESSION_STATEMENT,
    PRIMITIVE_STATEMENT,
    LEAVE_STATEMENT,
    CONTINUE_STATEMENT,
    THROW_STATEMENT,
    HANDLING,
    BLOCK,
    BLOCK_SYNTAX_ERROR,
    PROTECT,
    PROTECT_SYNTAX_ERROR,
    PROTECTION,
    TRY,
    TRY_SYNTAX_ERROR,
    WHEN,
    CATCH,
    CATCH_SYNTAX_ERROR,
    LOCK,
    LOCK_SYNTAX_ERROR,
    IF,
    IF_SYNTAX_ERROR,
    ELIF,
    ELSE,
    FOR,
    WHILE,
    OVER,
    LOOP,
    LOOP_SYNTAX_ERROR,
    FINALLY,

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

    // atoms
    STRING,
    NUMBER,
    CHARACTER,
    SYMBOL,
    REGEXP,
    IDENTIFIER,
    LABEL,
    SLOT,
    GLOBAL_REF,
    SIMPLE_VECTOR,
    SIMPLE_VECTOR_SYNTAX_ERROR,
    GATHER_EXPRESSION,
    CLASS,
    LOOPBODY,
    PROCEDURE_DEFINITION,
    PROCEDURE_DEFINITION_SYNTAX_ERROR,
    SELF,
    CLONE,
    UNSET,
    TRUE,
    FALSE,
    MAYBE,
    THISTHREAD,
    SUPER;

    private static final String WHITESPACE_SINGLE_REGEXP = "\\t\\u0020\\u00A0\\uFEFF";
    private static final String NEWLINE_REGEXP = "(?:\\n|\\r\\n|\\r)";
    private static final String COMMENT_REGEXP = "(?s)#[^\r\n]*";

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
    private static final String REGEXP_REGEXP = "/[^/]*/[cdilmqsux]*";

    private static final String SIMPLE_IDENTIFIER_REGEXP = "([a-z!?]|\\\\.)([a-z0-9_!?]|\\\\.)*";
    private static final String PIPED_IDENTIFIER_REGEXP = "\\|[^\\|]*\\|";
    private static final String BARE_IDENTIFIER_REGEXP = "(" + SIMPLE_IDENTIFIER_REGEXP + "|" + PIPED_IDENTIFIER_REGEXP + ")";
    private static final String LABEL_REGEXP = "(?is)" + BARE_IDENTIFIER_REGEXP + "+";
    private static final String IDENTIFIER_REGEXP = "(?is)(" + BARE_IDENTIFIER_REGEXP + "[" + WHITESPACE_SINGLE_REGEXP + "]*" + ":" + "[" + WHITESPACE_SINGLE_REGEXP + "]*" + ")?(" + BARE_IDENTIFIER_REGEXP + ")+";

    private static final String SIMPLE_SYMBOL_REGEXP = "([a-z0-9_!?]|\\\\.)+";
    private static final String PIPED_SYMBOL_REGEXP = "(\\|[^\\|]*\\|)";
    private static final String SYMBOL_REGEXP = "(?is):[" + WHITESPACE_SINGLE_REGEXP + "]*(" + SIMPLE_SYMBOL_REGEXP + "|" + PIPED_SYMBOL_REGEXP + ")+";

    /**
     * Create a new LexerlessGrammar for the Magik language.
     * @return Grammar for the Magik language
     */
    public static LexerlessGrammar create() {
        final LexerlessGrammarBuilder b = LexerlessGrammarBuilder.create();

        b.rule(NEWLINE).is(
            b.commentTrivia(b.regexp(NEWLINE_REGEXP))).skip();
        b.rule(WHITESPACE).is(
            b.commentTrivia(b.regexp("[" + WHITESPACE_SINGLE_REGEXP + "]+"))).skip();
        b.rule(COMMENT).is(
            b.commentTrivia(b.regexp(COMMENT_REGEXP))).skip();
        b.rule(SPACING).is(
            b.zeroOrMore(
                b.firstOf(
                    WHITESPACE,
                    NEWLINE,
                    COMMENT))).skip();
        b.rule(SPACING_NO_LB).is(
            b.zeroOrMore(
                b.commentTrivia(b.regexp("[\\s&&[^\n\r]]++")))).skip();
        b.rule(NEXT_NOT_LB).is(
            b.nextNot(b.regexp("[\n\r]"))).skip();

        b.rule(MAGIK).is(
            b.zeroOrMore(
                b.firstOf(
                    PACKAGE_SPECIFICATION,
                    PRAGMA,
                    METHOD_DEFINITION,
                    STATEMENT_SEPARATOR,
                    b.sequence(
                        STATEMENT,
                        b.firstOf(
                            STATEMENT_SEPARATOR,
                            b.next(SPACING, b.endOfInput()))),
                    TRANSMIT,
                    SYNTAX_ERROR)),
            SPACING,
            b.token(GenericTokenType.EOF, b.endOfInput()));

        b.rule(TRANSMIT).is(MagikPunctuator.DOLLAR);

        // Everything up to TRANSMIT that cannot be matched.
        b.rule(SYNTAX_ERROR).is(
            SPACING,
            b.firstOf(
                b.regexp(MagikGrammar.syntaxErrorRegexp(MagikPunctuator.DOLLAR)),
                b.regexp("(?s).+?(?=$)")));  // Match till end of input.

        MagikGrammar.operators(b);
        MagikGrammar.punctuators(b);
        MagikGrammar.keywords(b);
        MagikGrammar.atoms(b);
        MagikGrammar.expressions(b);
        MagikGrammar.statements(b);
        MagikGrammar.constructs(b);

        b.setRootRule(MAGIK);
        return b.build();
    }

    private static void operators(final LexerlessGrammarBuilder b) {
        for (final MagikOperator p : MagikOperator.values()) {
            b.rule(p).is(SPACING, p.getValue()).skip();
        }
    }

    private static void punctuators(final LexerlessGrammarBuilder b) {
        for (final MagikPunctuator p : MagikPunctuator.values()) {
            b.rule(p).is(SPACING, p.getValue()).skip();
        }
    }

    private static void keywords(final LexerlessGrammarBuilder b) {
        for (final MagikKeyword k : MagikKeyword.values()) {
            b.rule(k).is(SPACING, b.regexp("(?i)" + k.getValue() + "(?!\\w)")).skip();
        }
    }

    private static void atoms(final LexerlessGrammarBuilder b) {
        b.rule(STRING).is(SPACING, b.regexp(STRING_REGEXP));
        b.rule(NUMBER).is(SPACING, b.regexp(NUMBER_REGEXP));
        b.rule(CHARACTER).is(SPACING, b.regexp(CHARACTER_REGEXP));
        b.rule(REGEXP).is(SPACING, b.regexp(REGEXP_REGEXP));
        b.rule(IDENTIFIER).is(SPACING, b.regexp(IDENTIFIER_REGEXP));
        b.rule(SYMBOL).is(SPACING, b.regexp(SYMBOL_REGEXP));
        b.rule(GLOBAL_REF).is(MagikPunctuator.AT, SPACING, b.regexp(IDENTIFIER_REGEXP));

        b.rule(PROCEDURE_DEFINITION).is(
            b.optional(MagikKeyword.ITER),
            MagikKeyword.PROC, b.optional(LABEL),
            b.firstOf(
                b.sequence(
                    PARAMETERS_PAREN,
                    BODY,
                    b.next(MagikKeyword.ENDPROC)),
                PROCEDURE_DEFINITION_SYNTAX_ERROR),
            MagikKeyword.ENDPROC);
        b.rule(PROCEDURE_DEFINITION_SYNTAX_ERROR).is(
            SPACING,
            b.regexp(MagikGrammar.syntaxErrorRegexp(MagikKeyword.ENDPROC)));
    }

    private static void expressions(final LexerlessGrammarBuilder b) {
        b.rule(SLOT).is(MagikPunctuator.DOT, SPACING_NO_LB, NEXT_NOT_LB, IDENTIFIER);
        b.rule(SIMPLE_VECTOR).is(
            MagikPunctuator.BRACE_L,
            b.firstOf(
                b.sequence(
                    b.optional(EXPRESSION, b.zeroOrMore(MagikPunctuator.COMMA, EXPRESSION)),
                    b.next(MagikPunctuator.BRACE_R)),
                SIMPLE_VECTOR_SYNTAX_ERROR),
            MagikPunctuator.BRACE_R);
        b.rule(SIMPLE_VECTOR_SYNTAX_ERROR).is(
            SPACING,
            b.regexp(MagikGrammar.syntaxErrorRegexp(MagikPunctuator.BRACE_R)));

        b.rule(CLASS).is(MagikKeyword.CLASS, IDENTIFIER);
        b.rule(GATHER_EXPRESSION).is(MagikKeyword.GATHER, TUPLE);

        b.rule(EXPRESSION).is(ASSIGNMENT_EXPRESSION);
        b.rule(ASSIGNMENT_EXPRESSION).is(AUGMENTED_ASSIGNMENT_EXPRESSION, b.zeroOrMore(SPACING_NO_LB, NEXT_NOT_LB, b.firstOf(MagikOperator.CHEVRON, MagikOperator.BOOT_CHEVRON), AUGMENTED_ASSIGNMENT_EXPRESSION)).skipIfOneChild();
        b.rule(AUGMENTED_ASSIGNMENT_EXPRESSION).is(OR_EXPRESSION, b.zeroOrMore(SPACING_NO_LB, NEXT_NOT_LB, OPERATOR, b.firstOf(MagikOperator.CHEVRON, MagikOperator.BOOT_CHEVRON), OR_EXPRESSION)).skipIfOneChild();
        b.rule(OR_EXPRESSION).is(XOR_EXPRESSION, b.zeroOrMore(SPACING_NO_LB, NEXT_NOT_LB, b.firstOf(MagikKeyword.ORIF, MagikKeyword.OR), XOR_EXPRESSION)).skipIfOneChild();
        b.rule(XOR_EXPRESSION).is(AND_EXPRESSION, b.zeroOrMore(SPACING_NO_LB, NEXT_NOT_LB, MagikKeyword.XOR, AND_EXPRESSION)).skipIfOneChild();
        b.rule(AND_EXPRESSION).is(EQUALITY_EXPRESSION, b.zeroOrMore(SPACING_NO_LB, NEXT_NOT_LB, b.firstOf(MagikKeyword.ANDIF, MagikKeyword.AND), EQUALITY_EXPRESSION)).skipIfOneChild();
        b.rule(EQUALITY_EXPRESSION).is(RELATIONAL_EXPRESSION, b.zeroOrMore(SPACING_NO_LB, NEXT_NOT_LB, b.firstOf(MagikKeyword.ISNT, MagikKeyword.IS, MagikKeyword.CF, MagikOperator.EQ, MagikOperator.NEQ, MagikOperator.NE), RELATIONAL_EXPRESSION)).skipIfOneChild();
        b.rule(RELATIONAL_EXPRESSION).is(ADDITIVE_EXPRESSION, b.zeroOrMore(SPACING_NO_LB, NEXT_NOT_LB, b.firstOf(MagikOperator.GE, MagikOperator.GT, MagikOperator.LE, MagikOperator.LT), ADDITIVE_EXPRESSION)).skipIfOneChild();
        b.rule(ADDITIVE_EXPRESSION).is(MULTIPLICATIVE_EXPRESSION, b.zeroOrMore(SPACING_NO_LB, NEXT_NOT_LB, b.firstOf(MagikOperator.PLUS, MagikOperator.MINUS), MULTIPLICATIVE_EXPRESSION)).skipIfOneChild();
        b.rule(MULTIPLICATIVE_EXPRESSION).is(EXPONENTIAL_EXPRESSION, b.zeroOrMore(SPACING_NO_LB, NEXT_NOT_LB, b.firstOf(MagikOperator.STAR, MagikOperator.DIV, MagikKeyword.DIV, MagikKeyword.MOD), EXPONENTIAL_EXPRESSION)).skipIfOneChild();
        b.rule(EXPONENTIAL_EXPRESSION).is(UNARY_EXPRESSION, b.zeroOrMore(SPACING_NO_LB, NEXT_NOT_LB, MagikOperator.EXP, UNARY_EXPRESSION)).skipIfOneChild();
        b.rule(UNARY_EXPRESSION).is(b.firstOf(
            b.sequence(b.firstOf(MagikKeyword.ALLRESULTS, MagikKeyword.SCATTER, MagikOperator.NOT, MagikKeyword.NOT, MagikOperator.PLUS, MagikOperator.MINUS), UNARY_EXPRESSION),
            POSTFIX_EXPRESSION)).skipIfOneChild();
        b.rule(POSTFIX_EXPRESSION).is(
            ATOM,
            b.zeroOrMore(
                SPACING_NO_LB, NEXT_NOT_LB,
                b.firstOf(
                    METHOD_INVOCATION,
                    PROCEDURE_INVOCATION))).skipIfOneChild();

        b.rule(ATOM).is(
            b.firstOf(
                b.sequence(MagikPunctuator.PAREN_L, EXPRESSION, MagikPunctuator.PAREN_R),
                NUMBER,
                STRING,
                SYMBOL,
                CHARACTER,
                REGEXP,
                IDENTIFIER,
                GLOBAL_REF,
                SLOT,
                SIMPLE_VECTOR,
                CLASS,
                GATHER_EXPRESSION,
                LOOPBODY,
                PROCEDURE_DEFINITION,
                IF,
                FOR,
                OVER,
                LOOP,
                BLOCK,
                PROTECT,
                TRY,
                CATCH,
                LOCK,

                SELF,
                CLONE,
                UNSET,
                TRUE,
                FALSE,
                MAYBE,
                THISTHREAD,
                SUPER));
        b.rule(SELF).is(MagikKeyword.SELF);
        b.rule(CLONE).is(MagikKeyword.CLONE);
        b.rule(UNSET).is(MagikKeyword.UNSET);
        b.rule(TRUE).is(MagikKeyword.TRUE);
        b.rule(FALSE).is(MagikKeyword.FALSE);
        b.rule(MAYBE).is(MagikKeyword.MAYBE);
        b.rule(THISTHREAD).is(MagikKeyword.THISTHREAD);
        b.rule(SUPER).is(
            MagikKeyword.SUPER,
            b.optional(MagikPunctuator.PAREN_L, IDENTIFIER, MagikPunctuator.PAREN_R));

        b.rule(METHOD_INVOCATION).is(
            b.firstOf(
                b.sequence(
                    SPACING_NO_LB, NEXT_NOT_LB, MagikPunctuator.DOT, IDENTIFIER,
                    b.optional(
                        SPACING_NO_LB, NEXT_NOT_LB, ARGUMENTS_PAREN)),
                b.sequence(
                    SPACING_NO_LB, NEXT_NOT_LB, ARGUMENTS_SQUARE)),
            b.optional(
                SPACING_NO_LB, NEXT_NOT_LB,
                b.firstOf(
                    MagikOperator.CHEVRON,
                    MagikOperator.BOOT_CHEVRON),
                ASSIGNMENT_ARGUMENT));
        b.rule(PROCEDURE_INVOCATION).is(
            ARGUMENTS_PAREN);

        b.rule(CONDITIONAL_EXPRESSION).is(EXPRESSION);
        b.rule(ITERABLE_EXPRESSION).is(EXPRESSION);
    }

    private static void statements(final LexerlessGrammarBuilder b) {
        b.rule(STATEMENT).is(
            b.firstOf(
                MULTIPLE_ASSIGNMENT_STATEMENT,
                EXPRESSION_STATEMENT,
                RETURN_STATEMENT,
                EMIT_STATEMENT,
                CONTINUE_STATEMENT,
                LEAVE_STATEMENT,
                THROW_STATEMENT,
                VARIABLE_DEFINITION_STATEMENT,
                PRIMITIVE_STATEMENT));

        // b.rule(STATEMENT_SYNTAX_ERROR).is(
        //     SPACING,
        //     b.regexp(".+?(?=[" + LINE_TERMINATOR_REGEXP + "])"));

        b.rule(STATEMENT_SEPARATOR).is(
            b.firstOf(
                b.sequence(
                    SPACING_NO_LB,
                    b.commentTrivia(MagikPunctuator.SEMICOLON)),
                b.sequence(
                    SPACING_NO_LB,
                    b.optional(COMMENT),
                    NEWLINE))).skip();

        b.rule(BODY).is(
            b.zeroOrMore(HANDLING),
            b.zeroOrMore(STATEMENT_SEPARATOR),
            b.zeroOrMore(
                STATEMENT,
                b.firstOf(
                    b.oneOrMore(STATEMENT_SEPARATOR),
                    b.next(
                        b.firstOf(
                            // End of body by end-keyword.
                            MagikKeyword.ELIF,
                            MagikKeyword.ELSE,
                            MagikKeyword.ENDBLOCK,
                            MagikKeyword.ENDCATCH,
                            MagikKeyword.ENDIF,
                            MagikKeyword.ENDLOCK,
                            MagikKeyword.ENDLOOP,
                            MagikKeyword.ENDMETHOD,
                            MagikKeyword.ENDPROC,
                            MagikKeyword.ENDPROTECT,
                            MagikKeyword.ENDTRY,
                            MagikKeyword.PROTECTION,
                            MagikKeyword.WHEN)))));

        b.rule(VARIABLE_DEFINITION_STATEMENT).is(
            b.oneOrMore(VARIABLE_DEFINITION_MODIFIER),
            b.firstOf(
                VARIABLE_DEFINITION_MULTI,
                b.sequence(VARIABLE_DEFINITION, b.zeroOrMore(MagikPunctuator.COMMA, VARIABLE_DEFINITION))));
        b.rule(VARIABLE_DEFINITION_MODIFIER).is(
            b.firstOf(
                MagikKeyword.LOCAL,
                MagikKeyword.CONSTANT,
                MagikKeyword.RECURSIVE,
                MagikKeyword.GLOBAL,
                MagikKeyword.DYNAMIC,
                MagikKeyword.IMPORT));
        b.rule(VARIABLE_DEFINITION).is(
            IDENTIFIER,
            b.optional(
                MagikOperator.CHEVRON,
                EXPRESSION));
        b.rule(VARIABLE_DEFINITION_MULTI).is(
            MagikPunctuator.PAREN_L,
            IDENTIFIERS_WITH_GATHER,
            MagikPunctuator.PAREN_R,
            MagikOperator.CHEVRON,
            TUPLE);

        b.rule(MULTIPLE_ASSIGNMENT_STATEMENT).is(
            MagikPunctuator.PAREN_L,
            MULTIPLE_ASSIGNMENT_ASSIGNABLES,
            MagikPunctuator.PAREN_R,
            MagikOperator.CHEVRON,
            TUPLE);
        b.rule(MULTIPLE_ASSIGNMENT_ASSIGNABLES).is(
            b.firstOf(
                b.sequence(
                    MagikKeyword.GATHER,
                    EXPRESSION),
                b.sequence(
                    EXPRESSION,
                    b.zeroOrMore(
                        MagikPunctuator.COMMA,
                        EXPRESSION),
                    b.optional(
                        MagikPunctuator.COMMA,
                        MagikKeyword.GATHER,
                        EXPRESSION))));
        b.rule(RETURN_STATEMENT).is(
            MagikKeyword.RETURN, b.optional(SPACING_NO_LB, NEXT_NOT_LB, TUPLE));
        b.rule(EMIT_STATEMENT).is(MagikPunctuator.EMIT, TUPLE);
        b.rule(EXPRESSION_STATEMENT).is(EXPRESSION);
        b.rule(PRIMITIVE_STATEMENT).is(MagikKeyword.PRIMITIVE, NUMBER);
        b.rule(LEAVE_STATEMENT).is(
            MagikKeyword.LEAVE, b.optional(LABEL),
            b.optional(SPACING_NO_LB, NEXT_NOT_LB, MagikKeyword.WITH, TUPLE));
        b.rule(CONTINUE_STATEMENT).is(
            MagikKeyword.CONTINUE, b.optional(LABEL),
            b.optional(SPACING_NO_LB, NEXT_NOT_LB, MagikKeyword.WITH, TUPLE));
        b.rule(LOOPBODY).is(
            MagikKeyword.LOOPBODY,
            MagikPunctuator.PAREN_L, b.optional(TUPLE), MagikPunctuator.PAREN_R);
        b.rule(THROW_STATEMENT).is(
            MagikKeyword.THROW, EXPRESSION,
            b.optional(SPACING_NO_LB, NEXT_NOT_LB, MagikKeyword.WITH, TUPLE));

        b.rule(HANDLING).is(
            MagikKeyword.HANDLING, b.firstOf(
                b.sequence(
                    IDENTIFIERS,
                    MagikKeyword.WITH,
                    b.firstOf(EXPRESSION, MagikKeyword.DEFAULT)),
                MagikKeyword.DEFAULT),
            STATEMENT_SEPARATOR);  // SW5 requires a statement separator, either `;` or `\n`.

        b.rule(BLOCK).is(
            MagikKeyword.BLOCK, b.optional(LABEL),
            b.firstOf(
                b.sequence(
                    BODY,
                    b.next(MagikKeyword.ENDBLOCK)),
                BLOCK_SYNTAX_ERROR),
            MagikKeyword.ENDBLOCK);
        b.rule(BLOCK_SYNTAX_ERROR).is(
            SPACING,
            b.regexp(MagikGrammar.syntaxErrorRegexp(MagikKeyword.ENDBLOCK)));

        b.rule(PROTECT).is(
            MagikKeyword.PROTECT, b.optional(MagikKeyword.LOCKING, EXPRESSION),
            b.firstOf(
                b.sequence(
                    BODY,
                    PROTECTION,
                    b.next(MagikKeyword.ENDPROTECT)),
                PROTECT_SYNTAX_ERROR),
            MagikKeyword.ENDPROTECT);
        b.rule(PROTECTION).is(
            MagikKeyword.PROTECTION,
            BODY);
        b.rule(PROTECT_SYNTAX_ERROR).is(
            SPACING,
            b.regexp(MagikGrammar.syntaxErrorRegexp(MagikKeyword.ENDPROTECT)));

        b.rule(TRY).is(
            MagikKeyword.TRY, b.optional(MagikKeyword.WITH, TRY_VARIABLE),
            b.firstOf(
                b.sequence(
                    BODY,
                    b.oneOrMore(WHEN),
                    b.next(MagikKeyword.ENDTRY)),
                TRY_SYNTAX_ERROR),
            MagikKeyword.ENDTRY);
        b.rule(TRY_SYNTAX_ERROR).is(
            SPACING,
            b.regexp(MagikGrammar.syntaxErrorRegexp(MagikKeyword.ENDTRY)));
        b.rule(TRY_VARIABLE).is(IDENTIFIER);
        b.rule(WHEN).is(
            MagikKeyword.WHEN, IDENTIFIERS,
            BODY);

        b.rule(CATCH).is(
            MagikKeyword.CATCH, b.optional(SPACING_NO_LB, NEXT_NOT_LB, EXPRESSION),
            b.firstOf(
                b.sequence(
                    BODY,
                    b.next(MagikKeyword.ENDCATCH)),
                CATCH_SYNTAX_ERROR),
            MagikKeyword.ENDCATCH);
        b.rule(CATCH_SYNTAX_ERROR).is(
            SPACING,
            b.regexp(MagikGrammar.syntaxErrorRegexp(MagikKeyword.ENDCATCH)));

        b.rule(LOCK).is(
            MagikKeyword.LOCK, EXPRESSION,
            b.firstOf(
                b.sequence(
                    BODY,
                    b.next(MagikKeyword.ENDLOCK)),
                LOCK_SYNTAX_ERROR),
            MagikKeyword.ENDLOCK);
        b.rule(LOCK_SYNTAX_ERROR).is(
            SPACING,
            b.regexp(MagikGrammar.syntaxErrorRegexp(MagikKeyword.ENDLOCK)));

        b.rule(IF).is(
            MagikKeyword.IF,
            b.firstOf(
                b.sequence(
                    CONDITIONAL_EXPRESSION, MagikKeyword.THEN,
                    BODY,
                    b.zeroOrMore(ELIF),
                    b.optional(ELSE),
                    b.next(MagikKeyword.ENDIF)),
                IF_SYNTAX_ERROR),
            MagikKeyword.ENDIF);
        b.rule(IF_SYNTAX_ERROR).is(
            SPACING,
            b.regexp(MagikGrammar.syntaxErrorRegexp(MagikKeyword.ENDIF)));
        b.rule(ELIF).is(
            MagikKeyword.ELIF, CONDITIONAL_EXPRESSION,
            MagikKeyword.THEN,
            BODY);
        b.rule(ELSE).is(
            MagikKeyword.ELSE,
            BODY);

        b.rule(FOR).is(
            MagikKeyword.FOR, FOR_VARIABLES,
            OVER);
        b.rule(FOR_VARIABLES).is(
            IDENTIFIERS_WITH_GATHER);
        b.rule(WHILE).is(
            MagikKeyword.WHILE, CONDITIONAL_EXPRESSION,
            LOOP);
        b.rule(OVER).is(
            MagikKeyword.OVER, ITERABLE_EXPRESSION,
            LOOP);
        b.rule(FINALLY).is(
            MagikKeyword.FINALLY, b.optional(MagikKeyword.WITH, IDENTIFIERS_WITH_GATHER),
            BODY);

        b.rule(LOOP).is(
            MagikKeyword.LOOP, b.optional(LABEL),
            b.firstOf(
                b.sequence(
                    BODY,
                    b.optional(FINALLY),
                    b.next(MagikKeyword.ENDLOOP)),
                LOOP_SYNTAX_ERROR),
            MagikKeyword.ENDLOOP);
        b.rule(LOOP_SYNTAX_ERROR).is(
            SPACING,
            b.regexp(MagikGrammar.syntaxErrorRegexp(MagikKeyword.ENDLOOP)));
    }

    private static void constructs(final LexerlessGrammarBuilder b) {
        b.rule(METHOD_DEFINITION).is(
            METHOD_MODIFIERS,
            MagikKeyword.METHOD,
            b.firstOf(
                b.sequence(
                    IDENTIFIER,
                    b.firstOf(
                        b.sequence(
                            MagikPunctuator.DOT, IDENTIFIER,
                            b.optional(
                                SPACING_NO_LB, NEXT_NOT_LB,
                                PARAMETERS_PAREN)),
                        PARAMETERS_SQUARE),
                    b.optional(
                        SPACING_NO_LB, NEXT_NOT_LB,
                        b.firstOf(MagikOperator.CHEVRON, MagikOperator.BOOT_CHEVRON),
                        ASSIGNMENT_PARAMETER),
                    BODY,
                    b.next(MagikKeyword.ENDMETHOD)),
                METHOD_DEFINITION_SYNTAX_ERROR),
            MagikKeyword.ENDMETHOD);
        b.rule(METHOD_DEFINITION_SYNTAX_ERROR).is(
            SPACING,
            b.regexp(MagikGrammar.syntaxErrorRegexp(MagikKeyword.ENDMETHOD)));

        b.rule(METHOD_MODIFIERS).is(
            b.optional(MagikKeyword.ABSTRACT),
            b.optional(MagikKeyword.PRIVATE),
            b.optional(MagikKeyword.ITER));

        b.rule(PARAMETERS_PAREN).is(
            MagikPunctuator.PAREN_L,
            b.firstOf(
                b.sequence(
                    PARAMETERS,
                    b.next(MagikPunctuator.PAREN_R)),
                PARAMETERS_PAREN_SYNTAX_ERROR),
            MagikPunctuator.PAREN_R);
        b.rule(PARAMETERS_PAREN_SYNTAX_ERROR).is(
            SPACING,
            b.regexp(MagikGrammar.syntaxErrorRegexp(MagikPunctuator.PAREN_R)));
        b.rule(PARAMETERS_SQUARE).is(
            MagikPunctuator.SQUARE_L,
            b.firstOf(
                b.sequence(
                    b.nextNot(MagikKeyword.OPTIONAL),
                    b.nextNot(MagikKeyword.GATHER),
                    PARAMETERS,
                    b.next(MagikPunctuator.SQUARE_R)),
                PARAMETERS_SQUARE_SYNTAX_ERROR),
            MagikPunctuator.SQUARE_R);
        b.rule(PARAMETERS_SQUARE_SYNTAX_ERROR).is(
            SPACING,
            b.regexp(MagikGrammar.syntaxErrorRegexp(MagikPunctuator.SQUARE_R)));
        b.rule(PARAMETERS).is(
            b.optional(
                // Mandatory parameters.
                b.optional(
                    b.nextNot(MagikKeyword.OPTIONAL),
                    b.nextNot(MagikKeyword.GATHER),
                    PARAMETER,
                    b.zeroOrMore(
                        MagikPunctuator.COMMA,
                        b.nextNot(MagikKeyword.OPTIONAL),
                        b.nextNot(MagikKeyword.GATHER),
                        PARAMETER)),
                // Optional parameters.
                b.optional(
                    b.optional(MagikPunctuator.COMMA),
                    b.next(MagikKeyword.OPTIONAL),
                    PARAMETER,
                    b.zeroOrMore(
                        MagikPunctuator.COMMA,
                        b.nextNot(MagikKeyword.OPTIONAL),
                        b.nextNot(MagikKeyword.GATHER),
                        PARAMETER)),
                // Gathered parameters.
                b.optional(
                    b.optional(MagikPunctuator.COMMA),
                    b.next(MagikKeyword.GATHER),
                    PARAMETER))).skip();
        b.rule(PARAMETER).is(
            b.optional(PARAMETER_MODIFIER),
            IDENTIFIER);
        b.rule(PARAMETER_MODIFIER).is(
            b.firstOf(
                MagikKeyword.GATHER,
                MagikKeyword.OPTIONAL));
        b.rule(ASSIGNMENT_PARAMETER).is(
            b.nextNot(MagikKeyword.OPTIONAL),
            b.nextNot(MagikKeyword.GATHER),
            PARAMETER);

        b.rule(ARGUMENTS_PAREN).is(
            MagikPunctuator.PAREN_L,
            b.firstOf(
                b.sequence(
                    ARGUMENTS,
                    b.next(MagikPunctuator.PAREN_R)),
                ARGUMENTS_PAREN_SYNTAX_ERROR),
            MagikPunctuator.PAREN_R);
        b.rule(ARGUMENTS_PAREN_SYNTAX_ERROR).is(
            SPACING,
            b.regexp(MagikGrammar.syntaxErrorRegexp(MagikPunctuator.PAREN_R)));
        b.rule(ARGUMENTS_SQUARE).is(
            MagikPunctuator.SQUARE_L,
            b.firstOf(
                b.sequence(
                    ARGUMENTS,
                    b.next(MagikPunctuator.SQUARE_R)),
                ARGUMENTS_SQUARE_SYNTAX_ERROR),
            MagikPunctuator.SQUARE_R);
        b.rule(ARGUMENTS_SQUARE_SYNTAX_ERROR).is(
            SPACING,
            b.regexp(MagikGrammar.syntaxErrorRegexp(MagikPunctuator.SQUARE_R)));
        b.rule(ARGUMENTS).is(
            b.optional(
                // Normal arguments.
                b.optional(
                    ARGUMENT,
                    b.zeroOrMore(
                        MagikPunctuator.COMMA,
                        ARGUMENT)),
                b.optional(
                    b.optional(MagikPunctuator.COMMA),
                    b.next(MagikKeyword.SCATTER),
                    ARGUMENT))).skip();
        b.rule(ARGUMENT).is(EXPRESSION);
        b.rule(ASSIGNMENT_ARGUMENT).is(ARGUMENT);

        b.rule(TUPLE).is(
            b.firstOf(
                b.sequence(
                    EXPRESSION,
                    b.zeroOrMore(SPACING_NO_LB, NEXT_NOT_LB, MagikPunctuator.COMMA, EXPRESSION)),
                b.sequence(
                    MagikPunctuator.PAREN_L,
                    EXPRESSION,
                    b.zeroOrMore(MagikPunctuator.COMMA, EXPRESSION),
                    MagikPunctuator.PAREN_R)));

        b.rule(IDENTIFIERS).is(
            IDENTIFIER,
            b.zeroOrMore(
                SPACING_NO_LB, NEXT_NOT_LB, MagikPunctuator.COMMA, IDENTIFIER));
        b.rule(IDENTIFIERS_WITH_GATHER).is(
            b.firstOf(
                b.sequence(
                    MagikKeyword.GATHER,
                    IDENTIFIER),
                b.sequence(
                    IDENTIFIER,
                    b.zeroOrMore(
                        MagikPunctuator.COMMA,
                        IDENTIFIER),
                    b.optional(
                        MagikPunctuator.COMMA,
                        MagikKeyword.GATHER,
                        IDENTIFIER))));

        b.rule(OPERATOR).is(
            b.firstOf(
                MagikKeyword.ANDIF, MagikKeyword.AND,
                MagikKeyword.ORIF, MagikKeyword.OR,
                MagikKeyword.XOR,
                MagikKeyword.DIV, MagikKeyword.MOD,
                MagikKeyword.CF,
                MagikKeyword.IS, MagikKeyword.ISNT,
                MagikOperator.PLUS, MagikOperator.MINUS,
                MagikOperator.STAR, MagikOperator.DIV,
                MagikOperator.EXP,
                MagikOperator.EQ, MagikOperator.NEQ
                )).skip();

        b.rule(PRAGMA).is(MagikKeyword.PRAGMA, MagikPunctuator.PAREN_L, PRAGMA_PARAMS, MagikPunctuator.PAREN_R);
        b.rule(PRAGMA_PARAMS).is(PRAGMA_PARAM, b.zeroOrMore(MagikPunctuator.COMMA, PRAGMA_PARAM));
        b.rule(PRAGMA_PARAM).is(IDENTIFIER, MagikOperator.EQ, PRAGMA_VALUE);
        b.rule(PRAGMA_VALUE).is(b.firstOf(
            IDENTIFIER,
            b.sequence(
                MagikPunctuator.BRACE_L,
                IDENTIFIER,
                b.zeroOrMore(MagikPunctuator.COMMA, IDENTIFIER),
                MagikPunctuator.BRACE_R)));

        b.rule(PACKAGE_SPECIFICATION).is(MagikKeyword.PACKAGE, IDENTIFIER);

        b.rule(LABEL).is(MagikPunctuator.AT, SPACING, b.regexp(LABEL_REGEXP));
    }

    static String syntaxErrorRegexp(final MagikKeyword keyword) {
        return "(?s).+?(?=(?i)" + keyword.getValue() + ")";
    }

    static String syntaxErrorRegexp(final MagikPunctuator punctuator) {
        return "(?s).+?(?=\\" + punctuator.getValue() + ")";
    }

}
