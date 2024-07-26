package nl.ramsolutions.sw.magik.checks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test VariableDeclarationUsageDistanceCheck. */
@SuppressWarnings("checkstyle:MagicNumber")
class VariableDeclarationUsageDistanceCheckTest extends MagikCheckTestBase {

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
          _method object.m
              _local a << 1
              do_something()
              do_something()
              write(a)
          _endmethod""",
        """
          _method object.m
              _local a << 1
              do_something()
              do_something()
              _if _true
              _then
                  write(a)
              _endif
          _endmethod""",
      })
  void testValid(final String code) {
    final VariableDeclarationUsageDistanceCheck check = new VariableDeclarationUsageDistanceCheck();
    check.maxDistance = 5;
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
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
        _endmethod""",
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
        _endmethod""",
        """
        _method object.m
            _local a << 1
            do_something()
            a[:abc] << :def
            a.do()
        _endmethod""",
        """
        _method object.m
            _constant a << 1
            do_something()
            do_something()
            do_something()
            a.method1()
        _endmethod""",
      })
  void testValid2(final String code) {
    final VariableDeclarationUsageDistanceCheck check = new VariableDeclarationUsageDistanceCheck();
    check.maxDistance = 2;
    check.ignoreConstants = true; // Defaults to true, but to be explicit.
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
          _method object.m
              _local a << 1
              do_something()
              do_something()
              write(a)
              write(a)
          _endmethod""",
        """
          _method object.m
              _local a << 1
              do_something()
              do_something()
              _if _true
              _then
                  write(a)
              _endif
          _endmethod""",
        """
        _method object.m
            _local a << 1
            do_something()
            do_something()
            do_something()
            a.method1()
        _endmethod""",
      })
  void testInvalid(final String code) {
    final VariableDeclarationUsageDistanceCheck check = new VariableDeclarationUsageDistanceCheck();
    check.maxDistance = 2;
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }
}
