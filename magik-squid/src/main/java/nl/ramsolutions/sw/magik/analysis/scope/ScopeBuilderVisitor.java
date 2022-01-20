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

/**
 * Scope builder visitor.
 */
public class ScopeBuilderVisitor extends MagikVisitor {

    /**
     * Global scope.
     */
    private GlobalScope globalScope;

    /**
     * Current scope.
     */
    private Scope scope;

    /**
     * Scope index for quick searching.
     */
    private final Map<AstNode, Scope> scopeIndex = new HashMap<>();

    /**
     * Get the {{GlobalScope}}.
     * @return Global scope
     */
    public GlobalScope getGlobalScope() {
        return this.globalScope;
    }

    @Override
    protected void walkPreMagik(final AstNode node) {
        this.globalScope = new GlobalScope(this.scopeIndex, node);
        this.scope = this.globalScope;

        this.scopeIndex.put(node, this.globalScope);
    }

    @Override
    protected void walkPreBody(final AstNode node) {
        // Push new scope.
        final AstNode parentNode = node.getParent();
        if (parentNode.is(MagikGrammar.METHOD_DEFINITION)
            || parentNode.is(MagikGrammar.PROCEDURE_DEFINITION)) {
            this.walkPreBodyMethodProcDefinition(node, parentNode);
        } else if (parentNode.is(MagikGrammar.WHEN)) {
            this.walkPreBodyWhen(node, parentNode);
        } else if (parentNode.is(MagikGrammar.LOOP)) {
            this.walkPreBodyLoop(node);
        } else {
            this.walkPreBodyRegular(node);
        }

        this.scopeIndex.put(node, this.scope);
    }

    private void walkPreBodyRegular(final AstNode node) {
        // regular scope
        this.scope = new BodyScope(this.scope, node);
    }

    private void walkPreBodyLoop(final AstNode node) {
        this.scope = new BodyScope(scope, node);

        // add for-items to scope
        final AstNode forNode = AstQuery.getParentFromChain(
            node,
            MagikGrammar.LOOP,
            MagikGrammar.OVER,
            MagikGrammar.FOR);
        if (forNode != null) {
            final List<AstNode> identifierNodes = AstQuery.getChildrenFromChain(
                forNode,
                MagikGrammar.FOR_VARIABLES,
                MagikGrammar.IDENTIFIERS_WITH_GATHER,
                MagikGrammar.IDENTIFIER);
            for (final AstNode identifierNode: identifierNodes) {
                final String identifier = identifierNode.getTokenValue();
                this.scope.addDeclaration(ScopeEntry.Type.LOCAL, identifier, identifierNode, null);
            }
        }
    }

    private void walkPreBodyWhen(final AstNode node, final AstNode parentNode) {
        this.scope = new BodyScope(scope, node);

        // add _with items to scope
        final AstNode tryNode = parentNode.getParent();
        final AstNode tryVariableNode = tryNode.getFirstChild(MagikGrammar.TRY_VARIABLE);
        if (tryVariableNode != null) {
            final AstNode identifierNode = tryVariableNode.getFirstChild(MagikGrammar.IDENTIFIER);
            final String identifier = identifierNode.getTokenValue();
            this.scope.addDeclaration(ScopeEntry.Type.LOCAL, identifier, identifierNode, null);

            // Don't add identifierNode to scope index,
            // as this identifier can have multiple scopes (multiple _when).
        }
    }

    private void walkPreBodyMethodProcDefinition(final AstNode node, final AstNode parentNode) {
        this.scope = new ProcedureScope(this.scope, node);

        // Add all parameters to scope.
        parentNode.getChildren(MagikGrammar.PARAMETERS, MagikGrammar.ASSIGNMENT_PARAMETER).stream()
            .flatMap(paramsNode -> paramsNode.getChildren(MagikGrammar.PARAMETER).stream())
            .forEach(parameterNode -> {
                final AstNode identifierNode = parameterNode.getFirstChild(MagikGrammar.IDENTIFIER);
                final String identifier = identifierNode.getTokenValue();
                this.scope.addDeclaration(ScopeEntry.Type.PARAMETER, identifier, parameterNode, null);

                this.scopeIndex.put(parameterNode, this.scope);
            });
    }

