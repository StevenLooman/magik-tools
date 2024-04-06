package nl.ramsolutions.sw.magik.checks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test UnusedVariableCheck. */
class UnusedVariableCheckTest extends MagikCheckTestBase {

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
      })
  void testValid(final String code) {
    final MagikCheck check = new UnusedVariableCheck();
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
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
      })
  void testVariableNotUsed(final String code) {
    final MagikCheck check = new UnusedVariableCheck();
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
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
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(2);
  }

  @Test
  void testParameter() {
    final MagikCheck check = new UnusedVariableCheck(true);
    final String code = """
        _method a.b(p_param1)
        _endmethod
        """;
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }
}
