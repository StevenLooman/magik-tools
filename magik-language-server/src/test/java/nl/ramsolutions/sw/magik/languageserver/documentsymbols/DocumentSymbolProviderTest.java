package nl.ramsolutions.sw.magik.languageserver.documentsymbols;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;

/** Test {@link DocumentSymbolProvider}. */
class DocumentSymbolProviderTest {

  private MagikTypedFile createMagikTypedFile(final String code) {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    return new MagikTypedFile(MagikTypedFile.DEFAULT_URI, code, definitionKeeper);
  }

  @Test
  void testSlotsAppearOnlyNestedUnderExemplar() {
    final String code = "def_slotted_exemplar(:a, {{:slot1, _unset}})\n$\n";
    final MagikTypedFile magikFile = this.createMagikTypedFile(code);
    final DocumentSymbolProvider provider = new DocumentSymbolProvider();

    final List<Either<SymbolInformation, DocumentSymbol>> symbols =
        provider.provideDocumentSymbols(magikFile);

    assertThat(symbols)
        .map(Either::getRight)
        .extracting(DocumentSymbol::getName)
        .doesNotContain("slot1");
    assertThat(symbols)
        .map(Either::getRight)
        .filteredOn(symbol -> symbol.getKind() == SymbolKind.Class)
        .flatExtracting(DocumentSymbol::getChildren)
        .extracting(DocumentSymbol::getName)
        .containsExactly("slot1");
  }

  @Test
  void testInheritanceEdgesDoNotAppearAsSymbols() {
    final String code = "def_slotted_exemplar(:a, {}, :parent1)\n$\n";
    final MagikTypedFile magikFile = this.createMagikTypedFile(code);
    final DocumentSymbolProvider provider = new DocumentSymbolProvider();

    final List<Either<SymbolInformation, DocumentSymbol>> symbols =
        provider.provideDocumentSymbols(magikFile);

    assertThat(symbols)
        .map(Either::getRight)
        .extracting(DocumentSymbol::getName)
        .containsExactly("user:a");
  }
}
