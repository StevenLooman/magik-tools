package nl.ramsolutions.sw.magik.languageserver.documenthighlight;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.eclipse.lsp4j.DocumentHighlight;
import org.eclipse.lsp4j.DocumentHighlightKind;

/**
 * Stage-1 module: resolves the identifier against its enclosing scope. Covers locals, parameters,
 * globals, dynamics. Preempts the stage-2 (ancestor-based) modules.
 */
public class ScopeEntryHighlightModule implements DocumentHighlightModule {

  @Override
  public Optional<List<DocumentHighlight>> tryHighlights(final DocumentHighlightContext context) {
    final AstNode identifierNode = context.identifierNode();
    final AstNode parentNode = identifierNode.getParent();
    final MagikTypedFile magikFile = context.magikFile();
    final GlobalScope globalScope = magikFile.getGlobalScope();
    final Scope scope = globalScope.getScopeForNode(parentNode);
    if (scope == null) {
      return Optional.empty();
    }

    final String tokenValue = identifierNode.getTokenValue();
    final ScopeEntry scopeEntry = scope.getScopeEntry(tokenValue);
    if (scopeEntry == null) {
      return Optional.empty();
    }

    final List<DocumentHighlight> highlights = this.highlightsForScopeEntry(magikFile, scopeEntry);
    return Optional.of(highlights);
  }

  private List<DocumentHighlight> highlightsForScopeEntry(
      final MagikTypedFile magikFile, final ScopeEntry scopeEntry) {
    if (scopeEntry.isType(
        ScopeEntry.Type.DEFINITION,
        ScopeEntry.Type.LOCAL,
        ScopeEntry.Type.IMPORT,
        ScopeEntry.Type.CONSTANT,
        ScopeEntry.Type.PARAMETER)) {
      // Definition = Write, usages = Read.
      final AstNode definitionNode = scopeEntry.getDefinitionNode();
      final AstNode defIdentifier = DocumentHighlightUtils.toIdentifierNode(definitionNode);
      final DocumentHighlight defHighlight =
          defIdentifier != null
              ? DocumentHighlightUtils.toHighlight(defIdentifier, DocumentHighlightKind.Write)
              : null;

      final List<DocumentHighlight> usageHighlights =
          scopeEntry.getUsages().stream()
              .map(DocumentHighlightUtils::toIdentifierNode)
              .filter(Objects::nonNull)
              .map(node -> DocumentHighlightUtils.toHighlight(node, DocumentHighlightKind.Read))
              .toList();

      return Stream.concat(
              defHighlight != null ? Stream.of(defHighlight) : Stream.empty(),
              usageHighlights.stream())
          .toList();
    }

    // GLOBAL / DYNAMIC: find all matching atoms in the file.
    final String name = scopeEntry.getIdentifier();
    return magikFile.getTopNode().getDescendants(MagikGrammar.ATOM).stream()
        .filter(
            node ->
                node.getFirstChild() != null && node.getFirstChild().is(MagikGrammar.IDENTIFIER))
        .map(node -> node.getFirstChild(MagikGrammar.IDENTIFIER))
        .filter(Objects::nonNull)
        .filter(node -> name.equals(node.getTokenValue()))
        .map(node -> DocumentHighlightUtils.toHighlight(node, DocumentHighlightKind.Text))
        .toList();
  }
}
