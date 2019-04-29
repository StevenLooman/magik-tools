package org.stevenlooman.sw.magik.api;

import org.junit.Test;
import org.sonar.sslr.parser.LexerlessGrammar;
import org.sonar.sslr.tests.Assertions;

public class MagikGrammarTest {

  private final LexerlessGrammar g = MagikGrammar.create();

  @Test
  public void testPragma() {
    Assertions.assertThat(g.rule(MagikGrammar.PRAGMA))
        .matches("_pragma(a=b)")
        .matches("_pragma(a=b,c=d)")
        .matches("_pragma(a={b,c})");
  }

  @Test
  public void testPackage() {
    Assertions.assertThat(g.rule(MagikGrammar.PACKAGE_SPECIFICATION))
        .matches("_package sw")
        .matches("_package user");
  }

  @Test
  public void testHandling() {
    Assertions.assertThat(g.rule(MagikGrammar.HANDLING))
        .matches("_handling _default")
        .matches("_handling a _with _default")
        .matches("_handling a _with x")
        .matches("_handling a, b _with x")
        .matches("_handling a, b _with _default")
    ;
  }

  @Test
  public void testBlock() {
    Assertions.assertThat(g.rule(MagikGrammar.BLOCK))
        .matches("_block _endblock")
        .matches("_block\n_endblock")
        .matches("_BLOCK _ENDBLOCK")
        .matches("_block ; _endblock")
        ;
  }

  @Test
  public void testTryBlock() {
    Assertions.assertThat(g.rule(MagikGrammar.TRY_BLOCK))
        .matches("_try _when error _endtry")
        .matches("_try expr() _when error _endtry")
        .matches("_try _with e _when error _endtry")
        .matches("_try _with e expr() _when error _endtry")
        ;
  }

  @Test
  public void testCatchBlock() {
    Assertions.assertThat(g.rule(MagikGrammar.CATCH_BLOCK))
        .matches("_catch _endcatch")
        .matches("_catch :a\n_endcatch")
        ;
  }

  @Test
  public void testThrow() {
    Assertions.assertThat(g.rule(MagikGrammar.THROW_STATEMENT))
        .matches("_throw :test")
        .matches("_throw @error _with _false");
  }

  @Test
  public void testLockBlock() {
    Assertions.assertThat(g.rule(MagikGrammar.LOCK_BLOCK))
        .matches("_lock a _endlock");
  }

  @Test
  public void testProcedureDeclaration() {
    Assertions.assertThat(g.rule(MagikGrammar.PROC_DEFINITION))
        .matches("_proc() _endproc")
        .matches("_iter _proc() _endproc")
        .matches("_proc(a) _endproc")
        .matches("_proc(a, b) _endproc")
        .matches("_proc @test() _endproc")
        .matches("_proc @|test 123|() _endproc");
  }

  @Test
  public void testLabel() {
    Assertions.assertThat(g.rule(MagikGrammar.LABEL))
        .matches("@label")
        .matches("@ label")
        .matches("@LABEL")
        .matches("@label")
        .matches("@|label 123|");
  }

