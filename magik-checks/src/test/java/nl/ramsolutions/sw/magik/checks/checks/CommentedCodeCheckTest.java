package nl.ramsolutions.sw.magik.checks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test CommentedCodeCheck. */
class CommentedCodeCheckTest extends MagikCheckTestBase {

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
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
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
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  void testInvalid2() {
    final MagikCheck check = new CommentedCodeCheck();
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
        _endmethod""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(2);
  }
}
