package nl.ramsolutions.sw.magik.analysis.typing.reasoner;

import com.sonar.sslr.api.AstNode;
import java.util.Map;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.CombinedType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResult;
import nl.ramsolutions.sw.magik.analysis.typing.types.UndefinedType;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Super handler.
 */
class SuperHandler extends LocalTypeReasonerHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SuperHandler.class);

    /**
     * Constructor.
     * @param magikFile MagikFile
     * @param nodeTypes Node types.
     * @param nodeIterTypes Node iter types.
     * @param currentScopeEntryNodes Current scope entry nodes.
     */
    SuperHandler(
            final MagikTypedFile magikFile,
            final Map<AstNode, ExpressionResult> nodeTypes,
            final Map<AstNode, ExpressionResult> nodeIterTypes,
            final Map<ScopeEntry, AstNode> currentScopeEntryNodes) {
        super(magikFile, nodeTypes, nodeIterTypes, currentScopeEntryNodes);
    }

    /**
     * Handle super.
     * @param node
     */
    void handleSuper(final AstNode node) {
        // Determine which type we are.
        final AbstractType methodOwnerType = this.getMethodOwnerType(node);
        if (methodOwnerType == UndefinedType.INSTANCE) {
            LOGGER.debug("Unknown type for node: {}", node);
            return;
        }

        // Find specified super, if given.
        final AstNode identifierNode = node.getFirstChild(MagikGrammar.IDENTIFIER);
        final String identifier = identifierNode != null
            ? identifierNode.getTokenValue()
            : null;
        final AbstractType superType;
        if (identifier != null) {
            superType = methodOwnerType.getParents().stream()
                .filter(parentType -> {
                    final String fullTypeName = parentType.getFullName();
                    final String typeName = fullTypeName.split(":")[1];
                    return identifier.equals(typeName);
                })
                .findAny()
                .orElse(null);
        } else {
            superType = methodOwnerType.getParents().stream()
                .reduce(CombinedType::combine)
                .orElse(null);
        }

        if (superType == null) {
            return;
        }

        final ExpressionResult result = new ExpressionResult(superType);
        this.assignAtom(node, result);
    }

}
