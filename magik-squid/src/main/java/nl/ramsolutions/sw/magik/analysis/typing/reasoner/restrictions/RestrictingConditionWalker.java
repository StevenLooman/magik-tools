package nl.ramsolutions.sw.magik.analysis.typing.reasoner.restrictions;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.analysis.AstWalker;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResult;
import nl.ramsolutions.sw.magik.analysis.typing.types.UndefinedType;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.api.MagikKeyword;
import nl.ramsolutions.sw.magik.api.MagikOperator;

/**
 * Restriction CONDITIONAL_EXPRESSION walker.
 *
 * Collects restrictions from conditional expressions. Note that this walker is
 * not responsible for handling the body of the conditional expression.
 *
 * Some examples of conditional expressions:
 * - `a _is _unset`
 * - `a _isnt _unset`
 * - `a _is b`
 * - `a _isnt b`
 * - `a _is _unset _or b _is _unset`
 * - `a _is _unset _and b _is _unset`
 * - `a.is_kind_of?(integer)`
 * - `a.is_class_of?(integer)`
 *
 * Note that only the `_is` and `_isnt` operators are handled. Operators such as
 * `=` and `~=` can be custom binary operators and as such, we cannot provide
 * any guarantees regarding the type of the variable.
 */
public class RestrictingConditionWalker extends AstWalker {

    private static final String IS_KIND_OF = "is_kind_of?()";
    private static final String IS_CLASS_OF = "is_class_of?()";

    private final LocalTypeReasonerState state;
    private final GlobalScope globalScope;
    private final Map<AstNode, Set<TypeRestriction>> nodeTypeRestrictions = new HashMap<>();
    private AstNode topNode;

    public RestrictingConditionWalker(final LocalTypeReasonerState state, final GlobalScope globalScope) {
        this.state = state;
        this.globalScope = globalScope;
    }

    public Set<TypeRestriction> getTypeRestriction() {
        Objects.requireNonNull(this.topNode);
        return this.nodeTypeRestrictions.get(this.topNode);
    }

    @Override
    protected void walkPostConditionalExpression(final AstNode node) {
        this.topNode = node;

        // Pull up result.
        final AstNode childNode = node.getFirstChild();
        final Set<TypeRestriction> typeRestriction = this.nodeTypeRestrictions.containsKey(childNode)
            ? this.nodeTypeRestrictions.get(childNode)
            : this.createUndeterminableRestriction(childNode);
        this.nodeTypeRestrictions.put(node, typeRestriction);
    }

    @Override
    protected void walkPostExpression(final AstNode node) {
        // Pull up result.
        final AstNode childNode = node.getFirstChild();
        final Set<TypeRestriction> restrictions = this.nodeTypeRestrictions.containsKey(childNode)
            ? this.nodeTypeRestrictions.get(childNode)
            : this.createUndeterminableRestriction(childNode);
        this.nodeTypeRestrictions.put(node, restrictions);
    }

