package nl.ramsolutions.sw.magik.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.checks.checks.ForbiddenCallCheck;
import org.junit.jupiter.api.Test;

/** Tests for MagikIssueDisabledChecker. */
class MagikIssueDisabledCheckerTest {

  @Test
  void testNotDisabled() throws ReflectiveOperationException {
    assertIssueDisabled("show(1)\n", false);
  }

  @Test
  void testDisabledStatementInstruction() throws ReflectiveOperationException {
    assertIssueDisabled("show(1) # mlint: disable=forbidden-call\n", true);
  }

  @Test
  void testDisabledAllStatementInstruction() throws ReflectiveOperationException {
    assertIssueDisabled("show(1) # mlint: disable=all\n", true);
  }

  @Test
  void testDisabledScopeInstruction() throws ReflectiveOperationException {
    assertIssueDisabled(
        """
        # mlint: disable=line-length,forbidden-call
        show(1)
        """,
        true);
  }

  @Test
  void testDisabledParentScopeInstruction() throws ReflectiveOperationException {
    assertIssueDisabled(
        """
        # mlint: disable=forbidden-call
        _block
          show(1)
        _endblock
        """,
        true);
  }

  @Test
  void testDisabledAllScopeInstruction() throws ReflectiveOperationException {
    assertIssueDisabled(
        """
        # mlint: disable=all
        show(1)
        """,
        true);
  }

  @Test
  void testDisabledAllParentScopeInstruction() throws ReflectiveOperationException {
    assertIssueDisabled(
        """
        # mlint: disable=all
        _block
          show(1)
        _endblock
        """,
        true);
  }

  private void assertIssueDisabled(String code, boolean expectedDisabled)
      throws ReflectiveOperationException {
    final MagikFile magikFile = new MagikFile(MagikFile.DEFAULT_URI, code);
    final MagikCheckHolder holder =
        new MagikCheckHolder(ForbiddenCallCheck.class, Collections.emptySet(), true);
    final MagikCheck check = holder.createCheck();
    final List<MagikIssue> issues = check.scanFileForIssues(magikFile);
    final MagikIssue issue = issues.get(0);
    final boolean issueDisabled = MagikIssueDisabledChecker.issueDisabled(magikFile, issue);
    assertThat(issueDisabled).isEqualTo(expectedDisabled);
  }
}
