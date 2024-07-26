package nl.ramsolutions.sw.magik.analysis.scope;

import com.sonar.sslr.api.AstNode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.MagikVisitor;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/** Scope builder visitor. */
public class ScopeBuilderVisitor extends MagikVisitor {

  /** Global scope. */
  private GlobalScope globalScope;

  /** Current scope. */
  private Scope currentScope;

  /** Scope index for quick searching. */
  private final Map<AstNode, Scope> scopeIndex = new HashMap<>();

  /**
   * Get the {@link GlobalScope}.
   *
   * @return Global scope
   */
  public GlobalScope getGlobalScope() {
    return this.globalScope;
  }

  /**
   * Create the {@link GlobalScope} for the given node. To be used when not fully analyzing a
   * file/tree, but only a specific part, like a METHOD_DEFINITION.
   *
   * @param node Any {@link AstNode}, the top of the tree will be derived automatically.
   */
  public void createGlobalScope(final AstNode node) {
    final AstNode magikNode =
        node.is(MagikGrammar.MAGIK) ? node : node.getFirstAncestor(MagikGrammar.MAGIK);
    this.walkPreMagik(magikNode);
  }

  @Override
  protected void walkPreMagik(final AstNode node) {
    this.globalScope = new GlobalScope(this.scopeIndex, node);
    this.currentScope = this.globalScope;

    this.scopeIndex.put(node, this.globalScope);
  }

  @Override
  protected void walkPreBody(final AstNode node) {
    // Push new scope.
    final AstNode parentNode = node.getParent();
    if (parentNode.is(MagikGrammar.METHOD_DEFINITION, MagikGrammar.PROCEDURE_DEFINITION)) {
      this.walkPreBodyMethodProcDefinition(node, parentNode);
    } else if (parentNode.is(MagikGrammar.WHEN)) {
      this.walkPreBodyWhen(node, parentNode);
    } else if (parentNode.is(MagikGrammar.LOOP)) {
      this.walkPreBodyLoop(node);
    } else {
      this.walkPreBodyRegular(node);
    }

    this.scopeIndex.put(node, this.currentScope);
  }

  private void walkPreBodyRegular(final AstNode node) {
    // regular scope
    this.currentScope = new BodyScope(this.currentScope, node);
  }

  private void walkPreBodyLoop(final AstNode node) {
    this.currentScope = new BodyScope(this.currentScope, node);

    // add for-items to scope
    final AstNode forNode =
        AstQuery.getParentFromChain(node, MagikGrammar.LOOP, MagikGrammar.OVER, MagikGrammar.FOR);
    if (forNode != null) {
      final List<AstNode> identifierNodes =
          AstQuery.getChildrenFromChain(
              forNode,
              MagikGrammar.FOR_VARIABLES,
              MagikGrammar.IDENTIFIERS_WITH_GATHER,
              MagikGrammar.IDENTIFIER);
      for (final AstNode identifierNode : identifierNodes) {
        final String identifier = identifierNode.getTokenValue();
        this.currentScope.addDeclaration(ScopeEntry.Type.LOCAL, identifier, identifierNode, null);
      }
    }
  }

  private void walkPreBodyWhen(final AstNode node, final AstNode parentNode) {
    this.currentScope = new BodyScope(currentScope, node);

    // add _with items to scope
    final AstNode tryNode = parentNode.getParent();
    final AstNode tryVariableNode = tryNode.getFirstChild(MagikGrammar.TRY_VARIABLE);
    if (tryVariableNode != null) {
      final AstNode identifierNode = tryVariableNode.getFirstChild(MagikGrammar.IDENTIFIER);
      final String identifier = identifierNode.getTokenValue();
      this.currentScope.addDeclaration(ScopeEntry.Type.LOCAL, identifier, identifierNode, null);

      // Don't add identifierNode to scope index,
      // as this identifier can have multiple scopes (multiple _when).
    }
  }

  private void walkPreBodyMethodProcDefinition(final AstNode node, final AstNode parentNode) {
    this.currentScope = new ProcedureScope(this.currentScope, node);

    // Add all parameters to scope.
    parentNode.getChildren(MagikGrammar.PARAMETERS, MagikGrammar.ASSIGNMENT_PARAMETER).stream()
        .flatMap(paramsNode -> paramsNode.getChildren(MagikGrammar.PARAMETER).stream())
        .forEach(
            parameterNode -> {
              final AstNode identifierNode = parameterNode.getFirstChild(MagikGrammar.IDENTIFIER);
              final String identifier = identifierNode.getTokenValue();
              this.currentScope.addDeclaration(
                  ScopeEntry.Type.PARAMETER, identifier, identifierNode, null);

              this.scopeIndex.put(identifierNode, this.currentScope);
            });
  }

