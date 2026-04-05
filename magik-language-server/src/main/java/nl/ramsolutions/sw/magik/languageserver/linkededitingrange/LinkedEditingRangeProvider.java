package nl.ramsolutions.sw.magik.languageserver.linkededitingrange;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import org.eclipse.lsp4j.LinkedEditingRanges;
import org.eclipse.lsp4j.ServerCapabilities;

/** Linked editing range provider. */
public class LinkedEditingRangeProvider {

  /**
   * Set server capabilities.
   *
   * @param capabilities Server capabilities.
   */
  public void setCapabilities(final ServerCapabilities capabilities) {
    capabilities.setLinkedEditingRangeProvider(true);
  }

  /**
   * Provide linked editing ranges for a position in the given file.
   *
   * <p>Returns all occurrences of the local variable at the cursor position so the editor can
   * update them in sync.
   *
   * @param magikFile Magik file.
   * @param position Cursor position.
   * @return {@link LinkedEditingRanges}, or {@code null} if not applicable.
   */
  @CheckForNull
  public LinkedEditingRanges provideLinkedEditingRanges(
      final MagikTypedFile magikFile, final org.eclipse.lsp4j.Position position) {
    final AstNode topNode = magikFile.getTopNode();
    final Position magikPosition = Lsp4jConversion.positionFromLsp4j(position);

    final AstNode identifierNode = AstQuery.nodeAt(topNode, magikPosition, MagikGrammar.IDENTIFIER);
    if (identifierNode == null) {
      return null;
    }

    final AstNode parentNode = identifierNode.getParent();
    final ScopeEntry scopeEntry = this.findScopeEntry(magikFile, parentNode);
    if (scopeEntry == null
        || scopeEntry.isType(
            ScopeEntry.Type.GLOBAL, ScopeEntry.Type.DYNAMIC, ScopeEntry.Type.IMPORT)) {
      return null;
    }

    final AstNode definitionNode = scopeEntry.getDefinitionNode();
    final List<org.eclipse.lsp4j.Range> ranges =
        Stream.concat(Stream.of(definitionNode), scopeEntry.getUsages().stream())
            .map(
                node ->
                    node.isNot(MagikGrammar.IDENTIFIER)
                        ? node.getFirstChild(MagikGrammar.IDENTIFIER)
                        : node)
            .filter(Objects::nonNull)
            .map(Range::new)
            .map(Lsp4jConversion::rangeToLsp4j)
            .toList();

    if (ranges.isEmpty()) {
      return null;
    }

    return new LinkedEditingRanges(ranges);
  }

  @CheckForNull
  private ScopeEntry findScopeEntry(final MagikTypedFile magikFile, final AstNode node) {
    final GlobalScope globalScope = magikFile.getGlobalScope();
    final Scope scope = globalScope.getScopeForNode(node);
    if (scope == null) {
      return null;
    }

    return scope.getScopeEntry(node);
  }
}
