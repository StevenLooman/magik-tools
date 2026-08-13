package nl.ramsolutions.sw.magik.languageserver.documenthighlight;

import com.sonar.sslr.api.AstNode;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.eclipse.lsp4j.DocumentHighlight;
import org.eclipse.lsp4j.DocumentHighlightKind;

/** Provides highlights for a method name, in both its definition and its invocations. */
public class MethodNameHighlightModule implements DocumentHighlightModule {

  @Override
  public Optional<List<DocumentHighlight>> tryHighlights(final DocumentHighlightContext context) {
    final AstNode identifierNode = context.identifierNode();
    final AstNode methodNameNode = DocumentHighlightAncestor.nearest(identifierNode);
    if (methodNameNode == null || !methodNameNode.is(MagikGrammar.METHOD_NAME)) {
      return Optional.empty();
    }

    final AstNode topNode = context.magikFile().getTopNode();
    final List<DocumentHighlight> highlights =
        this.highlightsForMethodName(topNode, methodNameNode);
    return Optional.of(highlights);
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
        .map(idNode -> DocumentHighlightUtils.toHighlight(idNode, DocumentHighlightKind.Text))
        .toList();
  }
}
