package nl.ramsolutions.sw.magik.languageserver.diagnostics;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import nl.ramsolutions.sw.ConfigurationLocator;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikCheckHolder;
import nl.ramsolutions.sw.magik.checks.MagikChecksConfiguration;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MagikLint diagnostics provider.
 */
public class MagikChecksDiagnosticsProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(MagikChecksDiagnosticsProvider.class);
    private static final Map<String, DiagnosticSeverity> SEVERITY_MAPPING = Map.of(
        "Major", DiagnosticSeverity.Error,
        "Minor", DiagnosticSeverity.Warning);

    private final Path overrideConfigurationPath;

    /**
     * Constructor.
     * @param overrideConfigurationPath Path to override configuration.
     */
    public MagikChecksDiagnosticsProvider(final @Nullable Path overrideConfigurationPath) {
        this.overrideConfigurationPath = overrideConfigurationPath;
    }

    /**
     * Get {@link Diagnostic}s.
     * @param magikFile Magik file.
     * @return List with {@link Diagnostic}s.
     * @throws IOException -
     */
    public List<Diagnostic> getDiagnostics(final MagikFile magikFile) throws IOException {
        // Empty cache, as the configuration may have changed without us knowing it.
        ConfigurationLocator.resetCache();

        return this.createChecks(magikFile).stream()
            .flatMap(check -> check.scanFileForIssues(magikFile).stream())
            .map(issue -> {
                final MagikCheckHolder holder = issue.check().getHolder();
                final Location location = Lsp4jConversion.locationToLsp4j(issue.location());
                final Range range = location.getRange();
                final String message = issue.message();
                final DiagnosticSeverity severity = this.getCheckSeverity(holder);
                final String checkKeyKebabCase = holder.getCheckKeyKebabCase();
                final String diagnosticSource = String.format("mtype (%s)", checkKeyKebabCase);
                return new Diagnostic(range, message, severity, diagnosticSource);
            })
            .collect(Collectors.toList());
    }

    private Collection<MagikCheck> createChecks(final MagikFile magikFile) throws IOException {
        final URI uri = magikFile.getUri();
        final Path magikFilePath = Path.of(uri);
        final Path configPath = this.overrideConfigurationPath != null
            ? overrideConfigurationPath
            : ConfigurationLocator.locateConfiguration(magikFilePath);
        final MagikChecksConfiguration checksConfig = configPath != null
            ? new MagikChecksConfiguration(configPath)
            : new MagikChecksConfiguration();
        final List<MagikCheckHolder> holders = checksConfig.getAllChecks();
        return holders.stream()
            .filter(holder -> !holder.getCheckClass().isInstance(MagikTypedCheck.class))
            .filter(MagikCheckHolder::isEnabled)
            .map(holder -> {
                try {
                    return holder.createCheck();
                } catch (final ReflectiveOperationException exception) {
                    LOGGER.error(exception.getMessage(), exception);
                }

                return null;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    private DiagnosticSeverity getCheckSeverity(final MagikCheckHolder holder) {
        final String severity;
        try {
            severity = holder.getSeverity();
        } catch (final IOException exception) {
            LOGGER.error(exception.getMessage(), exception);
            return DiagnosticSeverity.Error;
        }

        return MagikChecksDiagnosticsProvider.SEVERITY_MAPPING.getOrDefault(severity, DiagnosticSeverity.Error);
    }

}
