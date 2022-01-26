package nl.ramsolutions.sw.magik.api;

import com.sonar.sslr.api.Rule;
import org.junit.jupiter.api.Test;
import org.sonar.sslr.parser.LexerlessGrammar;
import org.sonar.sslr.tests.Assertions;

/**
 * Test MagikGrammar.
 */
class MagikGrammarTest {

    private final LexerlessGrammar g = MagikGrammar.create();

    @Test
    void testPragma() {
        Assertions.assertThat(g.rule(MagikGrammar.PRAGMA))
            .matches("_pragma(a=b)")
            .matches("_pragma(a=b,c=d)")
            .matches("_pragma(a={b,c})");
    }

    @Test
    void testPackage() {
        Assertions.assertThat(g.rule(MagikGrammar.PACKAGE_SPECIFICATION))
            .matches("_package sw")
            .matches("_package user")
            .notMatches("_package p1:a");
    }

    @Test
    void testHandling() {
        Assertions.assertThat(g.rule(MagikGrammar.HANDLING))
            .matches("_handling _default\n")
            .matches("_handling a _with _default\n")
            .matches("_handling a _with x\n")
            .matches("_handling a, b _with x\n")
            .matches("_handling a, b _with _default\n");
    }

    @Test
    void testBlock() {
        MagikRuleForbiddenAssert.assertThat(g.rule(MagikGrammar.BLOCK), MagikGrammar.BLOCK_SYNTAX_ERROR)
            .matches("_block _endblock")
            .matches("_BLOCK _ENDBLOCK")
            .matches("_block \n _endblock")
            .matches("_block ; _endblock")
            .matches("_block \n; _endblock")
            .matches("_block _return _endblock")
            .matches("_block _return ; _endblock")
            .matches("_block\n_handling _default\n_endblock")
            .matches("_block\nremex(:type)\n_endblock")
            .matches("_block write(1) _endblock")
            .matches("_block write(1);write(2) _endblock")
            .matches("_block write(1);;write(2) _endblock")
            .matches("_block write(1);;;write(2) _endblock")
            .matches("_block write(1);write(2);write(3) _endblock")
            .matches("_block write(1)\nwrite(2) _endblock")
            .matches("_block write(1)\n\nwrite(2) _endblock")
            .matches("_block write(1)\n\n\nwrite(2) _endblock")
            .matches("_block write(1)\nwrite(2)\nwrite(3) _endblock")
            .matches("_block a.write(1);b.write(2) _endblock")
            .matches("_block _handling _default\nwrite(1) _endblock")
            .matches("_block _continue _endblock")
            .matches("_block _continue @label _endblock")
            .matches("_block _continue _with 1 _endblock")
            .matches("_block _continue _with e() _endblock")
            .matches("_block _continue _with (1, 2) _endblock")
            .matches("_block _leave _endblock")
            .matches("_block _leave @label _endblock")
            .matches("_block _leave _with 1 _endblock")
            .matches("_block _leave _with e() _endblock")
            .matches("_block _leave _with (1, 2) _endblock")
            .matches("_block _block _endblock _endblock")
            .matches("_block @label _endblock");
        MagikRuleRequiredAssert.assertThat(g.rule(MagikGrammar.BLOCK), MagikGrammar.BLOCK_SYNTAX_ERROR)
            .matches("_block _a _endblock")
            .matches("_block\n_a\n_endblock")
            .matches("_block write(1) write(2) _endblock")
            .matches("_block write(1)write(2) _endblock")
            .matches("_block _continue\n_with 10 _endblock")
            .matches("_block _leave\n_with 10 _endblock");
    }

    @Test
    void testTry() {
        MagikRuleForbiddenAssert.assertThat(g.rule(MagikGrammar.TRY), MagikGrammar.TRY_SYNTAX_ERROR)
            .matches("_try _when error _endtry")
            .matches("_try expr() _when error _endtry")
            .matches("_try _with e _when error _endtry")
            .matches("_try _with e expr() _when error _endtry");
        MagikRuleRequiredAssert.assertThat(g.rule(MagikGrammar.TRY), MagikGrammar.TRY_SYNTAX_ERROR)
            .matches("_try _w _endtry")
            .matches("_try _when _endtry")
            .matches("_try _a _when error _endtry");
    }

    @Test
    void testCatch() {
        MagikRuleForbiddenAssert.assertThat(g.rule(MagikGrammar.CATCH), MagikGrammar.CATCH_SYNTAX_ERROR)
            .matches("_catch _endcatch")
            .matches("_catch :a\n_endcatch");
        MagikRuleRequiredAssert.assertThat(g.rule(MagikGrammar.CATCH), MagikGrammar.CATCH_SYNTAX_ERROR)
            .matches("_catch _a _endcatch");
    }

