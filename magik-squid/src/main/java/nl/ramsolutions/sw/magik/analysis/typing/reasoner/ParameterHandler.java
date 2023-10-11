package nl.ramsolutions.sw.magik.analysis.typing.reasoner;

import com.sonar.sslr.api.AstNode;
import java.util.Map;
import java.util.Objects;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.helpers.ParameterNodeHelper;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.CombinedType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResult;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.parser.TypeDocParser;

/**
 * Parameter handler.
 */
class ParameterHandler extends LocalTypeReasonerHandler {

    private static final TypeString SW_UNSET = TypeString.ofIdentifier("unset", "sw");
    private static final TypeString SW_SIMPLE_VECTOR = TypeString.ofIdentifier("simple_vector", "sw");

    /**
     * Constructor.
     * @param magikFile MagikFile
     * @param nodeTypes Node types.
     * @param nodeIterTypes Node iter types.
     * @param currentScopeEntryNodes Current scope entry nodes.
     */
    ParameterHandler(
            final MagikTypedFile magikFile,
            final Map<AstNode, ExpressionResult> nodeTypes,
            final Map<AstNode, ExpressionResult> nodeIterTypes,
            final Map<ScopeEntry, AstNode> currentScopeEntryNodes) {
        super(magikFile, nodeTypes, nodeIterTypes, currentScopeEntryNodes);
    }

    /**
     * Handle parameter.
     * @param node Node.
     */
    void handleParameter(final AstNode node) {
        final AstNode identifierNode = node.getFirstChild(MagikGrammar.IDENTIFIER);
        final String identifier = identifierNode.getTokenValue();

        // Parse method/proc docs and extract parameter type.
        final AstNode definitionNode =
            node.getFirstAncestor(MagikGrammar.METHOD_DEFINITION, MagikGrammar.PROCEDURE_DEFINITION);
        final TypeDocParser docParser = new TypeDocParser(definitionNode);
        final Map<String, TypeString> parameterTypes = docParser.getParameterTypes();
        final TypeString parameterTypeString = parameterTypes.get(identifier);

        final ExpressionResult result;
        final ParameterNodeHelper helper = new ParameterNodeHelper(node);
        if (helper.isGatherParameter()) {
            final AbstractType simpleVectorType = this.typeKeeper.getType(SW_SIMPLE_VECTOR);
            result = new ExpressionResult(simpleVectorType);
        } else if (parameterTypeString != null && !parameterTypeString.isUndefined()) {
            final AbstractType type = this.typeReader.parseTypeString(parameterTypeString);
            if (helper.isOptionalParameter()) {
                final AbstractType unsetType = this.typeKeeper.getType(SW_UNSET);
                final AbstractType optionalType = new CombinedType(type, unsetType);
                result = new ExpressionResult(optionalType);
            } else {
                result = new ExpressionResult(type);
            }
        } else {
            result = ExpressionResult.UNDEFINED;
        }

        this.setNodeType(identifierNode, result);

        final GlobalScope globalScope = this.magikFile.getGlobalScope();
        final Scope scope = globalScope.getScopeForNode(node);
        Objects.requireNonNull(scope);
        final ScopeEntry scopeEntry = scope.getScopeEntry(identifier);
        this.currentScopeEntryNodes.put(scopeEntry, node);
    }

}
