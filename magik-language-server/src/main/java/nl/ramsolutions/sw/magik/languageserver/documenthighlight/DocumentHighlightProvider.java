package nl.ramsolutions.sw.magik.languageserver.documenthighlight;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import org.eclipse.lsp4j.DocumentHighlight;
import org.eclipse.lsp4j.DocumentHighlightKind;
import org.eclipse.lsp4j.ServerCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Document highlight provider. */
public class DocumentHighlightProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(DocumentHighlightProvider.class);

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

    LOGGER.trace("identifierNode: {}", identifierNode);

    // First: scope-based lookup handles local vars, parameters, globals, etc.
    final AstNode parentNode = identifierNode.getParent();
    final GlobalScope globalScope = magikFile.getGlobalScope();
    final Scope scope = globalScope.getScopeForNode(parentNode);
    if (scope != null) {
      final String tokenValue = identifierNode.getTokenValue();
      final ScopeEntry scopeEntry = scope.getScopeEntry(tokenValue);
      if (scopeEntry != null) {
        return this.highlightsForScopeEntry(magikFile, scopeEntry);
      }
    }

    // Fallback: ancestor-based lookup for method names, exemplars, slots, conditions.
    final AstNode wantedNode =
        identifierNode.getFirstAncestor(
            MagikGrammar.METHOD_NAME,
            MagikGrammar.EXEMPLAR_NAME,
            MagikGrammar.CONDITION_NAME,
            MagikGrammar.SLOT);

    if (wantedNode == null) {
      return Collections.emptyList();
    }

    LOGGER.trace("Wanted node: {}", wantedNode);

    if (wantedNode.is(MagikGrammar.METHOD_NAME)) {
      return this.highlightsForMethodName(topNode, wantedNode);
    } else if (wantedNode.is(MagikGrammar.EXEMPLAR_NAME)) {
      return this.highlightsForExemplarName(topNode, identifierNode);
    } else if (wantedNode.is(MagikGrammar.CONDITION_NAME)) {
      return this.highlightsForConditionName(topNode, identifierNode);
    } else if (wantedNode.is(MagikGrammar.SLOT)) {
      return this.highlightsForSlot(topNode, identifierNode);
    }

    return Collections.emptyList();
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
      final AstNode defIdentifier = this.toIdentifierNode(definitionNode);
      final DocumentHighlight defHighlight =
          defIdentifier != null ? toHighlight(defIdentifier, DocumentHighlightKind.Write) : null;

      final List<DocumentHighlight> usageHighlights =
          scopeEntry.getUsages().stream()
              .map(this::toIdentifierNode)
              .filter(Objects::nonNull)
              .map(node -> toHighlight(node, DocumentHighlightKind.Read))
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
        .map(node -> toHighlight(node, DocumentHighlightKind.Text))
        .toList();
  }

  private List<DocumentHighlight> highlightsForMethodName(
      final AstNode topNode, final AstNode methodNameNode) {
    // METHOD_NAME can be a child of either METHOD_DEFINITION or METHOD_INVOCATION.
    final AstNode parentNode = methodNameNode.getParent();
    final String methodIdentifier;
    if (parentNode.is(MagikGrammar.METHOD_DEFINITION)) {
      final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(parentNode);
      methodIdentifier = helper.getMethodNameIdentifier();
    } else if (parentNode.is(MagikGrammar.METHOD_INVOCATION)) {
      final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(parentNode);
      methodIdentifier = helper.getMethodNameIdentifier();
    } else {
      return Collections.emptyList();
    }
    return this.highlightsForMethodIdentifier(topNode, methodIdentifier);
  }

  /**
   * Find all METHOD_NAME nodes in the file (both in definitions and invocations) whose identifier
   * matches and return highlights over the identifier token.
   */
  private List<DocumentHighlight> highlightsForMethodIdentifier(
      final AstNode topNode, final String methodIdentifier) {
    return topNode.getDescendants(MagikGrammar.METHOD_NAME).stream()
        .filter(
            methodNameNode -> {
              final AstNode idNode = methodNameNode.getFirstChild(MagikGrammar.IDENTIFIER);
              return idNode != null && methodIdentifier.equals(idNode.getTokenValue());
            })
        .map(methodNameNode -> methodNameNode.getFirstChild(MagikGrammar.IDENTIFIER))
        .filter(Objects::nonNull)
        .map(idNode -> toHighlight(idNode, DocumentHighlightKind.Text))
        .toList();
  }

  private List<DocumentHighlight> highlightsForExemplarName(
      final AstNode topNode, final AstNode identifierNode) {
    final String name = identifierNode.getTokenValue();
    return topNode.getDescendants(MagikGrammar.EXEMPLAR_NAME).stream()
        .map(node -> node.getFirstChild(MagikGrammar.IDENTIFIER))
        .filter(Objects::nonNull)
        .filter(node -> name.equals(node.getTokenValue()))
        .map(node -> toHighlight(node, DocumentHighlightKind.Text))
        .toList();
  }

  private List<DocumentHighlight> highlightsForConditionName(
      final AstNode topNode, final AstNode identifierNode) {
    final String name = identifierNode.getTokenValue();
    return topNode.getDescendants(MagikGrammar.CONDITION_NAME).stream()
        .map(node -> node.getFirstChild(MagikGrammar.IDENTIFIER))
        .filter(Objects::nonNull)
        .filter(node -> name.equals(node.getTokenValue()))
        .map(node -> toHighlight(node, DocumentHighlightKind.Text))
        .toList();
  }

  private List<DocumentHighlight> highlightsForSlot(
      final AstNode topNode, final AstNode identifierNode) {
    final String name = identifierNode.getTokenValue();
    return topNode.getDescendants(MagikGrammar.SLOT).stream()
        .map(node -> node.getFirstChild(MagikGrammar.IDENTIFIER))
        .filter(Objects::nonNull)
        .filter(node -> name.equals(node.getTokenValue()))
        .map(node -> toHighlight(node, DocumentHighlightKind.Text))
        .toList();
  }

  @CheckForNull
  private AstNode toIdentifierNode(final AstNode node) {
    if (node == null) {
      return null;
    }

    return node.is(MagikGrammar.IDENTIFIER) ? node : node.getFirstChild(MagikGrammar.IDENTIFIER);
  }

  private static DocumentHighlight toHighlight(
      final AstNode identifierNode, final DocumentHighlightKind kind) {
    final org.eclipse.lsp4j.Range range = Lsp4jConversion.rangeToLsp4j(new Range(identifierNode));
    return new DocumentHighlight(range, kind);
  }
}
