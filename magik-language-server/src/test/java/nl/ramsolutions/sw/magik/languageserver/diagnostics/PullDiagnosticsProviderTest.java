package nl.ramsolutions.sw.magik.languageserver.diagnostics;

import static org.assertj.core.api.Assertions.assertThat;

import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import org.eclipse.lsp4j.DocumentDiagnosticReport;
import org.junit.jupiter.api.Test;

/** Test PullDiagnosticsProvider. */
class PullDiagnosticsProviderTest {

  private PullDiagnosticsProvider createProvider() {
    final DiagnosticsProvider diagnosticsProvider =
        new DiagnosticsProvider(MagikToolsProperties.DEFAULT_PROPERTIES);
    return new PullDiagnosticsProvider(diagnosticsProvider);
  }

  @Test
  void testProvidesFullReportForMagikFile() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final String code =
        """
        _package user
        _method a.b()
        _endmethod
        """;
    final MagikTypedFile magikFile =
        new MagikTypedFile(MagikTypedFile.DEFAULT_URI, code, definitionKeeper);

    final PullDiagnosticsProvider provider = this.createProvider();
    final DocumentDiagnosticReport report = provider.provideDiagnostics(magikFile);

    // Always returns a full (left) report, never an unchanged (right) report.
    assertThat(report.isLeft()).isTrue();
    assertThat(report.getLeft().getItems()).isNotNull();
  }

  @Test
  void testProvidesFullReportForNullFile() {
    final PullDiagnosticsProvider provider = this.createProvider();
    final DocumentDiagnosticReport report = provider.provideDiagnostics(null);

    assertThat(report.isLeft()).isTrue();
    assertThat(report.getLeft().getItems()).isEmpty();
  }
}