    @Test
    void testArguments() {
        final Rule rule = g.rule(MagikGrammar.ARGUMENTS_PAREN);
        MagikRuleForbiddenAssert.assertThat(rule, MagikGrammar.ARGUMENTS_PAREN_SYNTAX_ERROR)
            .matches("()")
            .matches("(1, 2)");
        // MagikRuleRequiredAssert.assertThat(rule, MagikGrammar.ARGUMENTS_PAREN_SYNTAX_ERROR)
        //     .matches("(, _self.a())");
    }

    @Test
    void testLoopBodyStatement() {
        Assertions.assertThat(g.rule(MagikGrammar.LOOPBODY))
            .matches("_loopbody()")
            .matches("_loopbody(1)")
            .matches("_loopbody(1, 2)");
    }

    @Test
    void testLeaveStatement() {
        Assertions.assertThat(g.rule(MagikGrammar.LEAVE_STATEMENT))
            .matches("_leave")
            .matches("_leave @label")
            .matches("_leave _with a")
            .matches("_leave _with (a, b)")
            .matches("_leave @label _with a")
            .matches("_leave _with 1, _scatter _allresults a.m()");
    }

    @Test
    void testContinueStatement() {
        Assertions.assertThat(g.rule(MagikGrammar.CONTINUE_STATEMENT))
            .matches("_continue")
            .matches("_continue @label")
            .matches("_continue _with a")
            .matches("_continue _with (a, b)")
            .matches("_continue @label _with a");
    }

    @Test
    void testThrow() {
        Assertions.assertThat(g.rule(MagikGrammar.THROW_STATEMENT))
            .matches("_throw :test")
            .matches("_throw @error _with _false")
            .matches("_throw :a _with 1, 2, 3")
            .notMatches("_throw a\n _with _false");
    }

    @Test
    void testLock() {
        MagikRuleForbiddenAssert.assertThat(g.rule(MagikGrammar.LOCK), MagikGrammar.LOCK_SYNTAX_ERROR)
            .matches("_lock x _endlock");
        MagikRuleRequiredAssert.assertThat(g.rule(MagikGrammar.LOCK), MagikGrammar.LOCK_SYNTAX_ERROR)
            .matches("_lock x _a _endlock");
    }

    @Test
    void testProcedureDeclaration() {
        final Rule rule = g.rule(MagikGrammar.PROCEDURE_DEFINITION);
        MagikRuleForbiddenAssert.assertThat(rule, MagikGrammar.PROCEDURE_DEFINITION_SYNTAX_ERROR)
            .matches("_proc() _endproc")
            .matches("_iter _proc() _endproc")
            .matches("_proc(a) _endproc")
            .matches("_proc(a, b) _endproc")
            .matches("_proc @test() _endproc")
            .matches("_proc @|test 123|() _endproc");
        MagikRuleRequiredAssert.assertThat(rule, MagikGrammar.PROCEDURE_DEFINITION_SYNTAX_ERROR)
            .matches("_proc() _a _endproc")
            .matches("_proc _a _endproc");
        MagikRuleRequiredAssert.assertThat(rule, MagikGrammar.PARAMETERS_PAREN_SYNTAX_ERROR)
            .matches("_proc(x, _a) _endproc");
    }

    @Test
    void testLabel() {
        Assertions.assertThat(g.rule(MagikGrammar.LABEL))
            .matches("@label")
            .matches("@ label")
            .matches("@LABEL")
            .matches("@label")
            .matches("@|label 123|");
    }

    @Test
    void testGlobalRef() {
        Assertions.assertThat(g.rule(MagikGrammar.GLOBAL_REF))
            .matches("@ref")
            .matches("@ ref")
            .matches("@REF")
            .matches("@ref1")
            .matches("@package:ref")
            .matches("@pkg_a:ref_a")
            .matches("@ pkg_a:ref_a")
            .matches("@|test 123|");
    }

    @Test
    void testFor() {
        Assertions.assertThat(g.rule(MagikGrammar.FOR))
            .matches("_for a _over a _loop _endloop")
            .matches("_for a, b _over a _loop _endloop")
            .matches("_for _gather x _over a _loop _endloop")
            .matches("_for x, _gather y _over a _loop _endloop");
    }