  @SuppressWarnings("java:S3776")
  @Override
  protected void walkPreVariableDefinitionStatement(final AstNode node) {
    final String type =
        node.getFirstChild(MagikGrammar.VARIABLE_DEFINITION_MODIFIER)
            .getTokenValue()
            .toUpperCase()
            .substring(1);
    final ScopeEntry.Type scopeEntryType = ScopeEntry.Type.valueOf(type);

    Stream.of(
            // Definitions from VARIABLE_DEFINITION_MULTI.
            node.getChildren(MagikGrammar.VARIABLE_DEFINITION_MULTI).stream()
                .map(
                    varDefMultiNode ->
                        varDefMultiNode.getFirstChild(MagikGrammar.IDENTIFIERS_WITH_GATHER))
                .flatMap(
                    identifiersNode ->
                        identifiersNode.getChildren(MagikGrammar.IDENTIFIER).stream()),
            // Defintions from VARIABLE_DEFINITION.
            node.getChildren(MagikGrammar.VARIABLE_DEFINITION).stream()
                .map(varDefNode -> varDefNode.getFirstChild(MagikGrammar.IDENTIFIER)))
        .flatMap(stream -> stream)
        // Work it!
        .filter(
            identifierNode -> {
              // Don't overwrite entries.
              final String identifier = identifierNode.getTokenValue();
              return this.currentScope.getLocalScopeEntry(identifier) == null;
            })
        .forEach(
            identifierNode -> {
              final String identifier = identifierNode.getTokenValue();

              // Figure parent entry.
              ScopeEntry parentEntry = null;
              if (scopeEntryType == ScopeEntry.Type.IMPORT) {
                AstNode procScopeNode =
                    identifierNode.getFirstAncestor(
                        MagikGrammar.METHOD_DEFINITION, MagikGrammar.PROCEDURE_DEFINITION);
                while (procScopeNode != null) {
                  final AstNode parentScopeNode = procScopeNode.getFirstAncestor(MagikGrammar.BODY);
                  final Scope parentScope =
                      parentScopeNode != null
                          ? this.scopeIndex.get(parentScopeNode)
                          : this.globalScope;
                  parentEntry = parentScope.getScopeEntry(identifier);
                  if (parentEntry != null) {
                    break;
                  }

                  procScopeNode =
                      procScopeNode.getFirstAncestor(
                          MagikGrammar.METHOD_DEFINITION, MagikGrammar.PROCEDURE_DEFINITION);
                }
              }

              if (parentEntry != null
                  && !parentEntry.isType(ScopeEntry.Type.LOCAL)
                  && !parentEntry.isType(ScopeEntry.Type.CONSTANT)
                  && !parentEntry.isType(ScopeEntry.Type.PARAMETER)) {
                // But only if parent entry is _local/_constant/parameter.
                parentEntry = null;
              }

              this.currentScope.addDeclaration(
                  scopeEntryType, identifier, identifierNode, parentEntry);
              // ParentEntry gets a usage via the constructor of the added declaration.

              this.scopeIndex.put(identifierNode, this.currentScope);
            });
  }

  @Override
  protected void walkPreMultipleAssignmentStatement(final AstNode node) {
    node
        .getFirstChild(MagikGrammar.MULTIPLE_ASSIGNMENT_ASSIGNABLES)
        .getChildren(MagikGrammar.EXPRESSION)
        .stream()
        .map(
            exprNode ->
                AstQuery.getOnlyFromChain(exprNode, MagikGrammar.ATOM, MagikGrammar.IDENTIFIER))
        .filter(Objects::nonNull)
        .forEach(
            identifierNode -> {
              final String tokenValue = identifierNode.getTokenValue();

              final int index = tokenValue.indexOf(':');
              final String identifier = index != -1 ? tokenValue.substring(index + 1) : tokenValue;

              if (this.currentScope.getScopeEntry(identifier) != null) {
                // Don't overwrite entries.
                return;
              }

              final ScopeEntry.Type type =
                  index != -1 ? ScopeEntry.Type.GLOBAL : ScopeEntry.Type.DEFINITION;
              this.currentScope.addDeclaration(type, identifier, identifierNode, null);
            });
  }

  @Override
  protected void walkPreAssignmentExpression(final AstNode node) {
    // get all atoms to the last <<
    final Integer lastAssignmentTokenIndex =
        node.getChildren().stream()
            .filter(
                childNode ->
                    childNode.getTokenValue().equals("<<")
                        || childNode.getTokenValue().equals("^<<"))
            .map(childNode -> node.getChildren().indexOf(childNode))
            .max(Comparator.naturalOrder())
            .orElse(null);

    final List<AstNode> childNodes = node.getChildren().subList(0, lastAssignmentTokenIndex);
    for (final AstNode childNode : childNodes) {
      if (!childNode.is(MagikGrammar.ATOM)) {
        continue;
      }

      final AstNode identifierNode = childNode.getFirstChild(MagikGrammar.IDENTIFIER);
      if (identifierNode == null) {
        return;
      }

      final String tokenValue = identifierNode.getTokenValue();
      if (this.currentScope.getScopeEntry(tokenValue) != null) {
        // Don't overwrite entries.
        return;
      }

      final int index = tokenValue.indexOf(':');
      final ScopeEntry.Type type =
          index != -1 ? ScopeEntry.Type.GLOBAL : ScopeEntry.Type.DEFINITION;
      final String identifier = index != -1 ? tokenValue.substring(index + 1) : tokenValue;

      this.currentScope.addDeclaration(type, identifier, identifierNode, null);
    }
  }

  @Override
  protected void walkPreAtom(final AstNode node) {
    final AstNode identifierNode = node.getFirstChild(MagikGrammar.IDENTIFIER);
    if (identifierNode == null) {
      return;
    }

    final String identifier = identifierNode.getTokenValue();
    final ScopeEntry existingScopeEntry = this.currentScope.getScopeEntry(identifier);
    if (existingScopeEntry != null) {
      if (existingScopeEntry.getDefinitionNode() != identifierNode) {
        // Prevent using ourselves.
        existingScopeEntry.addUsage(node);
      }

      // Don't overwrite entries.
      return;
    }

    // Add as global, and use directly.
    final ScopeEntry entry =
        this.currentScope.addDeclaration(ScopeEntry.Type.GLOBAL, identifier, identifierNode, null);
    entry.addUsage(node);
  }

  @Override
  protected void walkPostBody(final AstNode node) {
    // pop current scope
    this.currentScope = this.currentScope.getParentScope();
  }
}
