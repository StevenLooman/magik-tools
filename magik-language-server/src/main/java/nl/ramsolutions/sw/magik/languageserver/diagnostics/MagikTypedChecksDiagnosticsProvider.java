package nl.ramsolutions.sw.magik.languageserver.diagnostics;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.ConfigurationLocator;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.checks.MagikCheckHolder;
import nl.ramsolutions.sw.magik.checks.MagikCheckMetadata;
import nl.ramsolutions.sw.magik.checks.MagikChecksConfiguration;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import nl.ramsolutions.sw.magik.checks.MagikIssueDisabledChecker;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import nl.ramsolutions.sw.magik.typedchecks.CheckList;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** MagikType diagnostics provider. */
public class MagikTypedChecksDiagnosticsProvider {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(MagikTypedChecksDiagnosticsProvider.class);
  private static final Logger LOGGER_DURATION =
      LoggerFactory.getLogger(MagikTypedChecksDiagnosticsProvider.class.getName() + "Duration");
  private static final Map<String, DiagnosticSeverity> SEVERITY_MAPPING =
      Map.of(
          "Major", DiagnosticSeverity.Error,
          "Minor", DiagnosticSeverity.Warning);

  private final Path overrideConfigurationPath;

  /**
   * Constructor.
   *
   * @param overrideConfigurationPath Path to override configuration.
   */
  public MagikTypedChecksDiagnosticsProvider(final @Nullable Path overrideConfigurationPath) {
    this.overrideConfigurationPath = overrideConfigurationPath;
  }

  /**
   * Get magik-typed related {@link Diagnostic}s.
   *
   * @param magikFile Magik file.
   * @return List with {@link Diagnostic}s.
   * @throws IOException -
   */
  public List<Diagnostic> getDiagnostics(final MagikTypedFile magikFile) throws IOException {
    // Empty cache, as the configuration may have changed without us knowing it.
    ConfigurationLocator.resetCache();

    // Parse the file, determine types, and get issues.
    return this.createChecks(magikFile).stream()
        .flatMap(check -> this.runChecks(check, magikFile).stream())
        .filter(magikIssue -> !MagikIssueDisabledChecker.issueDisabled(magikFile, magikIssue))
        .map(
            issue -> {
              final MagikCheckHolder holder = issue.check().getHolder();
              final Location location = Lsp4jConversion.locationToLsp4j(issue.location());
              final Range range = location.getRange();
              final String message = issue.message();
              final DiagnosticSeverity severity = this.getCheckSeverity(holder);
              final String checkKeyKebabCase = holder.getCheckKeyKebabCase();
              final String diagnosticSource = String.format("mtype (%s)", checkKeyKebabCase);
              return new Diagnostic(range, message, severity, diagnosticSource);
            })
        .toList();
  }

  private List<MagikIssue> runChecks(final MagikTypedCheck check, final MagikTypedFile magikFile) {
    final long start = System.nanoTime();

    final List<MagikIssue> issues = check.scanFileForIssues(magikFile);

    LOGGER_DURATION.trace(
        "Duration: {} check: {}, uri: {}",
        (System.nanoTime() - start) / 1000000000.0,
        check.getClass().getSimpleName(),
        magikFile.getUri());

    return issues;
  }

  private Collection<MagikTypedCheck> createChecks(final MagikTypedFile magikFile)
      throws IOException {
    final URI uri = magikFile.getUri();
    final Path magikFilePath = Path.of(uri);
    final Path configPath =
        this.overrideConfigurationPath != null
            ? overrideConfigurationPath
            : ConfigurationLocator.locateConfiguration(magikFilePath);
    final MagikChecksConfiguration checksConfig =
        configPath != null
            ? new MagikChecksConfiguration(CheckList.getChecks(), configPath)
            : new MagikChecksConfiguration(CheckList.getChecks());
    final List<MagikCheckHolder> holders = checksConfig.getAllChecks();
    return holders.stream()
        .filter(MagikCheckHolder::isEnabled)
        .map(
            holder -> {
              try {
                return (MagikTypedCheck) holder.createCheck();
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
      final MagikCheckMetadata metadata = holder.getMetadata();
      severity = metadata.getDefaultSeverity();
    } catch (final IOException exception) {
      LOGGER.error(exception.getMessage(), exception);
      return DiagnosticSeverity.Error;
    }

    return MagikTypedChecksDiagnosticsProvider.SEVERITY_MAPPING.getOrDefault(
        severity, DiagnosticSeverity.Error);
  }
}
