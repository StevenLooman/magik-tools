package nl.ramsolutions.sw.checks.magik;

import static nl.ramsolutions.sw.checks.magik.MagikCheckAssert.assertThat;

import nl.ramsolutions.sw.checks.MagikCheck;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test {@link MagicNumberCheck}. */
class MagicNumberCheckTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.b()
          _local x << 0
        _endmethod
        """,
        """
        _method a.b()
          _local x << 1
        _endmethod
        """,
        """
        _method a.b()
          _local x << -1
        _endmethod
        """,
        """
        _method a.b()
          _local x << 2
        _endmethod
        """,
        """
        _method a.b()
          _constant max_count << 100
        _endmethod
        """,
        """
        a.define_shared_constant(:max_count, 100, :public)
        """,
      })
  void testValid(final String code) {
    final MagikCheck check = new MagicNumberCheck();
    assertThat(check).reportsNoIssues(code);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.b()
          _local x << 42
        _endmethod
        """,
        """
        _method a.b()
          _local x << -42
        _endmethod
        """,
        """
        _method a.b()
          _local x << 3.14
        _endmethod
        """,
      })
  void testInvalid(final String code) {
    final MagikCheck check = new MagicNumberCheck();
    assertThat(check).reportsIssueCount(code, 1);
  }

  @Test
  void testCustomIgnoreNumbers() {
    final MagicNumberCheck check = new MagicNumberCheck();
    check.ignoreNumbers = "42, 100";

    final String code =
        """
        _method a.b()
          _local x << 42
          _local y << 100
          _local z << 55
        _endmethod
        """;
    assertThat(check).reportsIssueCount(code, 1);
  }

  @Test
  void testCustomIgnoreNumbersRadix() {
    final MagicNumberCheck check = new MagicNumberCheck();
    check.ignoreNumbers = "100";

    final String code =
        """
        _method a.b()
          _local x << 2r1100100
          _local y << 55
        _endmethod
        """;
    assertThat(check).reportsIssueCount(code, 1);
  }

  @Test
  void testIgnoreConstantDeclarationsDisabled() {
    final MagicNumberCheck check = new MagicNumberCheck();
    check.ignoreConstantDeclarations = false;

    final String code =
        """
        _method a.b()
          _constant max_size << 100
        _endmethod
        """;
    assertThat(check).reportsIssueCount(code, 1);
  }

  @Test
  void testIgnoreFieldDeclarations() {
    final MagicNumberCheck check = new MagicNumberCheck();
    check.ignoreSlotDefaultValues = true;

    final String code =
        """
        def_slotted_exemplar(:my_exemplar, {:count, 100})
        """;
    assertThat(check).reportsNoIssues(code);
  }
}
