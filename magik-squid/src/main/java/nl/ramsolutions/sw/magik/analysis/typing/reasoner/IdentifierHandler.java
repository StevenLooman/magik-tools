package nl.ramsolutions.sw.magik.analysis.typing.reasoner;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import java.util.Objects;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResult;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/** Identifier handler. */
class IdentifierHandler extends LocalTypeReasonerHandler {

  /**
   * Constructor.
   *
   * @param state Reasoner state.
   */
  IdentifierHandler(final LocalTypeReasonerState state) {
    super(state);
  }

  /**
   * Handle identifier.
   *
   * @param node IDENTIFIER node.
   */
  void handleIdentifier(final AstNode node) {
    final AstNode parent = node.getParent();
    if (!parent.is(MagikGrammar.ATOM)) {
      return;
    }

    // Already assigned, perhaps another "plugin" has assigned it.
    final AstNode atomNode = node.getParent();
    if (this.state.hasNodeType(atomNode)) {
      return;
    }

    final GlobalScope globalScope = this.getGlobalScope();
    final Scope scope = globalScope.getScopeForNode(node);
    Objects.requireNonNull(scope);
    final String identifier = node.getTokenValue();
    final ScopeEntry scopeEntry = scope.getScopeEntry(identifier);
    Objects.requireNonNull(scopeEntry);
    if (scopeEntry.isType(ScopeEntry.Type.GLOBAL) || scopeEntry.isType(ScopeEntry.Type.DYNAMIC)) {
      final String currentPackage = this.getCurrentPackage(node);
      final TypeString typeString = TypeString.ofIdentifier(identifier, currentPackage);
      this.assignAtom(node, typeString);
    } else if (scopeEntry.isType(ScopeEntry.Type.IMPORT)) {
      final ScopeEntry parentScopeEntry = scopeEntry.getImportedEntry();
      Objects.requireNonNull(parentScopeEntry);
      final AstNode lastNodeType = this.state.getCurrentScopeEntryNode(parentScopeEntry);
      final ExpressionResult result = this.state.getNodeType(lastNodeType);
      this.assignAtom(node, result);
    } else if (scopeEntry.isType(ScopeEntry.Type.PARAMETER)) {
      // TODO: This does not handle assigning to parameter properly!
      final AstNode parameterNode = scopeEntry.getDefinitionNode();
      final ExpressionResult result = this.state.getNodeType(parameterNode);
      this.assignAtom(node, result);
    } else {
      final AstNode lastNodeType = this.state.getCurrentScopeEntryNode(scopeEntry);
      if (lastNodeType != null) {
        final ExpressionResult result = this.state.getNodeType(lastNodeType);
        this.assignAtom(node, result);
      }
    }
  }

  /**
   * Handle try variable ndoe.
   *
   * @param node TRY_VARIABLE node.
   */
  void handleTryVariable(final AstNode node) {
    final AstNode tryNode = node.getParent();
    final List<AstNode> whenNodes = tryNode.getChildren(MagikGrammar.WHEN);
    for (final AstNode whenNode : whenNodes) {
      final AstNode whenBodyNode = whenNode.getFirstChild(MagikGrammar.BODY);
      final GlobalScope globalScope = this.getGlobalScope();
      final Scope scope = globalScope.getScopeForNode(whenBodyNode);
      Objects.requireNonNull(scope);
      final AstNode identifierNode = node.getFirstChild(MagikGrammar.IDENTIFIER);
      final ScopeEntry scopeEntry = scope.getScopeEntry(identifierNode);
      Objects.requireNonNull(scopeEntry);
      this.state.setCurrentScopeEntryNode(scopeEntry, node);
    }

    final AbstractType conditionType = this.typeKeeper.getType(TypeString.SW_CONDITION);
    final ExpressionResult result = new ExpressionResult(conditionType);
    this.state.setNodeType(node, result);
  }

  /**
   * Handle exemplar name.
   *
   * @param node EXEMPLAR_NAME node.
   */
  void handleExemplarName(final AstNode node) {
    final String exemplarName = node.getTokenValue();
    final String currentPackage = this.getCurrentPackage(node);
    final TypeString typeStr = TypeString.ofIdentifier(exemplarName, currentPackage);
    final AbstractType type = this.typeKeeper.getType(typeStr);
    final ExpressionResult result = new ExpressionResult(type);
    this.state.setNodeType(node, result);
  }
}
