package nl.ramsolutions.sw.magik.languageserver.diagnostics;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.ConfigurationLocator;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.checks.Check;
import nl.ramsolutions.sw.checks.CheckHolder;
import nl.ramsolutions.sw.checks.CheckMetadata;
import nl.ramsolutions.sw.checks.ChecksConfiguration;
import nl.ramsolutions.sw.checks.Issue;
import nl.ramsolutions.sw.checks.IssueDisabledChecker;
import nl.ramsolutions.sw.checks.MagikCheckList;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** MagikLint diagnostics provider. */
public class MagikChecksDiagnosticsProvider {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(MagikChecksDiagnosticsProvider.class);
  private static final Logger LOGGER_DURATION =
      LoggerFactory.getLogger(MagikChecksDiagnosticsProvider.class.getName() + "Duration");
  private static final Map<String, DiagnosticSeverity> SEVERITY_MAPPING =
      Map.of(
          "Major", DiagnosticSeverity.Error,
          "Minor", DiagnosticSeverity.Warning);

  private final MagikToolsProperties properties;

  /**
   * Constructor.
   *
   * @param properties Properties.
   */
  public MagikChecksDiagnosticsProvider(final MagikToolsProperties properties) {
    this.properties = properties;
  }

  /**
   * Get {@link Diagnostic}s.
   *
   * @param magikFile Magik file.
   * @return List with {@link Diagnostic}s.
   * @throws IOException -
   */
  public List<Diagnostic> getDiagnostics(final MagikFile magikFile) throws IOException {
    // Empty cache, as the configuration may have changed without us knowing it.
    ConfigurationLocator.resetCache();

    return this.createChecks(magikFile).stream()
        .flatMap(check -> this.runChecks(check, magikFile).stream())
        .filter(issue -> !IssueDisabledChecker.issueDisabled(magikFile, issue))
        .map(
            issue -> {
              final CheckHolder holder = issue.check().getHolder();
              final Location location = Lsp4jConversion.locationToLsp4j(issue.location());
              final Range range = location.getRange();
              final String message = issue.message();
              final DiagnosticSeverity severity = this.getCheckSeverity(holder);
              final String checkKeyKebabCase = holder.getCheckKeyKebabCase();
              final String diagnosticSource = String.format("mlint (%s)", checkKeyKebabCase);
              return new Diagnostic(range, message, severity, diagnosticSource);
            })
        .toList();
  }

  private List<Issue> runChecks(final Check check, final MagikFile magikFile) {
    final long start = System.nanoTime();

    final List<Issue> issues = check.scanFileForIssues(magikFile);

    if (LOGGER_DURATION.isTraceEnabled()) {
      LOGGER_DURATION.trace(
          "Duration: {} check: {}, uri: {}",
          String.format("%.3f", (System.nanoTime() - start) / 1000000000.0),
          check.getClass().getSimpleName(),
          magikFile.getUri());
    }

    return issues;
  }

  private Collection<Check> createChecks(final MagikFile magikFile) throws IOException {
    final MagikToolsProperties fileProperties = magikFile.getProperties();
    final MagikToolsProperties actualProperties =
        MagikToolsProperties.merge(this.properties, fileProperties);
    final ChecksConfiguration config =
        new ChecksConfiguration(MagikCheckList.getBaseChecks(), actualProperties);
    final List<CheckHolder> holders = config.getAllChecks();
    return holders.stream()
        .filter(CheckHolder::isEnabled)
        .map(
            holder -> {
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

  private DiagnosticSeverity getCheckSeverity(final CheckHolder holder) {
    final String severity;
    try {
      final CheckMetadata metadata = holder.getMetadata();
      severity = metadata.getDefaultSeverity();
    } catch (final IOException exception) {
      LOGGER.error(exception.getMessage(), exception);
      return DiagnosticSeverity.Error;
    }

    return MagikChecksDiagnosticsProvider.SEVERITY_MAPPING.getOrDefault(
        severity, DiagnosticSeverity.Error);
  }
}
