package nl.ramsolutions.sw.checks.output;

import java.util.Set;
import nl.ramsolutions.sw.checks.Issue;

/** Reporter interface. */
public interface Reporter {

  /** Report the issue. */
  void reportIssue(Issue issue);

  /** Get the reported severities. */
  Set<String> reportedSeverities();

  /** Finish reporting. Called after all issues have been reported. */
  default void finish() {}
}
