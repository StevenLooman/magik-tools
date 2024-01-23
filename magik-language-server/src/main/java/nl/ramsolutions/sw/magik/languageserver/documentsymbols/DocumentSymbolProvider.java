package nl.ramsolutions.sw.magik.languageserver.documentsymbols;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.analysis.definitions.BinaryOperatorDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.Definition;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.PackageDefinition;
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
    public List<Either<org.eclipse.lsp4j.SymbolInformation, DocumentSymbol>> provideDocumentSymbol(
            final MagikTypedFile magikFile) {
        // Convert definitions to DocumentSymbols.
        return magikFile.getDefinitions().stream()
            .map(this::convertDefinition)
            .map(Either::<org.eclipse.lsp4j.SymbolInformation, DocumentSymbol>forRight)
            .collect(Collectors.toList());
    }

    private DocumentSymbol convertDefinition(final Definition definition) {
        final SymbolKind symbolKind = this.symbolKindForDefinition(definition);
        final AstNode definitionNode = definition.getNode();
        Objects.requireNonNull(definitionNode);
        final DocumentSymbol documentSymbol = new DocumentSymbol(
            definition.getName(),
            symbolKind,
            Lsp4jConversion.rangeToLsp4j(new Range(definitionNode)),
            Lsp4jConversion.rangeToLsp4j(new Range(definitionNode)));
        if (definition instanceof ExemplarDefinition) {
            final ExemplarDefinition exemplarDefinition = (ExemplarDefinition) definition;
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
        } else if (definition instanceof ExemplarDefinition) {
            return SymbolKind.Class;
        }
        return SymbolKind.Method;
    }

    private List<DocumentSymbol> convertedSlotsFromDefinition(final ExemplarDefinition exemplarDefinition) {
        return exemplarDefinition.getSlots().stream()
            .map(slotDefinition -> new DocumentSymbol(
                slotDefinition.getName(),
                SymbolKind.Field,
                Lsp4jConversion.rangeToLsp4j(new Range(slotDefinition.getNode())),
                Lsp4jConversion.rangeToLsp4j(new Range(slotDefinition.getNode()))))
            .collect(Collectors.toList());
    }

}