    @Test
    void testWhile() {
        Assertions.assertThat(g.rule(MagikGrammar.WHILE))
            .matches("_while a _loop _endloop")
            .matches("_while a _andif b _loop _endloop")
            .matches("_while a _isnt _unset _loop _endloop");
    }

    @Test
    void testLoop() {
        MagikRuleForbiddenAssert.assertThat(g.rule(MagikGrammar.LOOP), MagikGrammar.LOOP_SYNTAX_ERROR)
            .matches("_loop _endloop")
            .matches("_loop expr() _endloop")
            .matches("_loop _finally _endloop")
            .matches("_loop _finally _with total _endloop")
            .matches("_loop _finally _with _gather total _endloop")
            .matches("_loop _continue _with _false _endloop")
            .matches("_loop a << _loopbody(1) _endloop");
        MagikRuleRequiredAssert.assertThat(g.rule(MagikGrammar.LOOP), MagikGrammar.LOOP_SYNTAX_ERROR)
            .matches("_loop _a _endloop");
    }

    @Test
    void testIf() {
        MagikRuleForbiddenAssert.assertThat(g.rule(MagikGrammar.IF), MagikGrammar.IF_SYNTAX_ERROR)
            .matches("_if expr _then _endif")
            .matches("_if expr _then _else _endif")
            .matches("_if expr _then _elif expr _then _else _endif")
            .matches("_if expr _then >> 1 _endif")
            .matches("_if e _then _elif e _then _elif e _then _endif");
        MagikRuleRequiredAssert.assertThat(g.rule(MagikGrammar.IF), MagikGrammar.IF_SYNTAX_ERROR)
            .matches("_if _a _endif");
    }

    @Test
    void testEmitStatement() {
        Assertions.assertThat(g.rule(MagikGrammar.EMIT_STATEMENT))
            .matches(">> a")
            .matches(">> (a, b)")
            .matches(">> (\na, b)")
            .matches(">> (a) _mod b")
            .matches(">>\na")
            .matches(">> (a - b).m() < c");
    }

    @Test
    void testMethodInvocation() {
        Assertions.assertThat(g.rule(MagikGrammar.METHOD_INVOCATION))
            .matches(".method")
            .matches(".method()")
            .matches(".method(1)")
            .matches(".method(1, 2)")
            .matches(".method << a")
            .matches(".method() << a")
            .matches(".method(1) << a")
            .matches(".method(1, 2) << a")
            .matches("[1]")
            .matches("[1,2]")
            .matches("[1] << a")
            .matches("[1,2] << a")
            .matches("[p]")
            .matches(".method(1)")
            .matches(".method(1, 2)")
            .matches(".b(x _scatter y)")
            .matches(".b(x, y)")
            .matches(".b(x, y _scatter z)")
            .matches(".b(x, y, _scatter z)");
    }

    @Test
    void testProcedureInvocation() {
        Assertions.assertThat(g.rule(MagikGrammar.PROCEDURE_INVOCATION))
            .matches("()")
            .matches("(x _scatter y)")
            .matches("(x, y)")
            .matches("(x, y _scatter z)")
            .matches("(x, y, _scatter z)");
    }

    @Test
    void testReturnStatement() {
        Assertions.assertThat(g.rule(MagikGrammar.RETURN_STATEMENT))
            .matches("_return")
            .matches("_return a")
            .matches("_return (a)")
            .matches("_return (a).not")
            .matches("_return (a, b)")
            .matches("_return (\na, b)")
            .matches("_return (a) _mod b");
    }

    @Test
    void testPrimitiveStatement() {
        Assertions.assertThat(g.rule(MagikGrammar.PRIMITIVE_STATEMENT))
            .matches("_primitive 1")
            .matches("_primitive 512")
            .notMatches("_primitive");
    }

    @Test
    void testExpression() {
        Assertions.assertThat(g.rule(MagikGrammar.EXPRESSION))
            .matches("a()")
            .matches("expr()")
            .matches("a _is b")
            .matches("a _isnt b")
            .matches("a - b")
            .matches("a + b")
            .matches("a << b")
            .matches("a << b << c")
            .matches("a _and<< b")
            .matches("a _andif<< b")
            .matches("a -<< b")
            .matches("a +<< b")
            .matches("a << b + 1")
            .matches("a +<< 1")
            .matches("a + << 1")
            .matches("x +^<< 1")
            .matches("a _or b")
            .matches("(a) _or b")
            .matches("_allresults a()");
    }

