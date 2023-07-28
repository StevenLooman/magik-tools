package nl.ramsolutions.sw.magik.languageserver.diagnostics;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.checks.MagikCheckHolder;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import nl.ramsolutions.sw.magik.lint.Configuration;
import nl.ramsolutions.sw.magik.lint.ConfigurationLocator;
import nl.ramsolutions.sw.magik.lint.MagikLint;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MagikLint diagnostics provider.
 */
public class MagikLintDiagnosticsProvider {

    static final Logger LOGGER = LoggerFactory.getLogger(MagikLintDiagnosticsProvider.class);

    private static final Map<String, DiagnosticSeverity> LINT_SEVERITY_MAPPING = Map.of(
            "Critical", DiagnosticSeverity.Error,
            "Major", DiagnosticSeverity.Error,
            "Minor", DiagnosticSeverity.Warning);

    private final Path overrideConfigurationPath;

    /**
     * Constructor. Locates configuration to be used.
     */
    public MagikLintDiagnosticsProvider(final @Nullable Path overrideConfigurationPath) {
        this.overrideConfigurationPath = overrideConfigurationPath;
    }

    /**
     * Get {@link Diagnostic}s using {@link MagikLint}.
     *
     * @param magikFile Magik file.
     * @return List with {@link Diagnostic}s.
     * @throws IOException -
     */
    public List<Diagnostic> getDiagnostics(final MagikFile magikFile) throws IOException {
        final Path searchPath = Path.of(magikFile.getUri()).getParent();
        ConfigurationLocator.resetCache();
        final Path configurationPath = this.overrideConfigurationPath != null
            ? this.overrideConfigurationPath
            : ConfigurationLocator.locateConfiguration(searchPath);
        final Configuration configuration = configurationPath != null
            ? new Configuration(configurationPath)
            : new Configuration();  // Default configuration.
        final MagikLintDiagnosticsReporter reporter = new MagikLintDiagnosticsReporter();
        final MagikLint magikLint = new MagikLint(configuration, reporter);

        // Run linter.
        try {
            magikLint.run(magikFile);
        } catch (ReflectiveOperationException exception) {
            LOGGER.error(exception.getMessage(), exception);
            return Collections.emptyList();
        }

        // Return diagnostics.
        return reporter.getMagikIssues().stream()
            .map(issue -> {
                final MagikCheckHolder holder = issue.check().getHolder();

                final Location location = Lsp4jConversion.locationToLsp4j(issue.location());
                final Range range = location.getRange();
                final String message = issue.message();
                final DiagnosticSeverity severity = this.diagnosticSeverityForMagikLintSeverity(holder);
                final String checkKeyKebabCase = holder.getCheckKeyKebabCase();
                final String diagnosticSource = String.format("magik-lint (%s)", checkKeyKebabCase);
                return new Diagnostic(range, message, severity, diagnosticSource);
            })
            .collect(Collectors.toList());
    }

    private DiagnosticSeverity diagnosticSeverityForMagikLintSeverity(final MagikCheckHolder holder) {
        try {
            final String lintSeverity = holder.getSeverity();
            return LINT_SEVERITY_MAPPING.get(lintSeverity);
        } catch (final FileNotFoundException exception) {
            LOGGER.error(exception.getMessage(), exception);
        }
        return null;
    }

}