    @Override
    protected void walkPreVariableDefinitionStatement(final AstNode node) {
        final String type = node.getFirstChild(MagikGrammar.VARIABLE_DEFINITION_MODIFIER)
            .getTokenValue()
            .toUpperCase()
            .substring(1);
        final ScopeEntry.Type scopeEntryType = ScopeEntry.Type.valueOf(type);

        Stream.concat(
            // Definitions from VARIABLE_DEFINITION_MULTI.
            node.getChildren(MagikGrammar.VARIABLE_DEFINITION_MULTI).stream()
                .map(varDefMultiNode -> varDefMultiNode.getFirstChild(MagikGrammar.IDENTIFIERS_WITH_GATHER))
                .flatMap(identifiersNode -> identifiersNode.getChildren(MagikGrammar.IDENTIFIER).stream()),
            // Defintions from VARIABLE_DEFINITION.
            node.getChildren(MagikGrammar.VARIABLE_DEFINITION).stream()
                .map(varDefNode -> varDefNode.getFirstChild(MagikGrammar.IDENTIFIER)))
            // Work it!
            .filter(identifierNode -> {
                // Don't overwrite entries.
                final String identifier = identifierNode.getTokenValue();
                return this.scope.getScopeEntry(identifier) == null;
            })
            .forEach(identifierNode -> {
                final String identifier = identifierNode.getTokenValue();

                // Figure parent entry.
                ScopeEntry parentEntry = null;
                if (scopeEntryType == ScopeEntry.Type.IMPORT) {
                    final AstNode procScopeNode = identifierNode.getFirstAncestor(
                        MagikGrammar.METHOD_DEFINITION,
                        MagikGrammar.PROCEDURE_DEFINITION);
                    if (procScopeNode != null) {
                        final AstNode parentScopeNode = procScopeNode.getFirstAncestor(MagikGrammar.BODY);
                        final Scope parentScope = parentScopeNode != null
                            ? this.scopeIndex.get(parentScopeNode)
                            : this.globalScope;
                        parentEntry = parentScope.getScopeEntry(identifier);
                    }
                }

                if (parentEntry != null
                    && !parentEntry.isType(ScopeEntry.Type.LOCAL)
                    && !parentEntry.isType(ScopeEntry.Type.CONSTANT)
                    && !parentEntry.isType(ScopeEntry.Type.PARAMETER)) {
                    // But only if parent entry is _local/_constant/parameter/iterator
                    parentEntry = null;
                }

                this.scope.addDeclaration(scopeEntryType, identifier, identifierNode, parentEntry);
                // ParentEntry gets a usage via the constructor of the added declaration.

                this.scopeIndex.put(identifierNode, this.scope);
            });
    }

    @Override
    protected void walkPreMultipleAssignmentStatement(final AstNode node) {
        node.getFirstChild(MagikGrammar.MULTIPLE_ASSIGNMENT_ASSIGNABLES)
            .getChildren(MagikGrammar.EXPRESSION).stream()
            .map(exprNode -> AstQuery.getOnlyFromChain(
                exprNode,
                MagikGrammar.ATOM,
                MagikGrammar.IDENTIFIER))
            .filter(Objects::nonNull)
            .forEach(identifierNode -> {
                final String identifier = identifierNode.getTokenValue();
                if (this.scope.getScopeEntry(identifier) != null) {
                    // Don't overwrite entries.
                    return;
                }

                this.scope.addDeclaration(ScopeEntry.Type.DEFINITION, identifier, identifierNode, null);
            });
    }

    @Override
    protected void walkPreAssignmentExpression(final AstNode node) {
        // get all atoms to the last <<
        final Integer lastAssignmentTokenIndex = node.getChildren().stream()
            .filter(childNode -> childNode.getTokenValue().equals("<<")
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

            final String identifier = identifierNode.getTokenValue();
            if (this.scope.getScopeEntry(identifier) != null) {
                // Don't overwrite entries.
                return;
            }

            // add as definition
            this.scope.addDeclaration(ScopeEntry.Type.DEFINITION, identifier, identifierNode, null);
        }
    }

    @Override
    protected void walkPreAtom(final AstNode node) {
        final AstNode identifierNode = node.getFirstChild(MagikGrammar.IDENTIFIER);
        if (identifierNode == null) {
            return;
        }

        final String identifier = identifierNode.getTokenValue();
        final ScopeEntry existingScopeEntry = this.scope.getScopeEntry(identifier);
        if (existingScopeEntry != null) {
            if (existingScopeEntry.getNode() != identifierNode) {
                // Prevent using ourselves.
                existingScopeEntry.addUsage(node);
            }

            // Don't overwrite entries.
            return;
        }

        // Add as global, and use directly.
        final ScopeEntry entry = this.globalScope.addDeclaration(ScopeEntry.Type.GLOBAL, identifier, node, null);
        entry.addUsage(node);
    }

    @Override
    protected void walkPostBody(final AstNode node) {
        // pop current scope
        this.scope = this.scope.getParentScope();
    }

}
