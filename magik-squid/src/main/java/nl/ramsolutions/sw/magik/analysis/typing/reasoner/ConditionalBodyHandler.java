package nl.ramsolutions.sw.magik.analysis.typing.reasoner;

import com.sonar.sslr.api.AstNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.restrictions.RestrictingConditionWalker;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.restrictions.TypeRestriction;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResult;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/**
 * Conditional body handler.
 *
 * Sets tested types for variables in the body after a conditional expression, such as an:
 * - `_is`/`_isnt` `_unset`/`_maybe` (singleton types)
 * - `_is`/`_isnt` <value>
 * - `kind_of?()`/`class_of?()`
 * - `_not` <above>
 *
 * Note that combinations (`_and`/`_or` etc) aren't supported, as reasoning/determining about
 * the types of variables in the body after a conditional expression with combinations of these
 * is often impossible. I.e., the condition expression `a.is_kind_of?(sw:integer) _andif a > 0`
 * cannot be determined, as `a` might be an `sw:integer`, just not positive.
 */
class ConditionalBodyHandler extends LocalTypeReasonerHandler {

    /**
     * Constructor.
     * @param state Reasoner state.
     */
    ConditionalBodyHandler(final LocalTypeReasonerState state) {
        super(state);
    }

    /**
     * Handle a conditional expression.
     * @param node CONDITIONAL_EXPRESSION node.
     */
    void handleConditionalExpression(final AstNode node) {
        final AstNode parentNode = node.getParent();
        final AstNode bodyNode;
        if (parentNode.is(MagikGrammar.IF, MagikGrammar.ELIF)) {
            bodyNode = node.getNextSibling().getNextSibling();
        } else if (parentNode.is(MagikGrammar.WHILE)) {
            bodyNode = node.getNextSibling().getFirstChild(MagikGrammar.BODY);
        } else {
            throw new IllegalStateException("Expected parent node, got: " + parentNode);
        }

        // Apply all restrictions to nodes in body.
        final TypeRestriction typeRestriction = this.getTypeRestriction(node);
        final Map<ScopeEntry, AbstractType> restrictions = typeRestriction.getRestrictions();
        restrictions.entrySet()
            .forEach(entry -> {
                final ScopeEntry scopeEntry = entry.getKey();
                final List<AstNode> usages = this.getUsageInBody(scopeEntry, bodyNode);
                final AbstractType restriction = entry.getValue();
                this.setNodeTypes(usages, restriction);
            });

        final AstNode ifNode = parentNode.is(MagikGrammar.IF)
            ? parentNode
            : parentNode.getParent();
        final AstNode elseNode = ifNode.getFirstChild(MagikGrammar.ELSE);
        final AstNode lastElifNode = ifNode.getLastChild(MagikGrammar.ELIF);
        if (elseNode != null
            && (lastElifNode == null || lastElifNode == parentNode)) {
            this.handleElseNode(elseNode);
        }
    }

    /**
     * Handle else node.
     * @param node ELSE node.
     */
    private void handleElseNode(final AstNode node) {
        // Find all previous restrictions from previous nodes.
        final AstNode ifNode = node.getParent();
        final List<AstNode> conditionalExpressionNodes = Stream.concat(
                Stream.of(ifNode.getFirstChild(MagikGrammar.CONDITIONAL_EXPRESSION)),
                ifNode.getChildren(MagikGrammar.ELIF).stream()
                    .map(elifNode -> elifNode.getFirstChild(MagikGrammar.CONDITIONAL_EXPRESSION)))
            .collect(Collectors.toList());
        final List<TypeRestriction> allRestrictions = conditionalExpressionNodes.stream()
            .map(this::getTypeRestriction)
            .collect(Collectors.toList());

        // Combine and invert all previous restrictions.
        final Map<ScopeEntry, AbstractType> restrictions = this.combineAndInvertRestrictions(allRestrictions);

        // Apply to body.
        final AstNode bodyNode = node.getFirstChild(MagikGrammar.BODY);
        restrictions.entrySet()
            .forEach(entry -> {
                final ScopeEntry scopeEntry = entry.getKey();
                final List<AstNode> usages = this.getUsageInBody(scopeEntry, bodyNode);
                final AbstractType restriction = entry.getValue();
                this.setNodeTypes(usages, restriction);
            });
    }

    /**
     * Get the top {@link TypeRestriction} for a conditional expression.
     * @param conditionNode CONDITIONAL_EXPRESSION node.
     * @return Top {@link TypeRestriction}.
     */
    private TypeRestriction getTypeRestriction(final AstNode conditionNode) {
        final GlobalScope globalScope = this.state.getMagikFile().getGlobalScope();
        final RestrictingConditionWalker walker = new RestrictingConditionWalker(this.state, globalScope);
        walker.walkAst(conditionNode);

        return walker.getTypeRestriction();
    }

    private Map<ScopeEntry, AbstractType> combineAndInvertRestrictions(final List<TypeRestriction> allRestrictions) {
        // Invert restrictions and combine grouped by scope entry.
        // Intersect all restrictions for each scope entry.
        return allRestrictions.stream()
            .map(TypeRestriction::not)
            .map(TypeRestriction::getRestrictions)
            .flatMap(map -> map.entrySet().stream())
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                AbstractType::intersection));
    }

    private List<AstNode> getUsageInBody(final ScopeEntry scopeEntry, final AstNode bodyNode) {
        final List<AstNode> upToAssignmentNodes = new ArrayList<>();
        final List<AstNode> atomNodesInBodyNode = bodyNode.getDescendants(MagikGrammar.ATOM);
        for (final AstNode usageNode : scopeEntry.getUsages()) {  // NOSONAR
            if (!atomNodesInBodyNode.contains(usageNode)) {
                continue;
            }

            final AstNode parentUsageNode = usageNode.getParent();
            if (parentUsageNode.is(MagikGrammar.ASSIGNMENT_EXPRESSION)) {
                final AstNode lastChildNode = parentUsageNode.getLastChild();
                if (usageNode != lastChildNode) {
                    // Isn't right part of assignment, so it is assigned to.
                    // Cannot reason further from here.
                    break;
                }
            }

            upToAssignmentNodes.add(usageNode);
        }

        return upToAssignmentNodes;
    }

    private void setNodeTypes(final List<AstNode> usageNodes, final AbstractType restrictedType) {
        usageNodes.forEach(usageNode -> {
            final ExpressionResult restrictedResult = new ExpressionResult(restrictedType);
            final AstNode assignNode = usageNode.getFirstChild();
            this.assignAtom(assignNode, restrictedResult);
        });
    }

}
