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

/** Method/procedure invocation return type {@link InlayHint} module. */
class InvocationTypingInlayHintModule implements InlayHintModule {

  private final MagikToolsProperties properties;

  InvocationTypingInlayHintModule(final MagikToolsProperties properties) {
    this.properties = properties;
  }

  @Override
  public Stream<InlayHint> provideInlayHints(final InlayHintContext context) {
    final MagikLanguageServerSettings settings = new MagikLanguageServerSettings(this.properties);
    if (!settings.getTypingShowTypingInlayHints()) {
      return Stream.empty();
    }

    final MagikTypedFile magikFile = context.file();
    final Range range = context.range();
    final AstNode topNode = magikFile.getTopNode();
    return topNode
        .getDescendants(MagikGrammar.METHOD_INVOCATION, MagikGrammar.PROCEDURE_INVOCATION)
        .stream()
        .filter(node -> Range.fromTree(node).overlapsWith(range))
        .flatMap(node -> this.getInlayHintsForInvocations(magikFile, node));
  }

  private Stream<InlayHint> getInlayHintsForInvocations(
      final MagikTypedFile magikFile, final AstNode invocationNode) {
    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();
    final ExpressionResultString result = reasonerState.getNodeTypeSilent(invocationNode);
    if (result == null || result.stream().anyMatch(TypeString::isUndefined)) {
      return Stream.empty();
    }

    final TypeString typeStr = result.get(0, TypeString.UNDEFINED);
    if (typeStr.isUndefined() || typeStr.isParameterReference() || typeStr.isSlotReference()) {
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
