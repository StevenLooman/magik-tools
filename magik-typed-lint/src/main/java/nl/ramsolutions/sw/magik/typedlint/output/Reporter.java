package nl.ramsolutions.sw.magik.typedlint.output;

import java.util.Set;
import nl.ramsolutions.sw.magik.checks.MagikIssue;

/** Reporter interface. */
public interface Reporter {

  /** Report the issue. */
  void reportIssue(MagikIssue magikIssue);

  /** Get the reported severities. */
  Set<String> reportedSeverities();
}
