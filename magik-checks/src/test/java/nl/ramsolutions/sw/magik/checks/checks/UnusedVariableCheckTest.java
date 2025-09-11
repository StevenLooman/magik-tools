package nl.ramsolutions.sw.magik.checks.checks;

import static nl.ramsolutions.sw.magik.checks.checks.MagikCheckAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import nl.ramsolutions.sw.magik.checks.MagikCheck;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test {@link UnusedVariableCheck}. */
class UnusedVariableCheckTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.b
          _local a
          print(a)
        _endmethod
        """,
        """
        _method a.b
          _local (a, b) << (_scatter {1,2})
          write(b)
        _endmethod
        """,
        """
        _method a.b
          (a, b) << (_scatter {1,2})
          write(b)
        _endmethod
        """,
        """
        _method a.b
          a << 10
          print(a)
        _endmethod
        """,
        """
        _method a.b
          _for a, b _over x.fast_keys_and_elements()
          _loop
            write(b)
          _endloop
        _endmethod
        """,
        """
        _method a.b
          _dynamic !notify_database_data_changes?! << _false
        _endmethod
        """,
        """
        _method a.b
          _dynamic !notify_database_data_changes?!
          !notify_database_data_changes?! << _false
        _endmethod
        """,
        """
        _method a.b(param1)
          param1.a << 10
        _endmethod
        """,
        """
        _method a.b(p_param1, _optional p_param2)
          write(p_param1)
          write(p_param2)
        _endmethod
        """,
        """
        _method a.b(p_param1, _gather p_param2)
          write(p_param1)
          write(p_param2)
        _endmethod
        """,
        """
        _method a.b
          _local l_me << _self
          _proc()
            _import l_me
            print(l_me)
          _endproc()
        _endmethod
        """,
        """
        _method a.b
          _local l_me << _self
          _proc()
            _import l_me
            l_me << 10
          _endproc()
        _endmethod
        """,
        """
        _method a.b()
          l_Set << 10
          print(l_set)
        _endmethod
        """,
        """
        _method a.b()
          _if !current_grs! _isnt _unset
          _then
            write(1)
          _endif
        _endmethod
        """,
        """
        _try _with cond
          _self.do_something()
        _when file_does_not_exist
          write(cond.report_contents_string)
        _when error
          write(:something_failed)
        _endtry
        """,
        """
        _try _with cond
          _self.do_something()
        _when file_does_not_exist
          write(:something_failed)
        _when error
          write(cond.report_contents_string)
        _endtry
        """,
        """
        _try _with cond
          _self.do_something()
        _when file_does_not_exist
          write(cond.report_contents_string)
        _when error
          write(cond.report_contents_string)
        _endtry
        """,
        """
        _global x << 10
        """,
        """
        x << 10
        """,
        """
        _method a.b
          sw:x << 10
        _endmethod
        """,
      })
  void testValid(final String code) {
    final MagikCheck check = new UnusedVariableCheck();
    assertThat(check).reportsNoIssues(code);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.b
          _local a
        _endmethod
        """,
        """
        _method a.b
          a << 10
        _endmethod
        """,
        """
        _method a.b
          _for a _over x.fast_elements()
          _loop
          _endloop
        _endmethod
        """,
        """
        _method a.b
          _dynamic !notify_database_data_changes?!
        _endmethod
        """,
        """
        _method a.b
          _global !notify_database_data_changes?!
        _endmethod
        """,
        """
        _try _with cond
          _self.do_something()
        _when file_does_not_exist
          write(:missing_file)
        _when error
          write(:something_failed)
        _endtry
        """,
        """
        _method a.b
          _local l_me << _self
          _proc()
            _import l_me
          _endproc()
        _endmethod
        """,
      })
  void testVariableNotUsed(final String code) {
    final MagikCheck check = new UnusedVariableCheck();
    assertThat(check).reportsIssueCount(code, 1);
  }

  @Test
  void testVariableNotUsedMultiAssignment() {
    final MagikCheck check = new UnusedVariableCheck();
    final String code =
        """
        _method a.b
          _local (a, b) << (_scatter {1,2})
        _endmethod
        """;
    assertThat(check).reportsIssueCount(code, 2);
  }

  @Test
  void testParameter() {
    final UnusedVariableCheck check = new UnusedVariableCheck();
    check.checkParameters = true;
    final String code =
        """
        _method a.b(p_param1)
        _endmethod
        """;
    assertThat(check).reportsIssueCount(code, 1);
  }

  @Test
  void testParameterInAbstractMethod() {
    final UnusedVariableCheck check = new UnusedVariableCheck();
    check.checkParameters = true;
    final String code =
        """
        _abstract _method a.b(p_param1)
        _endmethod
        """;
    assertThat(check).reportsNoIssues(code);
  }
}
