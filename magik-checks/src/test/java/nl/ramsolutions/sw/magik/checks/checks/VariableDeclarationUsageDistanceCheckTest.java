package nl.ramsolutions.sw.magik.checks.checks;

import static nl.ramsolutions.sw.magik.checks.checks.MagikCheckAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test {@link VariableDeclarationUsageDistanceCheck}. */
@SuppressWarnings("checkstyle:MagicNumber")
class VariableDeclarationUsageDistanceCheckTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method object.m
          _local a << 1
          do_something()
          do_something()
          write(a)
        _endmethod
        """,
        """
        _method object.m
          _local a << 1
          do_something()
          do_something()
          _if _true
          _then
            write(a)
          _endif
        _endmethod
        """,
      })
  void testValid(final String code) {
    final VariableDeclarationUsageDistanceCheck check = new VariableDeclarationUsageDistanceCheck();
    check.maxDistance = 5; // Defaults to 5, but to be explicit.
    assertThat(check).reportsNoIssues(code);
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
        _endmethod
        """,
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
        _endmethod
        """,
        """
        _method object.m
          _local a << 1
          do_something()
          a[:abc] << :def
          a.do()
        _endmethod
        """,
        """
        _method object.m
          _constant a << 1
          do_something()
          do_something()
          do_something()
          a.method1()
        _endmethod
        """,
      })
  void testValid2(final String code) {
    final VariableDeclarationUsageDistanceCheck check = new VariableDeclarationUsageDistanceCheck();
    check.maxDistance = 2;
    check.ignoreConstants = true; // Defaults to true, but to be explicit.
    assertThat(check).reportsNoIssues(code);
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
        _endmethod
        """,
        """
        _method object.m
          _local a << 1
          do_something()
          do_something()
          _if _true
          _then
            write(a)
          _endif
        _endmethod
        """,
        """
        _method object.m
          _local a << 1
          do_something()
          do_something()
          do_something()
          a.method1()
        _endmethod
        """,
      })
  void testInvalid(final String code) {
    final VariableDeclarationUsageDistanceCheck check = new VariableDeclarationUsageDistanceCheck();
    check.maxDistance = 2;
    assertThat(check).reportsIssueCount(code, 1);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method object.m
          _local success? << _true
          do_something()
          do_something()
          do_something()
          success? _andif<< _false
          write(success?)
        _endmethod
        """,
        """
        _method object.m
          _local count << 5
          do_something()
          do_something()
          do_something()
          count +<< 1
          write(count)
        _endmethod
        """,
      })
  void testValidWithReassignment(final String code) {
    final VariableDeclarationUsageDistanceCheck check = new VariableDeclarationUsageDistanceCheck();
    check.maxDistance = 4;
    assertThat(check).reportsNoIssues(code);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method object.m
          _local success? << _true
          do_something()
          do_something()
          do_something()
          success? << _false
          write(success?)
        _endmethod
        """,
        """
        _method object.m
          _local a << 1
          do_something()
          do_something()
          do_something()
          a << 2
          _if _true
          _then
            write(a)
          _endif
        _endmethod
        """,
        """
        _method object.m
          _local x << 1
          do_something()
          do_something()
          do_something()
          x << 2
          x.do_something()
          x << 3
          x.do_another()
        _endmethod
        """,
      })
  void testInvalidWithReassignment(final String code) {
    final VariableDeclarationUsageDistanceCheck check = new VariableDeclarationUsageDistanceCheck();
    check.maxDistance = 2;
    assertThat(check).reportsIssueCount(code, 1);
  }
}
