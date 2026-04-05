package nl.ramsolutions.sw.magik.languageserver.documentlink;

import com.sonar.sslr.api.AstNode;
import java.net.URI;
import java.util.List;
import nl.ramsolutions.sw.loadlist.LoadListFile;
import nl.ramsolutions.sw.loadlist.api.LoadListGrammar;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import org.eclipse.lsp4j.DocumentLink;
import org.eclipse.lsp4j.DocumentLinkOptions;
import org.eclipse.lsp4j.ServerCapabilities;

/** Document link provider. */
public class DocumentLinkProvider {

  /**
   * Set server capabilities.
   *
   * @param capabilities Server capabilities.
   */
  public void setCapabilities(final ServerCapabilities capabilities) {
    capabilities.setDocumentLinkProvider(new DocumentLinkOptions());
  }

  /**
   * Provide document links for a load list file.
   *
   * @param loadListFile Load list file.
   * @return List of {@link DocumentLink}s.
   */
  public List<DocumentLink> provideDocumentLinks(final LoadListFile loadListFile) {
    final AstNode topNode = loadListFile.getTopNode();
    final URI loadListUri = loadListFile.getUri();

    return topNode.getDescendants(LoadListGrammar.FILE_PATH).stream()
        .map(filePathNode -> this.documentLinkForFilePathNode(loadListUri, filePathNode))
        .filter(link -> link != null)
        .toList();
  }

  private DocumentLink documentLinkForFilePathNode(
      final URI loadListUri, final AstNode filePathNode) {
    final String filePathValue = filePathNode.getTokenValue().strip();
    if (filePathValue.endsWith("/")) {
      // Directory entry — no link.
      return null;
    }

    final String appendExtension = filePathValue.endsWith(".magik") ? "" : ".magik";
    final URI targetUri = loadListUri.resolve(filePathValue + appendExtension);

    final Range range = new Range(filePathNode);
    final org.eclipse.lsp4j.Range rangeLsp4j = Lsp4jConversion.rangeToLsp4j(range);
    return new DocumentLink(rangeLsp4j, targetUri.toString());
  }
}
