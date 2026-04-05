package nl.ramsolutions.sw.magik.languageserver.diagnostics;

import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.OpenedFile;
import nl.ramsolutions.sw.loadlist.LoadListFile;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.moduledef.ModuleDefFile;
import nl.ramsolutions.sw.productdef.ProductDefFile;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticRegistrationOptions;
import org.eclipse.lsp4j.DocumentDiagnosticReport;
import org.eclipse.lsp4j.RelatedFullDocumentDiagnosticReport;
import org.eclipse.lsp4j.ServerCapabilities;

/** Provides diagnostics via the LSP pull model (textDocument/diagnostic). */
public class PullDiagnosticsProvider {

  private final DiagnosticsProvider diagnosticsProvider;

  public PullDiagnosticsProvider(final DiagnosticsProvider diagnosticsProvider) {
    this.diagnosticsProvider = diagnosticsProvider;
  }

  public void setCapabilities(final ServerCapabilities capabilities) {
    final DiagnosticRegistrationOptions options = new DiagnosticRegistrationOptions();
    options.setInterFileDependencies(false);
    options.setWorkspaceDiagnostics(false);
    capabilities.setDiagnosticProvider(options);
  }

  /**
   * Provide a full diagnostic report for the given opened file.
   *
   * @param openedFile The opened file, or {@code null} if the file is not open.
   * @return A {@link DocumentDiagnosticReport} containing all diagnostics for the file.
   */
  public DocumentDiagnosticReport provideDiagnostics(final OpenedFile openedFile) {
    final List<Diagnostic> diagnostics;
    if (openedFile instanceof ProductDefFile productDefFile) {
      diagnostics = this.diagnosticsProvider.provideDiagnostics(productDefFile);
    } else if (openedFile instanceof ModuleDefFile moduleDefFile) {
      diagnostics = this.diagnosticsProvider.provideDiagnostics(moduleDefFile);
    } else if (openedFile instanceof LoadListFile loadListFile) {
      diagnostics = this.diagnosticsProvider.provideDiagnostics(loadListFile);
    } else if (openedFile instanceof MagikTypedFile magikFile) {
      diagnostics = this.diagnosticsProvider.provideDiagnostics(magikFile);
    } else {
      diagnostics = Collections.emptyList();
    }

    return new DocumentDiagnosticReport(new RelatedFullDocumentDiagnosticReport(diagnostics));
  }
}
