package nl.ramsolutions.sw.magik.analysis.typing.reasoner;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import java.util.Objects;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ITypeStringDefinition;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
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

      // Resolve TypeString. A global is an alias, so use its aliased type (e.g. its `# type:`
      // doc) instead of the global's own name; otherwise the reference has no resolvable methods.
      final TypeString resolvedTypeStr =
          this.typeResolver.resolve(typeString).stream()
              .map(IdentifierHandler::getDefinitionTypeString)
              .reduce(TypeString::combine)
              .orElse(TypeString.UNDEFINED);

      this.assignAtom(node, resolvedTypeStr);
    } else if (scopeEntry.isType(ScopeEntry.Type.IMPORT)) {
      final ScopeEntry parentScopeEntry = scopeEntry.getImportedEntry();
      if (parentScopeEntry == null) {
        // Could not find parent scope entry. Ignore this identifier.
        return;
      }

      Objects.requireNonNull(parentScopeEntry);
      final AstNode lastNodeType = this.state.getCurrentScopeEntryNode(parentScopeEntry);
      final ExpressionResultString result = this.state.getNodeType(lastNodeType);
      this.assignAtom(node, result);
    } else if (scopeEntry.isType(ScopeEntry.Type.PARAMETER)) {
      // TODO: This does not handle assigning to parameter properly!
      final AstNode parameterNode = scopeEntry.getDefinitionNode();
      final ExpressionResultString result = this.state.getNodeType(parameterNode);
      this.assignAtom(node, result);
    } else {
      final AstNode lastNodeType = this.state.getCurrentScopeEntryNode(scopeEntry);
      if (lastNodeType != null) {
        final ExpressionResultString result = this.state.getNodeType(lastNodeType);
        this.assignAtom(node, result);
      }
    }
  }

  /**
   * Get the effective {@link TypeString} of a resolved definition. A {@link GlobalDefinition} is an
   * alias, so its aliased type is used; any other definition uses its own type.
   *
   * @param definition Resolved definition.
   * @return The type the reference evaluates to.
   */
  private static TypeString getDefinitionTypeString(final ITypeStringDefinition definition) {
    if (definition instanceof GlobalDefinition globalDefinition) {
      return globalDefinition.getAliasedTypeName();
    }

    return definition.getTypeString();
  }

  /**
   * Handle try variable node.
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

    final ExpressionResultString result = new ExpressionResultString(TypeString.SW_CONDITION);
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
    final ExpressionResultString result = new ExpressionResultString(typeStr);
    this.state.setNodeType(node, result);
  }
}
