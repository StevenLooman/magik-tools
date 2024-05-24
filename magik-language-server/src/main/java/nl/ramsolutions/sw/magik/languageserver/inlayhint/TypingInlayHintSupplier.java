package nl.ramsolutions.sw.magik.languageserver.inlayhint;

import com.sonar.sslr.api.AstNode;
import java.util.stream.Stream;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import nl.ramsolutions.sw.magik.languageserver.MagikLanguageServerSettings;
import org.eclipse.lsp4j.InlayHint;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/** Typing {@link InlayHint} provider. */
class TypingInlayHintSupplier {

  final MagikToolsProperties properties;

  TypingInlayHintSupplier(final MagikToolsProperties properties) {
    this.properties = properties;
  }

  /**
   * Get atom {@link InlayHint}s.
   *
   * @param magikFile Magik file.
   * @param range Range to get {@link InlayHint}s for.
   * @return {@link InlayHint}s.
   */
  Stream<InlayHint> getTypingInlayHints(final MagikTypedFile magikFile, final Range range) {
    final MagikLanguageServerSettings settings = new MagikLanguageServerSettings(this.properties);
    if (!settings.getTypingShowTypingInlayHints()) {
      return Stream.empty();
    }

    final AstNode topNode = magikFile.getTopNode();
    return Stream.concat(
        topNode.getDescendants(MagikGrammar.ATOM).stream()
            .filter(node -> Range.fromTree(node).overlapsWith(range))
            .flatMap(node -> this.getInlayHintsForAtoms(magikFile, node)),
        topNode
            .getDescendants(MagikGrammar.METHOD_INVOCATION, MagikGrammar.PROCEDURE_INVOCATION)
            .stream()
            .filter(node -> Range.fromTree(node).overlapsWith(range))
            .flatMap(node -> this.getInlayHintsForInvocations(magikFile, node)));
    // TODO: Unary operators
    // TODO: Binary operators
  }

  private Stream<InlayHint> getInlayHintsForAtoms(
      final MagikTypedFile magikFile, final AstNode atomNode) {
    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();
    final ExpressionResultString result = reasonerState.getNodeTypeSilent(atomNode);
    if (result == null || result.stream().anyMatch(TypeString::isUndefined)) {
      return Stream.empty();
    }

    final TypeString typeStr = result.get(0, TypeString.UNDEFINED);
    if (typeStr.isUndefined() || typeStr.isParameterReference()) {
      return Stream.empty();
    }

    final Position position = Position.fromTokenStart(atomNode.getToken());
    final String label = result.getTypeNames(",");
    final InlayHint inlayHint =
        new InlayHint(Lsp4jConversion.positionToLsp4j(position), Either.forLeft(label));
    return Stream.of(inlayHint);
  }

  private Stream<InlayHint> getInlayHintsForInvocations(
      final MagikTypedFile magikFile, final AstNode invocationNode) {
    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();
    final ExpressionResultString result = reasonerState.getNodeTypeSilent(invocationNode);
    if (result == null || result.stream().anyMatch(TypeString::isUndefined)) {
      return Stream.empty();
    }

    final TypeString typeStr = result.get(0, TypeString.UNDEFINED);
    if (typeStr.isUndefined() || typeStr.isParameterReference()) {
      return Stream.empty();
    }

    final AstNode lastChildNode = invocationNode.getLastChild();
    final AstNode lastTokenNode;
    if (lastChildNode.is(MagikGrammar.ARGUMENTS)) {
      lastTokenNode = lastChildNode.getLastChild();
    } else {
      lastTokenNode = lastChildNode;
    }
    final Position position = Position.fromTokenEnd(lastTokenNode.getToken());
    final String label = "->" + result.getTypeNames(",");
    final InlayHint inlayHint =
        new InlayHint(Lsp4jConversion.positionToLsp4j(position), Either.forLeft(label));
    return Stream.of(inlayHint);
  }
}
