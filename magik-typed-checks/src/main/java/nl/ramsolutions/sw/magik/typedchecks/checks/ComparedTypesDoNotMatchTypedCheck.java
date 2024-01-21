package nl.ramsolutions.sw.magik.typedchecks.checks;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResult;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.types.UndefinedType;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;
import org.sonar.check.Rule;

/**
 * Test if compared type can match.
 */
@Rule(key = ComparedTypesDoNotMatchTypedCheck.CHECK_KEY)
public class ComparedTypesDoNotMatchTypedCheck extends MagikTypedCheck {

    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final String CHECK_KEY = "ComparedTypesDoNotMatch";

    private static final String IS_KIND_OF = "is_kind_of?()";
    private static final String IS_CLASS_OF = "is_class_of?()";
    private static final String MESSAGE = "Left type (%s) can never match right type (%s)";

    @Override
    protected void walkPostEqualityExpression(final AstNode node) {
        final List<AstNode> children = node.getChildren();
        final AstNode operatorNode = children.get(1);
        if (!operatorNode.getTokenValue().equalsIgnoreCase("_is")
            && !operatorNode.getTokenValue().equalsIgnoreCase("_isnt")) {
            return;
        }

        final AstNode leftNode = children.get(0);
        final AstNode rightNode = children.get(2);

        final LocalTypeReasonerState state = this.getTypeReasonerState();
        final AbstractType leftType = state.getNodeType(leftNode).get(0, UndefinedType.INSTANCE);
        final AbstractType rightType = state.getNodeType(rightNode).get(0, UndefinedType.INSTANCE);
        if (leftType == UndefinedType.INSTANCE || rightType == UndefinedType.INSTANCE) {
            return;
        }

        final AbstractType intersection = AbstractType.intersection(leftType, rightType);
        if (intersection == null) {
            final TypeString leftTypeString = leftType.getTypeString();
            final TypeString rightTypeString = rightType.getTypeString();
            final String message = String.format(
                MESSAGE,
                leftTypeString.getFullString(), rightTypeString.getFullString());
            this.addIssue(node, message);
        }
    }

    @SuppressWarnings("checkstyle:NestedIfDepth")
    @Override
    protected void walkPostMethodInvocation(final AstNode node) {
        final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(node);
        final boolean isKindOf = helper.isMethodInvocationOf(ComparedTypesDoNotMatchTypedCheck.IS_KIND_OF);
        final boolean isClassOf = helper.isMethodInvocationOf(ComparedTypesDoNotMatchTypedCheck.IS_CLASS_OF);
        if (isKindOf
            || isClassOf) {
            final LocalTypeReasonerState state = this.getTypeReasonerState();
            final AstNode receiverNode = helper.getReceiverNode();
            final AbstractType receiverType = state.getNodeType(receiverNode).get(0, UndefinedType.INSTANCE);
            if (receiverType == UndefinedType.INSTANCE) {
                return;
            }

            final List<AstNode> argumentNodes = helper.getArgumentExpressionNodes();
            final AstNode argument0Node = argumentNodes.get(0);
            final ExpressionResult argument0Result = state.getNodeType(argument0Node);
            final AbstractType checkedType = argument0Result.get(0, UndefinedType.INSTANCE);
            if (checkedType == UndefinedType.INSTANCE) {
                return;
            }

            if (isClassOf) {
                final AbstractType intersection = AbstractType.intersection(receiverType, checkedType);
                if (intersection == null) {
                    final TypeString receiverTypeString = receiverType.getTypeString();
                    final TypeString checkedTypeString = checkedType.getTypeString();
                    final String message = String.format(
                        MESSAGE,
                        receiverTypeString.getFullString(), checkedTypeString.getFullString());
                    this.addIssue(node, message);
                }
            } else {  // isKindOf.
                if (!receiverType.isKindOf(checkedType)) {
                    final TypeString receiverTypeString = receiverType.getTypeString();
                    final TypeString checkedTypeString = checkedType.getTypeString();
                    final String message = String.format(
                        MESSAGE,
                        receiverTypeString.getFullString(), checkedTypeString.getFullString());
                    this.addIssue(node, message);
                }
            }
        }
    }

}