    @Test
    void testMethodDefinition() {
        final Rule rule = g.rule(MagikGrammar.METHOD_DEFINITION);
        MagikRuleForbiddenAssert.assertThat(rule, MagikGrammar.METHOD_DEFINITION_SYNTAX_ERROR)
            .matches("_method a.b _endmethod")
            .matches("_method a.b _return _endmethod")
            .matches("_method a.b << c _endmethod")
            .matches("_method a.b ^<< c _endmethod")
            .matches("_method a.b() _endmethod")
            .matches("_method a.b() << c _endmethod")
            .matches("_method a.b() ^<< c _endmethod")
            .matches("_method a.b(p1) _endmethod")
            .matches("_method a.b(p1, p2) _endmethod")
            .matches("_method a.b(_optional p1) _endmethod")
            .matches("_method a.b(_gather p1) _endmethod")
            .matches("_method a.b(p1 _optional p2) _endmethod")
            .matches("_method a.b(p1, _optional p2) _endmethod")
            .matches("_method a.b(p1 _gather p2) _endmethod")
            .matches("_method a.b(p1, _gather p2) _endmethod")
            .matches("_method a.b(p1, _optional p2, _gather p3) _endmethod")
            .matches("_method a.b(p1, _optional p2 _gather p3) _endmethod")
            .matches("_method a.b(p1 _optional p2, _gather p3) _endmethod")
            .matches("_method a.b(p1 _optional p2 _gather p3) _endmethod")
            .matches("_method a.b(p1, _optional p2, p3) _endmethod")
            .matches("_method a.b(p1, _optional p2, p3, _gather p4) _endmethod")
            .matches("_method a.b(p1, _optional p2, p3 _gather p4) _endmethod")
            .matches("_method a.b(p1, p2) _endmethod")
            .matches("_method a[p1] _endmethod")
            .matches("_method a[p1, p2] _endmethod")
            .matches("_method a[p1] << b _endmethod")
            .matches("_method a[p1] ^<< b _endmethod")
            .matches("_method a[p1, p2] << b _endmethod")
            .matches("_method a[p1, p2] ^<< b _endmethod")
            .matches("_method a.b a << .b _endmethod")
            .matches("_method a.b a << .b.c _endmethod")
            .matches("_method a.b\n(a, b) << _self.m()\n_endmethod")
            .matches("_method |a|.b _endmethod")
            .matches("_method |a b|.c _endmethod")
            .matches("_iter _method a.b _endmethod")
            .matches("_private _method a.b _endmethod")
            .matches("_abstract _method a.b _endmethod");
        MagikRuleRequiredAssert.assertThat(rule, MagikGrammar.METHOD_DEFINITION_SYNTAX_ERROR)
            .matches("_method a.b _x _endmethod")
            .matches("_method a.b\n_x\n_endmethod");
        MagikRuleRequiredAssert.assertThat(rule, MagikGrammar.PARAMETERS_PAREN_SYNTAX_ERROR)
            .matches("_method a.b(p1 p2) _endmethod")
            .matches("_method a.b(_x) _endmethod");
        MagikRuleRequiredAssert.assertThat(rule, MagikGrammar.PARAMETERS_SQUARE_SYNTAX_ERROR)
            .matches("_method a[_x] _endmethod");
    }

    @Test
    void testVariableDefinition() {
        Assertions.assertThat(g.rule(MagikGrammar.VARIABLE_DEFINITION_STATEMENT))
            .matches("_local a")
            .matches("_local a, b")
            .matches("_local a << 1")
            .matches("_local a << b << 1")
            .matches("_local a << 1, b << 2")
            .matches("_local (a, b) << (1, 2)")
            .matches("_local d << 1, e << 1")
            .matches("_constant _local f << 1")
            .matches("_local _local g << 1")
            .matches("_local _constant _local h << 1")
            .matches("_local (i, j) << (1, 2)")
            .matches("_local (i, _gather j) << (_scatter a)")
            .matches("_local a << b <= 1")
            .matches("_local (a,b,c) << x()")
            .matches("_local a << (b).m")
            .matches("_local a << (b - c).m")
            .matches("_global x")
            .matches("_global _global x")
            .matches("_dynamic y")
            .matches("_dynamic _dynamic y")
            .matches("_import a")
            .matches("_import a, b")
            .matches("_dynamic _import y");
    }

