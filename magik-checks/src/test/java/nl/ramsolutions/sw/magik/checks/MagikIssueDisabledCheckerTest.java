package nl.ramsolutions.sw.magik.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.checks.checks.ForbiddenCallCheck;
import org.junit.jupiter.api.Test;

/** Tests for MagikIssueDisabledChecker. */
class MagikIssueDisabledCheckerTest {

  private static final URI DEFAULT_URI = URI.create("tests://unittest");

  @Test
  void testNotDisbled() throws ReflectiveOperationException {
    final String code = "show(1)\n";
    final MagikFile magikFile = new MagikFile(DEFAULT_URI, code);

    final MagikCheckHolder holder =
        new MagikCheckHolder(ForbiddenCallCheck.class, Collections.emptySet(), true);
    final MagikCheck check = holder.createCheck();
    final List<MagikIssue> issues = check.scanFileForIssues(magikFile);
    final MagikIssue issue = issues.get(0);

    final boolean issueDisabled = MagikIssueDisabledChecker.issueDisabled(magikFile, issue);
    assertThat(issueDisabled).isFalse();
  }

  @Test
  void testDisabledStatementInstruction() throws ReflectiveOperationException {
    final String code = "show(1)  # mlint: disable=forbidden-call\n";
    final MagikFile magikFile = new MagikFile(DEFAULT_URI, code);

    final MagikCheckHolder holder =
        new MagikCheckHolder(ForbiddenCallCheck.class, Collections.emptySet(), true);
    final MagikCheck check = holder.createCheck();
    final List<MagikIssue> issues = check.scanFileForIssues(magikFile);
    final MagikIssue issue = issues.get(0);

    final boolean issueDisabled = MagikIssueDisabledChecker.issueDisabled(magikFile, issue);
    assertThat(issueDisabled).isTrue();
  }

  @Test
  void testDisabledScopeInstruction() throws ReflectiveOperationException {
    final String code =
        """
        # mlint: disable=line-length,forbidden-call
        show(1)
        """;
    final MagikFile magikFile = new MagikFile(DEFAULT_URI, code);

    final MagikCheckHolder holder =
        new MagikCheckHolder(ForbiddenCallCheck.class, Collections.emptySet(), true);
    final MagikCheck check = holder.createCheck();
    final List<MagikIssue> issues = check.scanFileForIssues(magikFile);
    final MagikIssue issue = issues.get(0);

    final boolean issueDisabled = MagikIssueDisabledChecker.issueDisabled(magikFile, issue);
    assertThat(issueDisabled).isTrue();
  }

  @Test
  void testDisabledParentScopeInstruction() throws ReflectiveOperationException {
    final String code =
        """
        # mlint: disable=forbidden-call
        _block
          show(1)
        _endblock
        """;
    final MagikFile magikFile = new MagikFile(DEFAULT_URI, code);

    final MagikCheckHolder holder =
        new MagikCheckHolder(ForbiddenCallCheck.class, Collections.emptySet(), true);
    final MagikCheck check = holder.createCheck();
    final List<MagikIssue> issues = check.scanFileForIssues(magikFile);
    final MagikIssue issue = issues.get(0);

    final boolean issueDisabled = MagikIssueDisabledChecker.issueDisabled(magikFile, issue);
    assertThat(issueDisabled).isTrue();
  }
}
