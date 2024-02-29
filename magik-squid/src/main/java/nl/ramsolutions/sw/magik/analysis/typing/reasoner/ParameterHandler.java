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
import nl.ramsolutions.sw.magik.analysis.typing.types.MagikType;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.parser.TypeDocParser;

/** Parameter handler. */
class ParameterHandler extends LocalTypeReasonerHandler {

  /**
   * Constructor.
   *
   * @param state Reasoner state.
   */
  ParameterHandler(final LocalTypeReasonerState state) {
    super(state);
  }

  /**
   * Handle parameter.
   *
   * @param node Node.
   */
  void handleParameter(final AstNode node) {
    final AstNode identifierNode = node.getFirstChild(MagikGrammar.IDENTIFIER);

    // Parse method/proc docs and extract parameter type.
    final AstNode definitionNode =
        node.getFirstAncestor(MagikGrammar.METHOD_DEFINITION, MagikGrammar.PROCEDURE_DEFINITION);
    final TypeDocParser docParser = new TypeDocParser(definitionNode);
    final Map<String, TypeString> parameterTypes = docParser.getParameterTypes();
    final String identifier = identifierNode.getTokenValue();
    final TypeString parameterTypeString =
        parameterTypes.getOrDefault(identifier, TypeString.UNDEFINED);

    final ExpressionResult result;
    final ParameterNodeHelper helper = new ParameterNodeHelper(node);
    if (helper.isGatherParameter()) {
      final AbstractType simpleVectorType = this.typeKeeper.getType(TypeString.SW_SIMPLE_VECTOR);
      final TypeString newTypeString =
          TypeString.ofIdentifier(
              TypeString.SW_SIMPLE_VECTOR.getIdentifier(),
              TypeString.SW_SIMPLE_VECTOR.getPakkage(),
              TypeString.ofGenericDefinition("E", parameterTypeString));
      final AbstractType paramType =
          simpleVectorType instanceof MagikType magikType
              ? new MagikType(magikType, newTypeString)
              : simpleVectorType;

      result = new ExpressionResult(paramType);
    } else if (!parameterTypeString.isUndefined()) {
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
    final ScopeEntry scopeEntry = scope.getScopeEntry(identifierNode);
    Objects.requireNonNull(scopeEntry);
    this.state.setCurrentScopeEntryNode(scopeEntry, node);
  }
}
