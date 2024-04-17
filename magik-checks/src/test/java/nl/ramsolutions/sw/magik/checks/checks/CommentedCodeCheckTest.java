package nl.ramsolutions.sw.magik.checks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;

/** Test CommentedCodeCheck. */
class CommentedCodeCheckTest extends MagikCheckTestBase {

  @Test
  void testNoCommentedCode() {
    final MagikCheck check = new CommentedCodeCheck();
    final String code =
        """
        _method a.b
            _local x << _self.call()
            x +<< 10
            write(x)
            _return x
        _endmethod""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  void testCommentedCode() {
    final MagikCheck check = new CommentedCodeCheck();
    final String code =
        """
        _method a.b
            #_local x << _self.call()
            #x +<< 10
            #write(x)
            #_return x
        _endmethod""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  void testCommentedCodeTwice() {
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

  @Test
  void testCommentedMessage() {
    final MagikCheck check = new CommentedCodeCheck();
    final String code =
        """
        _method a.b
            # This is
            # just a
            # message,
            # no real.
            # code.
        _endmethod""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  void testCommentsAfterCodeIgnored() {
    final MagikCheck check = new CommentedCodeCheck();
    final String code =
        """
        _method a.b
            a # z
            b # y
            c # x
        _endmethod""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  void testIgnoreMethodDoc() {
    final CommentedCodeCheck check = new CommentedCodeCheck();
    check.minLines = 2;
    final String code =
        """
        _method a.b
            ## Interesting
            ## story.
            # x
            print(1)\
        _endmethod""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  void testHeaderAccepted() {
    final String code =
        """
          # Author         : Me
          # Date written   : 01/95
          # Date changed   :
        """;
    final CommentedCodeCheck check = new CommentedCodeCheck();
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  void testBlockWithEmptyLines() {
    final String code =
        """
        #
        # Add/Remove
        #
        """;
    final CommentedCodeCheck check = new CommentedCodeCheck();
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }
}
