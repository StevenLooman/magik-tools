package nl.ramsolutions.sw.checks.magik;

import static nl.ramsolutions.sw.checks.magik.MagikCheckAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import nl.ramsolutions.sw.checks.MagikCheck;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test {@link CommentedCodeCheck}. */
class CommentedCodeCheckTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.b
          _local x << _self.call()
          x +<< 10
          write(x)
          _return x
        _endmethod
        """,
        """
        _method a.b
          # This is
          # just a
          # message,
          # no real.
          # code.
        _endmethod
        """,
        """
        _method a.b
          a # z
          b # y
          c # x
        _endmethod
        """,
        """
        _method a.b
          ## print(1)
          ## print(2)
          ## print(3)
          ## print(4)
          print(1)
        _endmethod
        """,
        """
        # Author         : Me
        # Date written   : 01/95
        # Date changed   :
        """,
        """
        #
        # Add/Remove
        #
        """,
      })
  void testValid(final String code) {
    final MagikCheck check = new CommentedCodeCheck();
    assertThat(check).reportsNoIssues(code);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.b
          #_local x << _self.call()
          #x +<< 10
          #write(x)
          #_return x
        _endmethod
        """,
        """
        #_method a.b
        #_local x << _self.call()
        #x +<< 10
        #write(x)
        #_return x
        #_endmethod
        """,
        // Include the empty line in the commented code.
        """
        #_method a.b
        #_local x << _self.call()
        #x +<< 10
        #x *<< 2
        #
        #write(x)
        #x -<< 5
        #_return x
        #_endmethod
        """,
        // This will mark the indented commented body.
        """
        #_method a.b
          #_local x << _self.call()
          #x +<< 10
          #write(x)
          #_return x
        #_endmethod
        """,
        """
        #_method a.b
          #_local x << _self.call()
          #x +<< 10
          #x *<< 2
          #
          #write(x)
          #x -<< 5
          #_return x
        #_endmethod
        """,
      })
  void testInvalid(final String code) {
    final MagikCheck check = new CommentedCodeCheck();
    assertThat(check).reportsIssueCount(code, 1);
  }

  @Test
  void testInvalid2() {
    final String code =
        """
        _method a.b
          #_local x << _self.call()
          #x +<< 10
          #write(x)
          #_return x
          _return 10
          #_local x << _self.call()
          #x +<< 10
          #write(x)
          #_return x
        _endmethod
        """;
    final MagikCheck check = new CommentedCodeCheck();
    assertThat(check).reportsIssueCount(code, 2);
  }
}
