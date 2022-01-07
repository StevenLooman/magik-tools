package nl.ramsolutions.sw.magik.languageserver.diagnostics;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import nl.ramsolutions.sw.magik.lint.output.Reporter;

/**
 * MagikLint diagnostics reporter.
 */
public class MagikLintDiagnosticsReporter implements Reporter {

    private final Set<MagikIssue> magikIssues = new HashSet<>();

    public Set<MagikIssue> getMagikIssues() {
        return Collections.unmodifiableSet(this.magikIssues);
    }

    public void clearInfractions() {
        this.magikIssues.clear();
    }

    @Override
    public void reportIssue(final MagikIssue magikIssue) {
        this.magikIssues.add(magikIssue);
    }

    @Override
    public Set<String> reportedSeverities() {
        return Collections.emptySet(); // Not used.
    }

}
