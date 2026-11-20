package nl.ramsolutions.sw.checks.magiktyped;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.sonar.check.Rule;

/** Test if compared type can match. */
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

    final TypeString leftTypeStr = this.getComparedType(leftNode);
    final TypeString rightTypeStr = this.getComparedType(rightNode);
    if (this.isUncheckable(leftTypeStr) || this.isUncheckable(rightTypeStr)) {
      return;
    }

    if (this.isGlobalAgainstUnset(leftNode, rightTypeStr)
        || this.isGlobalAgainstUnset(rightNode, leftTypeStr)
        || this.canMatch(node, leftTypeStr, rightTypeStr)) {
      return;
    }

    final String message =
        MESSAGE.formatted(leftTypeStr.getFullString(), rightTypeStr.getFullString());
    this.addIssue(node, message);
  }

  private TypeString getComparedType(final AstNode node) {
    final LocalTypeReasonerState state = this.getTypeReasonerState();
    final ExpressionResultString result = state.getNodeType(node);
    return result.get(0, TypeString.UNDEFINED);
  }

  /** An undefined type can be anything, and so can a union holding {@code _self}. */
  private boolean isUncheckable(final TypeString typeStr) {
    if (typeStr.isUndefined()) {
      return true;
    }

    final List<TypeString> parts = typeStr.getCombinedTypes();
    return parts.size() > 1 && parts.stream().anyMatch(TypeString::isSelf);
  }

  /**
   * A global is unset until the file defining it is loaded, and a {@code _dynamic} without a value
   * inherits whatever is bound.
   */
  private boolean isGlobalAgainstUnset(final AstNode node, final TypeString otherTypeStr) {
    final List<TypeString> otherParts = otherTypeStr.getCombinedTypes();
    return otherParts.contains(TypeString.SW_UNSET) && this.isGlobalRead(node);
  }

  private boolean isGlobalRead(final AstNode node) {
    final AstNode identifierNode =
        node.is(MagikGrammar.ATOM) ? node.getFirstChild(MagikGrammar.IDENTIFIER) : null;
    if (identifierNode == null) {
      return false;
    }

    final MagikFile magikFile = this.getMagikFile();
    final GlobalScope globalScope = magikFile.getGlobalScope();
    final Scope scope = globalScope.getScopeForNode(identifierNode);
    final ScopeEntry scopeEntry = scope != null ? scope.getScopeEntry(identifierNode) : null;
    if (scopeEntry == null || !scopeEntry.isType(ScopeEntry.Type.GLOBAL, ScopeEntry.Type.DYNAMIC)) {
      return false;
    }

    if (scopeEntry.isType(ScopeEntry.Type.GLOBAL)) {
      return true;
    }

    final AstNode definitionNode = scopeEntry.getDefinitionNode();
    final AstNode variableNode = definitionNode.getParent();
    return variableNode.is(MagikGrammar.VARIABLE_DEFINITION)
        && variableNode.getFirstChild(MagikGrammar.EXPRESSION) == null;
  }

  private boolean canMatch(
      final AstNode node, final TypeString leftTypeStr, final TypeString rightTypeStr) {
    if (leftTypeStr.isSelf()) {
      return this.canBeSelf(node, rightTypeStr);
    }

    if (rightTypeStr.isSelf()) {
      return this.canBeSelf(node, leftTypeStr);
    }

    final TypeString intersection = TypeString.intersection(leftTypeStr, rightTypeStr);
    return intersection != null;
  }

  /** In a method {@code _self} is its exemplar, or an instance of it or of a subtype. */
  private boolean canBeSelf(final AstNode node, final TypeString otherTypeStr) {
    final AstNode definitionNode =
        node.getFirstAncestor(MagikGrammar.METHOD_DEFINITION, MagikGrammar.PROCEDURE_DEFINITION);
    if (otherTypeStr.isSelf()
        || definitionNode == null
        || definitionNode.isNot(MagikGrammar.METHOD_DEFINITION)) {
      return true;
    }

    final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(definitionNode);
    final TypeString ownerTypeStr = helper.getExemplarTypeString();
    if (ownerTypeStr.isUndefined()) {
      return true;
    }

    final TypeStringResolver resolver = this.getTypeStringResolver();
    final List<TypeString> otherParts = otherTypeStr.getCombinedTypes();
    return otherParts.stream().anyMatch(part -> resolver.isKindOf(part, ownerTypeStr));
  }

  @SuppressWarnings("checkstyle:NestedIfDepth")
  @Override
  protected void walkPostMethodInvocation(final AstNode node) {
    final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(node);
    final boolean isKindOf =
        helper.isMethodInvocationOf(ComparedTypesDoNotMatchTypedCheck.IS_KIND_OF);
    final boolean isClassOf =
        helper.isMethodInvocationOf(ComparedTypesDoNotMatchTypedCheck.IS_CLASS_OF);
    if (isKindOf || isClassOf) {
      final LocalTypeReasonerState state = this.getTypeReasonerState();
      final AstNode receiverNode = helper.getReceiverNode();
      final TypeString receiverTypeStr =
          state.getNodeType(receiverNode).get(0, TypeString.UNDEFINED);
      if (receiverTypeStr.isUndefined() || receiverTypeStr.equals(TypeString.SW_OBJECT)) {
        return;
      }

      final List<AstNode> argumentNodes = helper.getArgumentExpressionNodes();
      final AstNode argument0Node = argumentNodes.get(0);
      final ExpressionResultString argument0Result = state.getNodeType(argument0Node);
      final TypeString checkedTypeStr = argument0Result.get(0, TypeString.UNDEFINED);
      if (checkedTypeStr.isUndefined()) {
        return;
      }

      if (isClassOf) {
        final TypeString intersection = TypeString.intersection(receiverTypeStr, checkedTypeStr);
        if (intersection == null) {
          final String message =
              MESSAGE.formatted(receiverTypeStr.getFullString(), checkedTypeStr.getFullString());
          this.addIssue(node, message);
        }
      } else { // isKindOf.
        final TypeStringResolver resolver = this.getTypeStringResolver();
        if (!resolver.isKindOf(receiverTypeStr, checkedTypeStr)) {
          final String message =
              MESSAGE.formatted(receiverTypeStr.getFullString(), checkedTypeStr.getFullString());
          this.addIssue(node, message);
        }
      }
    }
  }
}
