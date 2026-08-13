package nl.ramsolutions.sw.magik.languageserver.documenthighlight;

import com.sonar.sslr.api.AstNode;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import org.eclipse.lsp4j.DocumentHighlight;
import org.eclipse.lsp4j.ServerCapabilities;

/**
 * Document highlight provider. Runs a set of {@link DocumentHighlightModule}s and uses the first
 * claimed result.
 */
public class DocumentHighlightProvider {

  private final List<DocumentHighlightModule> modules;

  /** Constructor. */
  public DocumentHighlightProvider() {
    this.modules = this.createModules();
  }

  /**
   * Create the ordered modules. Override to add or remove document highlight modules.
   *
   * <p>The first module is the stage-1 scope lookup, which must run before the stage-2
   * ancestor-based modules so it can preempt them.
   *
   * @return Ordered modules.
   */
  protected List<DocumentHighlightModule> createModules() {
    return List.of(
        new ScopeEntryHighlightModule(),
        new MethodNameHighlightModule(),
        new ExemplarNameHighlightModule(),
        new ConditionNameHighlightModule(),
        new SlotHighlightModule());
  }

  /**
   * Set server capabilities.
   *
   * @param capabilities Server capabilities.
   */
  public void setCapabilities(final ServerCapabilities capabilities) {
    capabilities.setDocumentHighlightProvider(true);
  }

  /**
   * Provide document highlights for the symbol at the given position.
   *
   * @param magikFile Magik file.
   * @param lsp4jPosition Cursor position.
   * @return List of document highlights.
   */
  public List<DocumentHighlight> provideDocumentHighlights(
      final MagikTypedFile magikFile, final org.eclipse.lsp4j.Position lsp4jPosition) {
    final AstNode topNode = magikFile.getTopNode();
    final Position position = Lsp4jConversion.positionFromLsp4j(lsp4jPosition);

    final AstNode identifierNode = AstQuery.nodeAt(topNode, position, MagikGrammar.IDENTIFIER);
    if (identifierNode == null) {
      return Collections.emptyList();
    }

    // Run the modules in order; the first module claiming the context provides the highlights.
    final DocumentHighlightContext context =
        new DocumentHighlightContext(magikFile, identifierNode);
    for (final DocumentHighlightModule module : this.modules) {
      final Optional<List<DocumentHighlight>> result = module.tryHighlights(context);
      if (result.isPresent()) {
        return result.get();
      }
    }

    return Collections.emptyList();
  }
}