    private Set<TypeRestriction> createUndeterminableRestriction(final AstNode node) {
        return node.getDescendants(MagikGrammar.ATOM).stream()
            .map(atomNode -> {
                final AstNode identifierNode = atomNode.getFirstChild(MagikGrammar.IDENTIFIER);
                if (identifierNode == null) {
                    return null;
                }

                final ScopeEntry scopeEntry = this.globalScope.getScopeEntry(identifierNode);
                if (scopeEntry == null) {
                    return null;
                }

                final ExpressionResult result = this.state.getNodeType(node);
                final AbstractType unrestrictedType = result.get(0, UndefinedType.INSTANCE);
                return new ScopeEntryTypeRestriction(scopeEntry, unrestrictedType, UndefinedType.INSTANCE);
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    protected void walkPostUnaryExpression(final AstNode node) {
        final String operator = node.getTokenValue();
        if (!MagikKeyword.NOT.getValue().equalsIgnoreCase(operator)
            && !MagikOperator.NOT.getValue().equals(operator)) {
            return;
        }

        final AstNode childNode = node.getChildren().get(1);
        final Set<TypeRestriction> childRestrictions = this.nodeTypeRestrictions.containsKey(childNode)
            ? this.nodeTypeRestrictions.get(childNode)
            : this.createUndeterminableRestriction(childNode);
        final Set<TypeRestriction> restrictions = childRestrictions.stream()
            .map(TypeRestriction::not)
            .collect(Collectors.toUnmodifiableSet());
        this.nodeTypeRestrictions.put(node, restrictions);
    }

    @Override
    protected void walkPostEqualityExpression(final AstNode node) {
        final Scope scope = globalScope.getScopeForNode(node);
        Objects.requireNonNull(scope);

        final List<AstNode> children = node.getChildren();
        final AstNode leftNode = children.get(0);
        final AstNode leftIdentifierNode = leftNode.getFirstChild(MagikGrammar.IDENTIFIER);
        final ScopeEntry leftScopeEntry = leftIdentifierNode != null
            ? scope.getScopeEntry(leftIdentifierNode)
            : null;
        final AbstractType leftType = this.state.getNodeType(leftNode).get(0, UndefinedType.INSTANCE);

        final AstNode operatorNode = children.get(1);

        final AstNode rightNode = children.get(2);
        final AstNode rightIdentifierNode = rightNode.getFirstChild(MagikGrammar.IDENTIFIER);
        final ScopeEntry rightScopeEntry = rightIdentifierNode != null
            ? scope.getScopeEntry(rightIdentifierNode)
            : null;
        final AbstractType rightType = this.state.getNodeType(rightNode).get(0, UndefinedType.INSTANCE);

        final String operator = operatorNode.getTokenValue();
        final Set<TypeRestriction> restrictions =
            operator.equalsIgnoreCase(MagikKeyword.IS.getValue())
            || operator.equalsIgnoreCase(MagikKeyword.ISNT.getValue())
            ? this.createRestriction(leftScopeEntry, leftType, rightScopeEntry, rightType)
            : Collections.emptySet();

        final Set<TypeRestriction> finalRestriction = operator.equalsIgnoreCase(MagikKeyword.ISNT.getValue())
            ? restrictions.stream()
                .map(TypeRestriction::not)
                .collect(Collectors.toUnmodifiableSet())
            : restrictions;
        this.nodeTypeRestrictions.put(node, finalRestriction);
    }

    private Set<TypeRestriction> createRestriction(
            @Nullable ScopeEntry leftScopeEntry,
            AbstractType leftType,
            @Nullable ScopeEntry rightScopeEntry,
            AbstractType rightType) {
        final Set<TypeRestriction> restrictions = new HashSet<>();
        if (leftScopeEntry != null) {
            final TypeRestriction restriction = new ScopeEntryTypeRestriction(leftScopeEntry, leftType, rightType);
            restrictions.add(restriction);
        }

        if (rightScopeEntry != null) {
            final TypeRestriction restriction = new ScopeEntryTypeRestriction(rightScopeEntry, rightType, leftType);
            restrictions.add(restriction);
        }

        return restrictions;
    }

    @Override
    protected void walkPostMethodInvocation(final AstNode node) {
        final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(node);
        if (helper.isMethodInvocationOf(RestrictingConditionWalker.IS_KIND_OF)
            || helper.isMethodInvocationOf(RestrictingConditionWalker.IS_CLASS_OF)) {
            final AstNode receiverNode = helper.getReceiverNode();
            if (receiverNode.isNot(MagikGrammar.ATOM)) {
                return;
            }

            final AstNode identifierNode = receiverNode.getFirstChild(MagikGrammar.IDENTIFIER);
            if (identifierNode == null) {
                return;
            }

            final AstNode parentNode = node.getParent();
            if (parentNode == null
                || !parentNode.is(MagikGrammar.POSTFIX_EXPRESSION)) {
                return;
            }

            final Scope scope = globalScope.getScopeForNode(receiverNode);
            Objects.requireNonNull(scope);
            final AbstractType receiverType = this.state.getNodeType(receiverNode).get(0, UndefinedType.INSTANCE);
            final ScopeEntry scopeEntry = scope.getScopeEntry(identifierNode);
            Objects.requireNonNull(scopeEntry);

            final List<AstNode> argumentNodes = helper.getArgumentExpressionNodes();
            final AstNode argument0Node = argumentNodes.get(0);
            final ExpressionResult argument0Result = this.state.getNodeType(argument0Node);
            final AbstractType restrictedType = argument0Result.get(0, UndefinedType.INSTANCE);
            final Set<TypeRestriction> restriction = this.createRestriction(
                scopeEntry, receiverType,
                null, restrictedType);

            this.nodeTypeRestrictions.put(parentNode, restriction);
        }
    }

}
