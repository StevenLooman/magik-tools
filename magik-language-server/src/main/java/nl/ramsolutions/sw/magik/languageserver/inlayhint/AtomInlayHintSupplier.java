package nl.ramsolutions.sw.magik.languageserver.inlayhint;

import com.sonar.sslr.api.AstNode;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResult;
import nl.ramsolutions.sw.magik.analysis.typing.types.ParameterReferenceType;
import nl.ramsolutions.sw.magik.analysis.typing.types.UndefinedType;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import nl.ramsolutions.sw.magik.languageserver.MagikSettings;
import org.eclipse.lsp4j.InlayHint;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/**
 * Atom {@link InlayHint} provider.
 */
class AtomInlayHintSupplier {

    /**
     * Get atom {@link InlayHint}s.
     * @param magikFile Magik file.
     * @param range Range to get {@link InlayHint}s for.
     * @return {@link InlayHint}s.
     */
    Stream<InlayHint> getAtomInlayHints(final MagikTypedFile magikFile, final Range range) {
        if (!MagikSettings.INSTANCE.getTypingShowAtomInlayHints()) {
            return Stream.empty();
        }

        final AstNode topNode = magikFile.getTopNode();
        return topNode.getDescendants(MagikGrammar.ATOM).stream()
            .filter(node -> Range.fromTree(node).overlapsWith(range))
            .flatMap(node -> this.getInlayHintsForAtoms(magikFile, node));
    }

    private Stream<InlayHint> getInlayHintsForAtoms(final MagikTypedFile magikFile, final AstNode atomNode) {
        final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();
        final ExpressionResult result = reasonerState.getNodeTypeSilent(atomNode);
        if (result == null
            || result == ExpressionResult.UNDEFINED) {
            return Stream.empty();
        }

        final AbstractType result0 = result.get(0, null);
        if (result0 == UndefinedType.INSTANCE
            || result0 instanceof ParameterReferenceType) {
            return Stream.empty();
        }

        final Position position = Position.fromTokenStart(atomNode.getToken());
        final String label = result.getTypeNames(",");
        final InlayHint inlayHint = new InlayHint(
            Lsp4jConversion.positionToLsp4j(position),
            Either.forLeft(label));
        return Stream.of(inlayHint);
    }

}
