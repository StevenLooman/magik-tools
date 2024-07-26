package nl.ramsolutions.sw.magik.checks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;

/** Test VariableDeclarationUsageDistanceCheck. */
@SuppressWarnings("checkstyle:MagicNumber")
class VariableDeclarationUsageDistanceCheckTest extends MagikCheckTestBase {

  @Test
  void testDistanceOk() {
    final VariableDeclarationUsageDistanceCheck check = new VariableDeclarationUsageDistanceCheck();
    check.maxDistance = 5;
    final String code =
        """
        _method object.m
            _local a << 1
            do_something()
            do_something()
            write(a)
        _endmethod""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  void testDistanceNotOk() {
    final VariableDeclarationUsageDistanceCheck check = new VariableDeclarationUsageDistanceCheck();
    check.maxDistance = 2;
    final String code =
        """
        _method object.m
            _local a << 1
            do_something()
            do_something()
            write(a)
            write(a)
        _endmethod""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  void testUpperScopeDistanceOk() {
    final VariableDeclarationUsageDistanceCheck check = new VariableDeclarationUsageDistanceCheck();
    check.maxDistance = 5;
    final String code =
        """
        _method object.m
            _local a << 1
            do_something()
            do_something()
            _if _true
            _then
                write(a)
            _endif
        _endmethod""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  void testUpperScopeDistanceNotOk() {
    final VariableDeclarationUsageDistanceCheck check = new VariableDeclarationUsageDistanceCheck();
    check.maxDistance = 2;
    final String code =
        """
        _method object.m
            _local a << 1
            do_something()
            do_something()
            _if _true
            _then
                write(a)
            _endif
        _endmethod""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  void testVariableUsedInChildScope() {
    final VariableDeclarationUsageDistanceCheck check = new VariableDeclarationUsageDistanceCheck();
    check.maxDistance = 2;
    final String code =
        """
        _method object.m
            a << 1
            _if x
            _then
                a.method1()
            _endif
            _if y
            _then
                a.method2()
            _endif
            >> a
        _endmethod""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  void testDifferentScopesNotChecked() {
    final VariableDeclarationUsageDistanceCheck check = new VariableDeclarationUsageDistanceCheck();
    check.maxDistance = 2;
    final String code =
        """
        _method object.m
            _if _true
            _then
                a << 1
            _endif
            do_something()
            do_something()
            _if _true
            _then
                write(a)
            _endif
        _endmethod""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  void testDistanceOkIndexerMethod() {
    final VariableDeclarationUsageDistanceCheck check = new VariableDeclarationUsageDistanceCheck();
    check.maxDistance = 2;
    final String code =
        """
        _method object.m
            _local a << 1
            do_something()
            a[:abc] << :def
            a.do()
        _endmethod""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  void testDistanceNotOkMethod() {
    final VariableDeclarationUsageDistanceCheck check = new VariableDeclarationUsageDistanceCheck();
    check.maxDistance = 2;
    final String code =
        """
        _method object.m
            _local a << 1
            do_something()
            do_something()
            do_something()
            a.method1()
        _endmethod""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  void testIgnoreConstants() {
    final VariableDeclarationUsageDistanceCheck check = new VariableDeclarationUsageDistanceCheck();
    check.maxDistance = 2;
    check.ignoreConstants = true; // Defaults to try, but to be explicit.
    final String code =
        """
        _method object.m
            _constant a << 1
            do_something()
            do_something()
            do_something()
            a.method1()
        _endmethod""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  void testNotIgnoreConstants() {
    final VariableDeclarationUsageDistanceCheck check = new VariableDeclarationUsageDistanceCheck();
    check.maxDistance = 2;
    check.ignoreConstants = false;
    final String code =
        """
        _method object.m
            _constant a << 1
            do_something()
            do_something()
            do_something()
            a.method1()
        _endmethod""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }
}
