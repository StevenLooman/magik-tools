package nl.ramsolutions.sw.magik.languageserver.diagnostics;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import nl.ramsolutions.sw.magik.typedchecks.CheckList;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MagikType diagnostics provider.
 */
public class MagikTypeDiagnosticsProvider {

    static final Logger LOGGER = LoggerFactory.getLogger(MagikTypeDiagnosticsProvider.class);

    /**
     * Get {@link Diagnostic}s for typing errors..
     * @param magikFile Magik file.
     * @return List with {@link Diagnostic}s.
     * @throws IOException -
     */
    public List<Diagnostic> getDiagnostics(final MagikTypedFile magikFile) throws IOException {
        // Parse the file, determine types, and get issues.
        final Set<MagikTypedCheck> checks = this.createTypedChecks();

        return checks.stream()
            .flatMap(check -> check.scanFileForIssues(magikFile).stream())
            .map(issue -> {
                final Location location = Lsp4jConversion.locationToLsp4j(issue.location());
                final Range range = location.getRange();
                final String message = issue.message();
                final DiagnosticSeverity severity = DiagnosticSeverity.Information;
                final String diagnosticSource = "mtype";
                return new Diagnostic(range, message, severity, diagnosticSource);
            })
            .toList();
    }

    private Set<MagikTypedCheck> createTypedChecks() {
        return CheckList.getChecks().stream()
            .map(checkClass -> {
                try {
                    return (MagikTypedCheck) checkClass.getDeclaredConstructor().newInstance();
                } catch (final ReflectiveOperationException exception) {
                    LOGGER.error(exception.getMessage(), exception);
                }

                return null;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

}
