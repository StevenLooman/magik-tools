package nl.ramsolutions.sw.magik.languageserver.documenthighlight;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import org.eclipse.lsp4j.DocumentHighlight;
import org.eclipse.lsp4j.DocumentHighlightKind;

/** Shared helpers for document highlight modules. */
final class DocumentHighlightUtils {

  private DocumentHighlightUtils() {}

  @CheckForNull
  static AstNode toIdentifierNode(final AstNode node) {
    if (node == null) {
      return null;
    }

    return node.is(MagikGrammar.IDENTIFIER) ? node : node.getFirstChild(MagikGrammar.IDENTIFIER);
  }

  static DocumentHighlight toHighlight(
      final AstNode identifierNode, final DocumentHighlightKind kind) {
    final org.eclipse.lsp4j.Range range = Lsp4jConversion.rangeToLsp4j(new Range(identifierNode));
    return new DocumentHighlight(range, kind);
  }
}