  @Test
  public void testGlobalRef() {
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
  public void testFor() {
    Assertions.assertThat(g.rule(MagikGrammar.FOR))
        .matches("_for a _over a _loop _endloop")
        .matches("_for a, b _over a _loop _endloop");
  }

  @Test
  public void testWhile() {
    Assertions.assertThat(g.rule(MagikGrammar.WHILE))
        .matches("_while a _loop _endloop")
        .matches("_while a _andif b _loop _endloop");
  }

  @Test
  public void testLoop() {
    Assertions.assertThat(g.rule(MagikGrammar.LOOP))
        .matches("_loop _endloop")
        .matches("_loop expr() _endloop")
        .matches("_loop _finally _endloop")
        .matches("_loop _finally _with total _endloop")
        .matches("_loop _finally _with _gather total _endloop")
        ;
  }

  @Test
  public void testIf() {
    Assertions.assertThat(g.rule(MagikGrammar.IF))
        .matches("_if expr _then _endif")
        .matches("_if expr _then _else _endif")
        .matches("_if expr _then _elif expr _then _else _endif")
        .matches("_if expr _then >> 1 _endif")
    ;
  }

  @Test
  public void testResult() {
    Assertions.assertThat(g.rule(MagikGrammar.EMIT_STATEMENT))
        .matches(">> a")
        .matches(">> (1, 2)")
        .matches(">> (a) _mod b")
    ;
  }

  @Test
  public void testMethodInvocation() {
    Assertions.assertThat(g.rule(MagikGrammar.EXPRESSION))
        .matches("identifier.method")
        .matches("identifier.method()")
        .matches("identifier.method(1)")
        .matches("identifier.method(1, 2)")
        .matches("identifier.method << a")
        .matches("identifier.method() << a")
        .matches("identifier.method(1) << a")
        .matches("identifier.method(1, 2) << a")
        .matches("10.method")
        .matches("1.0.method")
    ;
  }

  @Test
  public void testIndexedInvocation() {
    Assertions.assertThat(g.rule(MagikGrammar.EXPRESSION))
        .matches("identifier[1]")
        .matches("identifier[1,2]")
        .matches("identifier[1] << a")
        .matches("identifier[1,2] << a")
    ;
  }

  @Test
  public void testMethodInvocationIndexedInvocation() {
    Assertions.assertThat(g.rule(MagikGrammar.EXPRESSION))
        .matches("expr.method[p]")
        .matches("expr.method(1)[1]")
        .matches("expr.method(1, 2)[1]")
        .matches("expr.method(1)[1,2]")
        .matches("expr.method(1, 2)[1,2]");
  }

  @Test
  public void testReturnStatement() {
    Assertions.assertThat(g.rule(MagikGrammar.RETURN_STATEMENT))
        .matches("_return")
        .matches("_return a")
        .matches("_return (a, b)")
        .matches("_return (\na, b)")
    ;
  }

  @Test
  public void testExpression() {
    Assertions.assertThat(g.rule(MagikGrammar.EXPRESSION))
        .matches("a()")
        .matches("expr()")
        .matches("a _is b")
        .matches("a _isnt b")
        .matches("a - b")
        .matches("a + b")
        .matches("a << b")
        .matches("a _and<< b")
        .matches("a _andif<< b")
        .matches("a -<< b")
        .matches("a +<< b")
        .matches("a << b + 1")
        ;
  }

  @Test
  public void testMethodDefinition() {
    Assertions.assertThat(g.rule(MagikGrammar.METHOD_DEFINITION))
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
        .matches("_method a[p1] _endmethod")
        .matches("_method a[p1, p2] _endmethod")
        .matches("_method a[p1] << b _endmethod")
        .matches("_method a[p1] ^<< b _endmethod")
        .matches("_method a[p1, p2] << b _endmethod")
        .matches("_method a[p1, p2] ^<< b _endmethod")
        .matches("_method a.b a << .b _endmethod")
        .matches("_method a.b a << .b.c _endmethod")
        .matches("_method a.b(a _optional b) _endmethod")
        .matches("_method a.b\n(a, b) << _self.m()\n_endmethod")
        .matches("_method |a|.b _endmethod")
        .matches("_method |a b|.c _endmethod")
        .matches("_iter _method a.b _endmethod")
        .matches("_private _method a.b _endmethod")
        .matches("_abstract _method a.b _endmethod")
    ;
  }

  @Test
  public void testVariableDeclaration() {
    Assertions.assertThat(g.rule(MagikGrammar.VARIABLE_DECLARATION_STATEMENT))
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
        .matches("_global x")
        .matches("_global _global x")
        .matches("_dynamic y")
        .matches("_dynamic _dynamic y")
        .matches("_import a")
        .matches("_import a, b")
        .matches("_dynamic _import y");
  }

  @Test
  public void testBody() {
    Assertions.assertThat(g.rule(MagikGrammar.BODY))
        .matches("expr()")
        .matches("_handling _default\nexpr()")
        .matches("_continue")
        .matches("_continue @label")
        .matches("_continue _with e()")
        .matches("_continue @label _with e()")
        .matches("_leave")
        .matches("_leave @label")
        .matches("_leave _with e()")
        .matches("_leave @label _with e()")
        ;
  }

  @Test
  public void testNumber() {
    Assertions.assertThat(g.rule(MagikGrammar.NUMBER))
        .matches("1")
        .matches("10")
        .matches("1.0")
        ;
  }

  @Test
  public void testString() {
    Assertions.assertThat(g.rule(MagikGrammar.STRING))
        .matches("\"test\"")
        .matches("\'test\'")
        ;
  }

  @Test
  public void testSymbol() {
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
        ;
  }

  @Test
  public void testIdentifier() {
    Assertions.assertThat(g.rule(MagikGrammar.IDENTIFIER))
        .matches("test")
        .matches("test_test")
        .matches("!test!")
        .matches("!test?!")
        .matches("ab:test")
        .matches("a_b:test")
        .matches("|a b|")
        .matches("a|ab|b")
        .matches("a|ab|bc|cd|d")
        .matches("a1:abc")
        .matches("\\|a")
        .matches("xyz\\|abcdef")
        ;
  }

  @Test
  public void testCharacter() {
    Assertions.assertThat(g.rule(MagikGrammar.CHARACTER))
        .matches("%a")
        .matches("%:")
        .matches("%newline")
        .matches("%.")
        ;
  }

  @Test
  public void testMagik() {
    Assertions.assertThat(g.rule(MagikGrammar.MAGIK))
        .matches("_block _return _endblock")
        .matches("_block ; _endblock")
        .matches("_block _return ; _endblock")
        .matches("_block _endblock ")
        .matches("_block\n_handling _default\n_endblock")
        .matches("(a, b) << (1, 2)")
        .matches("a + b\na + b")
        .matches("a()\n(a, b) << (1, 2)")
        .matches("a.b()\n.c")
        .matches("_block\nremex(:xsd_type)\n_endblock")
        .matches("_local u\n_proc() _import u _endproc")
        .matches("@package:ref")
        .matches(">> {@pkg_a:ref_a}")
        .matches("(a,b,c) << x()")
        .matches("_local (a,b,c) << x()")
        .matches("e[1].call()")
        .matches("_method a.b(p) e[1].call() _endmethod")
        .matches("_if e _then _elif e _then _elif e _then _endif")
        .matches("a +<< 1")
        .matches("a + << 1")
        .matches("_local a << (b).m")
        .matches("_local a << (b - c).m")
        .matches("_loop _continue _with _false _endloop")
        .matches("_loop a << _loopbody(1) _endloop")
        .matches("_for _gather x _over a _loop _endloop")
        .matches("_for x, _gather y _over a _loop _endloop")
        .matches(">> (a - b).m() < c")
        .matches("_method a.a a.b(x _scatter y) _endmethod")
        .matches("a.b() ; c.d()")
        .matches(";")
        .matches("x +^<< 1")
        .matches("_proc() _handling _default _endproc")
        .matches("_package sw")
        .matches("_package sw\n_package user")
    ;
  }

}
