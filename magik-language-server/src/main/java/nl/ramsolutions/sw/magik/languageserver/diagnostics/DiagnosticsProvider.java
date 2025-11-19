package nl.ramsolutions.sw.magik.languageserver.diagnostics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.OpenedFile;
import nl.ramsolutions.sw.checks.Check;
import nl.ramsolutions.sw.checks.MagikCheckList;
import nl.ramsolutions.sw.checks.MagikTypedCheckList;
import nl.ramsolutions.sw.checks.ModuleDefCheckList;
import nl.ramsolutions.sw.checks.ProductDefCheckList;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.languageserver.MagikLanguageServerSettings;
import nl.ramsolutions.sw.moduledef.ModuleDefFile;
import nl.ramsolutions.sw.productdef.ProductDefFile;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.ServerCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Provides diagnostics. */
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
   * Provides diagnostics for a {@link ProductDefFile}.
   *
   * @param productDefFile Product definition file.
   * @return Diagnostics.
   */
  public List<Diagnostic> provideDiagnostics(final ProductDefFile productDefFile) {
    final List<Diagnostic> diagnostics = new ArrayList<>();

    final List<Diagnostic> diagnosticsChecks =
        DiagnosticsProvider.getDiagnosticsFromFileAndChecks(
            productDefFile, ProductDefCheckList.INSTANCE.getBaseChecks());
    diagnostics.addAll(diagnosticsChecks);

    return diagnostics;
  }

  /**
   * Provides diagnostics for a {@link ModuleDefFile}.
   *
   * @param moduleDefFile Module definition file.
   * @return Diagnostics.
   */
  public List<Diagnostic> provideDiagnostics(final ModuleDefFile moduleDefFile) {
    final List<Diagnostic> diagnostics = new ArrayList<>();

    final List<Diagnostic> diagnosticsChecks =
        DiagnosticsProvider.getDiagnosticsFromFileAndChecks(
            moduleDefFile, ModuleDefCheckList.INSTANCE.getBaseChecks());
    diagnostics.addAll(diagnosticsChecks);

    return diagnostics;
  }

  /**
   * Provides diagnostics for a {@link MagikFile}.
   *
   * @param magikFile Magik file.
   * @return Diagnostics.
   */
  public List<Diagnostic> provideDiagnostics(final MagikTypedFile magikFile) {
    final List<Diagnostic> diagnostics = new ArrayList<>();

    // Magik diagnostics.
    final List<Class<? extends Check>> magikChecks = MagikCheckList.INSTANCE.getBaseChecks();
    final List<Diagnostic> diagnosticsChecks =
        DiagnosticsProvider.getDiagnosticsFromFileAndChecks(magikFile, magikChecks);
    diagnostics.addAll(diagnosticsChecks);

    // Magik typed diagnostics.
    final MagikLanguageServerSettings settings = new MagikLanguageServerSettings(this.properties);
    final Boolean typingEnableChecks = settings.getTypingEnableChecks();
    if (Boolean.TRUE.equals(typingEnableChecks)) {
      final List<Class<? extends Check>> typedChecks = MagikTypedCheckList.INSTANCE.getBaseChecks();
      final List<Diagnostic> diagnosticsTypedChecks =
          DiagnosticsProvider.getDiagnosticsFromFileAndChecks(magikFile, typedChecks);
      diagnostics.addAll(diagnosticsTypedChecks);
    }

    return diagnostics;
  }

  private static List<Diagnostic> getDiagnosticsFromFileAndChecks(
      final OpenedFile openedFile, final List<Class<? extends Check>> checks) {
    final MagikToolsProperties properties = openedFile.getProperties();
    final ChecksDiagnosticsProvider diagnosticsProvider =
        new ChecksDiagnosticsProvider(checks, properties);
    try {
      return diagnosticsProvider.getDiagnostics(openedFile);
    } catch (final IOException exception) {
      LOGGER.error(exception.getMessage(), exception);
    }

    return Collections.emptyList();
  }
}
