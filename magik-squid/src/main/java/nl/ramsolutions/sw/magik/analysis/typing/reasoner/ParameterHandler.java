package nl.ramsolutions.sw.magik.analysis.typing.reasoner;

import com.sonar.sslr.api.AstNode;
import java.util.Map;
import java.util.Objects;
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

    private static final TypeString SW_SIMPLE_VECTOR = TypeString.ofIdentifier("simple_vector", "sw");

    /**
     * Constructor.
     * @param state Reasoner state.
     */
    ParameterHandler(final LocalTypeReasonerState state) {
        super(state);
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
                final AbstractType unsetType = this.typeKeeper.getType(TypeString.SW_UNSET);
                final AbstractType optionalType = new CombinedType(type, unsetType);
                result = new ExpressionResult(optionalType);
            } else {
                result = new ExpressionResult(type);
            }
        } else {
            result = ExpressionResult.UNDEFINED;
        }

        this.state.setNodeType(identifierNode, result);

        final GlobalScope globalScope = this.getGlobalScope();
        final Scope scope = globalScope.getScopeForNode(node);
        Objects.requireNonNull(scope);
        final ScopeEntry scopeEntry = scope.getScopeEntry(identifier);
        this.state.setCurrentScopeEntryNode(scopeEntry, node);
    }

}
