package nl.ramsolutions.sw.magik.typedlint.output;

import java.util.Set;
import nl.ramsolutions.sw.checks.Issue;

/** Reporter interface. */
public interface Reporter {

  /** Report the issue. */
  void reportIssue(Issue issue);

  /** Get the reported severities. */
  Set<String> reportedSeverities();
}
