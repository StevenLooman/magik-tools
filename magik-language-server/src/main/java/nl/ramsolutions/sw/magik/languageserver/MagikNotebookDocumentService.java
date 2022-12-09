package nl.ramsolutions.sw.magik.languageserver;

import org.eclipse.lsp4j.DidChangeNotebookDocumentParams;
import org.eclipse.lsp4j.DidCloseNotebookDocumentParams;
import org.eclipse.lsp4j.DidOpenNotebookDocumentParams;
import org.eclipse.lsp4j.DidSaveNotebookDocumentParams;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.services.NotebookDocumentService;

/**
 * Magik NotebookDocumentService.
 */
public class MagikNotebookDocumentService implements NotebookDocumentService {

    @SuppressWarnings("unused")
    private final MagikLanguageServer magikLanguageServer;

    public MagikNotebookDocumentService(final MagikLanguageServer magikLanguageServer) {
        this.magikLanguageServer = magikLanguageServer;
    }

    @Override
    public void didOpen(final DidOpenNotebookDocumentParams params) {
    }

    @Override
    public void didChange(final DidChangeNotebookDocumentParams params) {
    }

    @Override
    public void didSave(final DidSaveNotebookDocumentParams params) {
    }

    @Override
    public void didClose(final DidCloseNotebookDocumentParams params) {
    }

    public void setCapabilities(final ServerCapabilities capabilities) {
    }

}
