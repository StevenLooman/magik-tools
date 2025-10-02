package nl.ramsolutions.sw.magik.lint.output;

import java.util.Set;
import nl.ramsolutions.sw.magik.checks.Issue;

/** Reporter interface. */
public interface Reporter {

  /** Report the issue. */
  void reportIssue(Issue magikIssue);

  /** Get the reported severities. */
  Set<String> reportedSeverities();
}
