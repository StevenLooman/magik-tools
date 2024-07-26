package nl.ramsolutions.sw.magik.languageserver.diagnostics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.languageserver.MagikLanguageServerSettings;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.ServerCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Provides diagnostics for Magik files. */
public class DiagnosticsProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticsProvider.class);

  private final MagikToolsProperties properties;

  public DiagnosticsProvider(final MagikToolsProperties properties) {
    this.properties = properties;
  }

  public void setCapabilities(final ServerCapabilities capabilities) {
    // No capabilities to set.
  }

  /**
   * Provides diagnostics for a Magik file.
   *
   * @param magikFile Magik file.
   * @return Diagnostics.
   */
  public List<Diagnostic> provideDiagnostics(final MagikTypedFile magikFile) {
    final List<Diagnostic> diagnostics = new ArrayList<>();

    // Linter diagnostics.
    final List<Diagnostic> diagnosticsLinter =
        DiagnosticsProvider.getDiagnosticsFromLinter(magikFile);
    diagnostics.addAll(diagnosticsLinter);

    // Typing diagnostics.
    final MagikLanguageServerSettings settings = new MagikLanguageServerSettings(this.properties);
    final Boolean typingEnableChecks = settings.getTypingEnableChecks();
    if (Boolean.TRUE.equals(typingEnableChecks)) {
      final List<Diagnostic> diagnosticsTyping =
          DiagnosticsProvider.getDiagnosticsFromTyping(magikFile);
      diagnostics.addAll(diagnosticsTyping);
    }

    return diagnostics;
  }

  private static List<Diagnostic> getDiagnosticsFromLinter(final MagikTypedFile magikFile) {
    final MagikToolsProperties magikFileProperties = magikFile.getProperties();
    final MagikChecksDiagnosticsProvider lintProvider =
        new MagikChecksDiagnosticsProvider(magikFileProperties);
    try {
      return lintProvider.getDiagnostics(magikFile);
    } catch (final IOException exception) {
      LOGGER.error(exception.getMessage(), exception);
    }

    return Collections.emptyList();
  }

  private static List<Diagnostic> getDiagnosticsFromTyping(final MagikTypedFile magikFile) {
    final MagikToolsProperties magikFileProperties = magikFile.getProperties();
    final MagikTypedChecksDiagnosticsProvider typedDiagnosticsProvider =
        new MagikTypedChecksDiagnosticsProvider(magikFileProperties);
    try {
      return typedDiagnosticsProvider.getDiagnostics(magikFile);
    } catch (final IOException exception) {
      LOGGER.error(exception.getMessage(), exception);
    }

    return Collections.emptyList();
  }
}
