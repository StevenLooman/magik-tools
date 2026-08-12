package nl.ramsolutions.sw.magik.languageserver.documenthighlight;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.eclipse.lsp4j.DocumentHighlight;
import org.eclipse.lsp4j.DocumentHighlightKind;

/** Provides highlights for a slot access. */
public class SlotHighlightModule implements DocumentHighlightModule {

  @Override
  public Optional<List<DocumentHighlight>> tryHighlights(final DocumentHighlightContext context) {
    final AstNode identifierNode = context.identifierNode();
    final AstNode wantedNode = DocumentHighlightAncestor.nearest(identifierNode);
    if (wantedNode == null || !wantedNode.is(MagikGrammar.SLOT)) {
      return Optional.empty();
    }

    final AstNode topNode = context.magikFile().getTopNode();
    final List<DocumentHighlight> highlights = this.highlightsForSlot(topNode, identifierNode);
    return Optional.of(highlights);
  }

  private List<DocumentHighlight> highlightsForSlot(
      final AstNode topNode, final AstNode identifierNode) {
    final String name = identifierNode.getTokenValue();
    return topNode.getDescendants(MagikGrammar.SLOT).stream()
        .map(node -> node.getFirstChild(MagikGrammar.IDENTIFIER))
        .filter(Objects::nonNull)
        .filter(node -> name.equals(node.getTokenValue()))
        .map(node -> DocumentHighlightUtils.toHighlight(node, DocumentHighlightKind.Text))
        .toList();
  }
}
