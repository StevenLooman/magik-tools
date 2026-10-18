package nl.ramsolutions.sw.magik.languageserver.diagnostics;

import static org.assertj.core.api.Assertions.assertThat;

import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.message.MessageFile;
import nl.ramsolutions.sw.translation.TranslationFile;
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
  void testProvidesFullReportForMessageFile() {
    final String code =
        """
        #% text_encoding = utf8
        :id_1 Text 1
        :2id Text 2
        """;
    final MessageFile messageFile = new MessageFile(MessageFile.DEFAULT_URI, code);

    final PullDiagnosticsProvider provider = this.createProvider();
    final DocumentDiagnosticReport report = provider.provideDiagnostics(messageFile);

    assertThat(report.isLeft()).isTrue();
    assertThat(report.getLeft().getItems())
        .extracting(diagnostic -> diagnostic.getMessage().getLeft())
        .containsExactly("Invalid syntax.");
  }

  @Test
  void testProvidesFullReportForTranslationFile() {
    final String code =
        """
        #% text_encoding = utf8
        external_name\tpipe\tunset\ten_gb\tPipe\tWater pipe
        external_name   pipe    unset   nl_nl   Pipe    Waterleiding
        """;
    final TranslationFile translationFile = new TranslationFile(TranslationFile.DEFAULT_URI, code);

    final PullDiagnosticsProvider provider = this.createProvider();
    final DocumentDiagnosticReport report = provider.provideDiagnostics(translationFile);

    assertThat(report.isLeft()).isTrue();
    assertThat(report.getLeft().getItems())
        .extracting(diagnostic -> diagnostic.getMessage().getLeft())
        .containsExactly("Invalid syntax.");
  }

  @Test
  void testProvidesFullReportForNullFile() {
    final PullDiagnosticsProvider provider = this.createProvider();
    final DocumentDiagnosticReport report = provider.provideDiagnostics(null);

    assertThat(report.isLeft()).isTrue();
    assertThat(report.getLeft().getItems()).isEmpty();
  }
}
