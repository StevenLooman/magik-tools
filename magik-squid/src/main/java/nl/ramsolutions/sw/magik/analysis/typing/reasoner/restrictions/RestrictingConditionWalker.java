package nl.ramsolutions.sw.magik.analysis.typing.reasoner.restrictions;

import com.sonar.sslr.api.AstNode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
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
    private final Map<AstNode, TypeRestriction> nodeTypeRestrictions = new HashMap<>();
    private AstNode topNode;

    public RestrictingConditionWalker(final LocalTypeReasonerState state, final GlobalScope globalScope) {
        this.state = state;
        this.globalScope = globalScope;
    }

    public TypeRestriction getTypeRestriction() {
        Objects.requireNonNull(this.topNode);
        return this.nodeTypeRestrictions.get(this.topNode);
    }

    @Override
    protected void walkPostConditionalExpression(final AstNode node) {
        this.topNode = node;

        // Pull up result.
        final AstNode childNode = node.getFirstChild();
        final TypeRestriction typeRestriction = this.nodeTypeRestrictions.containsKey(childNode)
            ? this.nodeTypeRestrictions.get(childNode)
            : this.createUndeterminableRestriction(childNode);
        this.nodeTypeRestrictions.put(node, typeRestriction);
    }

    @Override
    protected void walkPostExpression(final AstNode node) {
        // Pull up result.
        final AstNode childNode = node.getFirstChild();
        final TypeRestriction typeRestriction = this.nodeTypeRestrictions.containsKey(childNode)
            ? this.nodeTypeRestrictions.get(childNode)
            : this.createUndeterminableRestriction(childNode);
        this.nodeTypeRestrictions.put(node, typeRestriction);
    }

    @Override
    protected void walkPostUnaryExpression(final AstNode node) {
        final String operator = node.getTokenValue();
        if (!MagikKeyword.NOT.getValue().equalsIgnoreCase(operator)
            && !MagikOperator.NOT.getValue().equals(operator)) {
            return;
        }

        final AstNode childNode = node.getChildren().get(1);
        final TypeRestriction childRestriction = this.nodeTypeRestrictions.containsKey(childNode)
            ? this.nodeTypeRestrictions.get(childNode)
            : this.createUndeterminableRestriction(childNode);
        final TypeRestriction restriction = childRestriction.not();
        this.nodeTypeRestrictions.put(node, restriction);
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

        if (leftScopeEntry == null && rightScopeEntry == null) {
            // Need something to restrict.
            return;
        }

        final AstNode literalTypeNode = this.getLiteralNode(node);
        final AstNode singletonTypeNode = this.getSingletonTypeFromEqualityExpression(node);
        final String operator = operatorNode.getTokenValue();
        final TypeRestriction restriction;
        if (operator.equalsIgnoreCase(MagikKeyword.IS.getValue())
            || operator.equalsIgnoreCase(MagikKeyword.ISNT.getValue())) {
            if (singletonTypeNode != null) {
                restriction = this.createRestrictionForSingletonType(
                    leftScopeEntry, leftType,
                    rightScopeEntry, rightType,
                    leftNode, singletonTypeNode);
            } else if (literalTypeNode != null) {
                restriction = this.createRestrictionForValueType(
                    leftScopeEntry, rightScopeEntry,
                    leftNode, literalTypeNode);
            } else {
                restriction = this.createRestrictionForVariableType(
                    leftScopeEntry, leftType,
                    rightScopeEntry, rightType);
            }
        } else {
            restriction = this.createUndeterminableRestriction(node);
        }

        final TypeRestriction finalRestriction = operator.equalsIgnoreCase(MagikKeyword.ISNT.getValue())
            ? restriction.not()
            : restriction;
        this.nodeTypeRestrictions.put(node, finalRestriction);
    }

    private TypeRestriction createRestrictionForValueType(
            final ScopeEntry leftScopeEntry, final ScopeEntry rightScopeEntry,
            final AstNode leftNode, final AstNode literalTypeNode) {
        final TypeRestriction restriction;
        final ScopeEntry scopeEntry = literalTypeNode == leftNode
            ? rightScopeEntry
            : leftScopeEntry;
        Objects.requireNonNull(scopeEntry);
        final ExpressionResult nodeResult = this.state.getNodeType(literalTypeNode);
        final AbstractType restrictedType = nodeResult.get(0, UndefinedType.INSTANCE);
        Objects.requireNonNull(restrictedType);
        restriction = new IsValueTypeRestriction(scopeEntry, restrictedType);
        return restriction;
    }

    private TypeRestriction createRestrictionForVariableType(
            final ScopeEntry leftScopeEntry, final AbstractType leftType,
            final ScopeEntry rightScopeEntry, final AbstractType rightType) {
        return new IsVariableTypeRestriction(leftScopeEntry, leftType, rightScopeEntry, rightType);
    }

    private UndeterminableTypeRestriction createUndeterminableRestriction(final AstNode node) {
        final Set<ScopeEntry> scopeEntries = node.getDescendants(MagikGrammar.ATOM).stream()
            .map(atomNode -> atomNode.getFirstChild(MagikGrammar.IDENTIFIER))
            .filter(Objects::nonNull)
            .map(this.globalScope::getScopeEntry)
            .filter(Objects::nonNull)
            .collect(Collectors.toUnmodifiableSet());
        return new UndeterminableTypeRestriction(scopeEntries);
    }

    private TypeRestriction createRestrictionForSingletonType(
            final ScopeEntry leftScopeEntry, final AbstractType leftType,
            final ScopeEntry rightScopeEntry, final AbstractType rightType,
            final AstNode leftNode, final AstNode singletonTypeNode) {
        final TypeRestriction restriction;
        final ScopeEntry variableScopeEntry;
        final AbstractType variableType;
        final AbstractType restrictedType;
        if (singletonTypeNode == leftNode) {
            variableScopeEntry = rightScopeEntry;
            variableType = rightType;
            restrictedType = leftType;
        } else {
            variableScopeEntry = leftScopeEntry;
            variableType = leftType;
            restrictedType = rightType;
        }
        restriction = new IsTypeRestriction(variableScopeEntry, variableType, restrictedType);
        return restriction;
    }

    @CheckForNull
    private AstNode getLiteralNode(final AstNode node) {
        for (final AstNode testNode : node.getChildren(MagikGrammar.ATOM)) {
            final AstNode childNode = testNode.getFirstChild(
                    MagikGrammar.STRING,
                    MagikGrammar.NUMBER,
                    MagikGrammar.CHARACTER,
                    MagikGrammar.REGEXP,
                    MagikGrammar.SYMBOL,
                    MagikGrammar.SELF,
                    MagikGrammar.CLONE,
                    MagikGrammar.UNSET,
                    MagikGrammar.TRUE,
                    MagikGrammar.FALSE,
                    MagikGrammar.MAYBE);
            if (childNode != null) {
                return testNode;
            }
        }

        return null;
    }

    @CheckForNull
    private AstNode getSingletonTypeFromEqualityExpression(final AstNode node) {
        for (final AstNode testNode : node.getChildren(MagikGrammar.ATOM)) {
            if (testNode.getTokenValue().equalsIgnoreCase(MagikKeyword.UNSET.getValue())
                || testNode.getTokenValue().equalsIgnoreCase(MagikKeyword.MAYBE.getValue())) {
                return testNode;
            }
        }

        return null;
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

            final Scope scope = globalScope.getScopeForNode(node);
            Objects.requireNonNull(scope);

            final AstNode identifierNode = receiverNode.getFirstChild(MagikGrammar.IDENTIFIER);
            if (identifierNode == null) {
                return;
            }

            final AbstractType receiverType = this.state.getNodeType(receiverNode).get(0, UndefinedType.INSTANCE);
            final ScopeEntry scopeEntry = scope.getScopeEntry(identifierNode);
            Objects.requireNonNull(scopeEntry);

            final List<AstNode> argumentNodes = helper.getArgumentExpressionNodes();
            final AstNode argument0Node = argumentNodes.get(0);
            final ExpressionResult argument0Result = this.state.getNodeType(argument0Node);
            final AbstractType restrictedType = argument0Result.get(0, UndefinedType.INSTANCE);
            final TypeRestriction restriction = new IsTypeRestriction(scopeEntry, receiverType, restrictedType);

            final AstNode parentNode = node.getParent();
            if (parentNode != null
                && parentNode.is(MagikGrammar.POSTFIX_EXPRESSION)) {
                this.nodeTypeRestrictions.put(parentNode, restriction);
            }
        }
    }

}
