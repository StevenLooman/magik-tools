package nl.ramsolutions.sw.magik.languageserver.documentsymbols;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.Range;
import nl.ramsolutions.sw.magik.analysis.definitions.BinaryOperatorDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.Definition;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionReader;
import nl.ramsolutions.sw.magik.analysis.definitions.EnumerationDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IndexedExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MixinDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.PackageDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.SlottedExemplarDefinition;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/**
 * Document symbol provider.
 */
public class DocumentSymbolProvider {

    /**
     * Set server capabilities.
     * @param capabilities Server capabilities.
     */
    public void setCapabilities(final ServerCapabilities capabilities) {
        capabilities.setDocumentSymbolProvider(true);
    }

    /**
     * Provide {@link DocumentSymbol}s.
     * @param magikFile Magik file to provide symbols for.
     * @return {@link DocumentSymbol}s.
     */
    @SuppressWarnings("deprecation")
    public List<Either<org.eclipse.lsp4j.SymbolInformation, DocumentSymbol>> provideDocumentSymbol(
            final MagikTypedFile magikFile) {
        final DefinitionReader definitionReader = new DefinitionReader();
        final AstNode topNode = magikFile.getTopNode();
        definitionReader.walkAst(topNode);

        // Convert definitions to DocumentSymbols.
        return definitionReader.getDefinitions().stream()
            .map(this::convertDefinition)
            .map(Either::<org.eclipse.lsp4j.SymbolInformation, DocumentSymbol>forRight)
            .toList();
    }

    private DocumentSymbol convertDefinition(final Definition definition) {
        final SymbolKind symbolKind = this.symbolKindForDefinition(definition);
        final DocumentSymbol documentSymbol = new DocumentSymbol(
            definition.getName(),
            symbolKind,
            Lsp4jConversion.rangeToLsp4j(new Range(definition.getNode())),
            Lsp4jConversion.rangeToLsp4j(new Range(definition.getNode())));
        if (definition instanceof final SlottedExemplarDefinition exemplarDefinition) {
            final List<DocumentSymbol> slotSymbols = this.convertedSlotsFromDefinition(exemplarDefinition);
            documentSymbol.setChildren(slotSymbols);
        }
        return documentSymbol;
    }

    private SymbolKind symbolKindForDefinition(final Definition definition) {
        if (definition instanceof PackageDefinition) {
            return SymbolKind.Namespace;
        } else if (definition instanceof BinaryOperatorDefinition) {
            return SymbolKind.Operator;
        } else if (definition instanceof GlobalDefinition) {
            return SymbolKind.Variable;
        } else if (definition instanceof EnumerationDefinition
                || definition instanceof IndexedExemplarDefinition
                || definition instanceof SlottedExemplarDefinition
                || definition instanceof MixinDefinition) {
            return SymbolKind.Class;
        }
        return SymbolKind.Method;
    }

    private List<DocumentSymbol> convertedSlotsFromDefinition(final SlottedExemplarDefinition exemplarDefinition) {
        return exemplarDefinition.getSlots().stream()
            .map(slotDefinition -> new DocumentSymbol(
                slotDefinition.getName(),
                SymbolKind.Field,
                Lsp4jConversion.rangeToLsp4j(new Range(slotDefinition.getNode())),
                Lsp4jConversion.rangeToLsp4j(new Range(slotDefinition.getNode()))))
            .toList();
    }

}
