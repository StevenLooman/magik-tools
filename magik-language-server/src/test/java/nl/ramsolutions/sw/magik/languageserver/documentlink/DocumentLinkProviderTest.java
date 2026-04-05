package nl.ramsolutions.sw.magik.languageserver.documentlink;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.loadlist.LoadListFile;
import org.eclipse.lsp4j.DocumentLink;
import org.junit.jupiter.api.Test;

/** Tests for {@link DocumentLinkProvider}. */
class DocumentLinkProviderTest {

  private List<DocumentLink> provideDocumentLinks(final String source) {
    final LoadListFile loadListFile = new LoadListFile(LoadListFile.DEFAULT_URI, source);
    final DocumentLinkProvider provider = new DocumentLinkProvider();
    return provider.provideDocumentLinks(loadListFile);
  }

  @Test
  void testSimplePathProducesLink() {
    final String source = "source/my_class\n";
    final List<DocumentLink> links = this.provideDocumentLinks(source);
    assertThat(links).hasSize(1);
    assertThat(links.get(0).getTarget()).endsWith("my_class.magik");
  }

  @Test
  void testExistingMagikExtensionNotDoubled() {
    final String source = "source/my_class.magik\n";
    final List<DocumentLink> links = this.provideDocumentLinks(source);
    assertThat(links).hasSize(1);
    assertThat(links.get(0).getTarget()).endsWith("my_class.magik");
    assertThat(links.get(0).getTarget()).doesNotContain("magik.magik");
  }

  @Test
  void testDirectoryEntryProducesNoLink() {
    final String source = "source/\n";
    final List<DocumentLink> links = this.provideDocumentLinks(source);
    assertThat(links).isEmpty();
  }

  @Test
  void testCommentLineProducesNoLink() {
    final String source = "# this is a comment\n";
    final List<DocumentLink> links = this.provideDocumentLinks(source);
    assertThat(links).isEmpty();
  }

  @Test
  void testMultiplePathsProduceMultipleLinks() {
    final String source =
        """
        source/class_a
        source/class_b
        """;
    final List<DocumentLink> links = this.provideDocumentLinks(source);
    assertThat(links).hasSize(2);
  }
}
