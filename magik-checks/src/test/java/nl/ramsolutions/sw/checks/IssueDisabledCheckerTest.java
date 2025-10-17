package nl.ramsolutions.sw.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.checks.magik.ForbiddenCallCheck;
import nl.ramsolutions.sw.magik.MagikFile;
import org.junit.jupiter.api.Test;

/** Tests for {@link IssueDisabledChecker}. */
class IssueDisabledCheckerTest {

  @Test
  void testNotDisabled() throws ReflectiveOperationException {
    this.assertIssueDisabled("show(1)\n", false);
  }

  @Test
  void testDisabledStatementInstruction() throws ReflectiveOperationException {
    this.assertIssueDisabled("show(1) # mlint: disable=forbidden-call\n", true);
  }

  @Test
  void testDisabledAllStatementInstruction() throws ReflectiveOperationException {
    this.assertIssueDisabled("show(1) # mlint: disable=all\n", true);
  }

  @Test
  void testDisabledScopeInstruction() throws ReflectiveOperationException {
    this.assertIssueDisabled(
        """
        # mlint: disable=line-length,forbidden-call
        show(1)
        """,
        true);
  }

  @Test
  void testDisabledParentScopeInstruction() throws ReflectiveOperationException {
    this.assertIssueDisabled(
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
    this.assertIssueDisabled(
        """
        # mlint: disable=all
        show(1)
        """,
        true);
  }

  @Test
  void testDisabledAllParentScopeInstruction() throws ReflectiveOperationException {
    this.assertIssueDisabled(
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
    final CheckHolder holder =
        new CheckHolder(ForbiddenCallCheck.class, Collections.emptySet(), true);
    final Check check = holder.createCheck();
    final List<Issue> issues = check.scanFileForIssues(magikFile);
    final Issue issue = issues.get(0);
    final boolean issueDisabled = IssueDisabledChecker.issueDisabled(magikFile, issue);
    assertThat(issueDisabled).isEqualTo(expectedDisabled);
  }
}