    @Test
    void testMultipleAssignment() {
        Assertions.assertThat(g.rule(MagikGrammar.MULTIPLE_ASSIGNMENT_STATEMENT))
            .matches("(a, b) << (1, 2)")
            .matches("(a, b, c) << (1, 2, 3)")
            .matches("(a, b, c) << x()")
            .matches("(a, .slot) << (1, 2)")
            .matches("(a, object.m) << (1, 2)")
            .matches("(a, r[2]) << (1, 2)")
            .matches("(a, b) << 1")
            .matches("(.slot, a) << (1, 2)")
            .matches("(a, b) << (_scatter {1, 2})")
            .matches("(a, _gather b) << (1, 2)")
            .matches("(a, _gather b) << (1, 2, 3)")
            .matches("(a, _gather b) << (1, _scatter {1, 2})");
    }

    @Test
    void testStatementSyntaxError() {
        MagikRuleForbiddenAssert.assertThat(g.rule(MagikGrammar.STATEMENT), MagikGrammar.STATEMENT_SYNTAX_ERROR)
            .matches("a.b.c");
        // MagikRuleRequiredAssert.assertThat(g.rule(MagikGrammar.STATEMENT), MagikGrammar.STATEMENT_SYNTAX_ERROR)
        //     .matches("a.b.");
    }

    @Test
    void testProtect() {
        MagikRuleForbiddenAssert.assertThat(g.rule(MagikGrammar.PROTECT), MagikGrammar.PROTECT_SYNTAX_ERROR)
            .matches("_protect _protection _endprotect")
            .matches("_protect a() _protection b() _endprotect")
            .matches("_protect _locking _self a() _protection _endprotect");
        MagikRuleRequiredAssert.assertThat(g.rule(MagikGrammar.PROTECT), MagikGrammar.PROTECT_SYNTAX_ERROR)
            .matches("_protect _a _protection _endprotect")
            .matches("_protect _protection _a _endprotect")
            .matches("_protect _a _endprotect");
    }

    @Test
    void testNumber() {
        Assertions.assertThat(g.rule(MagikGrammar.NUMBER))
            .matches("1")
            .matches("10")
            .matches("1.0");
    }

    @Test
    void testString() {
        Assertions.assertThat(g.rule(MagikGrammar.STRING))
            .matches("\"test\"")
            .matches("\'test\'");
    }

    @Test
    void testSymbol() {
        Assertions.assertThat(g.rule(MagikGrammar.SYMBOL))
            .matches(":test")
            .matches(":Test")
            .matches(":test?")
            .matches(":?")
            .matches(":utf8")
            .matches(":ab|()|")
            .matches(":|ab()|")
            .matches(":||")
            .matches(":|a b|")
            .matches(":|ab:cd|")
            .matches(": a");
    }

    @Test
    void testIdentifier() {
        Assertions.assertThat(g.rule(MagikGrammar.IDENTIFIER))
            .matches("test")
            .matches("test_test")
            .matches("!test!")
            .matches("!test?!")
            .matches("ab:test")
            .matches("ab: test")
            .matches("ab :test")
            .matches("ab : test")
            .matches("a_b:test")
            .matches("|a b|")
            .matches("a|ab|b")
            .matches("a|ab|bc|cd|d")
            .matches("a1:abc")
            .matches("\\|a")
            .matches("xyz\\|abcdef")
            .notMatches("_loop");
    }

    @Test
    void testCharacter() {
        Assertions.assertThat(g.rule(MagikGrammar.CHARACTER))
            .matches("%a")
            .matches("%:")
            .matches("%newline")
            .matches("%.");
    }

    @Test
    void testRegexp() {
        Assertions.assertThat(g.rule(MagikGrammar.REGEXP))
            .matches("/a/")
            .matches("/\\n/")
            .matches("/a/i")
            .matches("/a[xyz]b/x");
    }

    @Test
    void testSuper() {
        Assertions.assertThat(g.rule(MagikGrammar.SUPER))
            .matches("_super")
            .matches("_super(sw_component)");
    }

    @Test
    void testClass() {
        Assertions.assertThat(g.rule(MagikGrammar.CLASS))
            .matches("_class |java.lang.Integer|");
    }

    @Test
    void testMagik() {
        Assertions.assertThat(g.rule(MagikGrammar.MAGIK))
            .matches("")
            .matches(";")
            .matches("# comment")
            .matches("write(1)")
            .matches("write(1)\nwrite(2)")
            .matches("write(1) ; write(2)")
            // Syntax errors.
            .matches("_block _endblo")
            .matches("_blocki _endblock");

        MagikRuleForbiddenAssert.assertThat(g.rule(MagikGrammar.MAGIK), MagikGrammar.SYNTAX_ERROR)
            .matches("_block\n _local x << 10  # type: integer\n write(x)\n _endblock\n");

    }

}
