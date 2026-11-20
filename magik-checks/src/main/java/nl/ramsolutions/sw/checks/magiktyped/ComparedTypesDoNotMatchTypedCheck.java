package nl.ramsolutions.sw.checks.magiktyped;

import com.sonar.sslr.api.AstNode;
import java.util.Collection;
import java.util.List;
import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ITypeStringDefinition;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.PackageNodeHelper;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.SelfHelper;
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

  /**
   * An undefined type can be anything, and so can an unbound generic or a union holding {@code
   * _self}.
   */
  private boolean isUncheckable(final TypeString typeStr) {
    if (typeStr.isUndefined()) {
      return true;
    }

    final List<TypeString> parts = typeStr.getCombinedTypes();
    return parts.stream().anyMatch(TypeString::isGenericReference)
        || parts.size() > 1 && parts.stream().anyMatch(TypeString::isSelf);
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

  @Override
  protected void walkPostMethodInvocation(final AstNode node) {
    final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(node);
    final boolean isKindOf =
        helper.isMethodInvocationOf(ComparedTypesDoNotMatchTypedCheck.IS_KIND_OF);
    final boolean isClassOf =
        helper.isMethodInvocationOf(ComparedTypesDoNotMatchTypedCheck.IS_CLASS_OF);
    if (isKindOf || isClassOf) {
      final AstNode receiverNode = helper.getReceiverNode();
      final TypeString receiverTypeStr = this.getTestedType(node, receiverNode);
      if (this.isUncheckable(receiverTypeStr) || receiverTypeStr.equals(TypeString.SW_OBJECT)) {
        return;
      }

      final List<AstNode> argumentNodes = helper.getArgumentExpressionNodes();
      final AstNode argument0Node = argumentNodes.get(0);
      final TypeString checkedTypeStr = this.getTestedType(node, argument0Node);
      if (this.isUncheckable(checkedTypeStr)) {
        return;
      }

      final boolean canMatch =
          isKindOf
              ? this.canBeKindOf(receiverNode, receiverTypeStr, checkedTypeStr)
              : this.canBeClassOf(receiverNode, argument0Node, receiverTypeStr, checkedTypeStr);
      if (!canMatch) {
        final String message =
            MESSAGE.formatted(receiverTypeStr.getFullString(), checkedTypeStr.getFullString());
        this.addIssue(node, message);
      }
    }
  }

  /**
   * The reasoned type of a side of a test. In a method {@code _self} is its exemplar, or an
   * instance of it or of a subtype. Elsewhere it is not known.
   */
  private TypeString getTestedType(final AstNode node, final AstNode sideNode) {
    final TypeString typeStr = this.getComparedType(sideNode);
    final List<TypeString> parts = typeStr.getCombinedTypes();
    if (parts.stream().noneMatch(TypeString::isSelf)) {
      return typeStr;
    }

    final AstNode definitionNode =
        node.getFirstAncestor(MagikGrammar.METHOD_DEFINITION, MagikGrammar.PROCEDURE_DEFINITION);
    if (definitionNode == null || definitionNode.isNot(MagikGrammar.METHOD_DEFINITION)) {
      return TypeString.UNDEFINED;
    }

    return SelfHelper.substituteSelf(typeStr, node);
  }

  /**
   * A value is an instance of its type or of a subtype, so it can be a kind of the tested type when
   * some known type is a kind of both. An exemplar read through its global is that exemplar.
   */
  private boolean canBeKindOf(
      final AstNode receiverNode, final TypeString receiverTypeStr, final TypeString kindTypeStr) {
    final TypeStringResolver resolver = this.getTypeStringResolver();
    if (this.isExemplarRead(receiverNode)) {
      return resolver.isKindOf(receiverTypeStr, kindTypeStr);
    }

    return resolver.canBeKindOfBoth(receiverTypeStr, kindTypeStr);
  }

  /**
   * The class of a value is its type or a subtype of it, and the class of an exemplar read through
   * its global is exactly its type.
   */
  private boolean canBeClassOf(
      final AstNode receiverNode,
      final AstNode argumentNode,
      final TypeString receiverTypeStr,
      final TypeString argumentTypeStr) {
    final TypeStringResolver resolver = this.getTypeStringResolver();
    final boolean receiverIsExact = this.isExemplarRead(receiverNode);
    final boolean argumentIsExact = this.isExemplarRead(argumentNode);
    if (!receiverIsExact && !argumentIsExact) {
      return resolver.canBeKindOfBoth(receiverTypeStr, argumentTypeStr);
    }

    return (!argumentIsExact || resolver.isKindOf(argumentTypeStr, receiverTypeStr))
        && (!receiverIsExact || resolver.isKindOf(receiverTypeStr, argumentTypeStr));
  }

  /** Whether a node reads a global that names an exemplar. */
  private boolean isExemplarRead(final AstNode node) {
    AstNode atomNode = node;
    while (atomNode.isNot(MagikGrammar.ATOM) && atomNode.getNumberOfChildren() == 1) {
      atomNode = atomNode.getFirstChild();
    }

    if (atomNode.getNumberOfChildren() != 1 || !this.isGlobalRead(atomNode)) {
      return false;
    }

    final AstNode identifierNode = atomNode.getFirstChild(MagikGrammar.IDENTIFIER);
    final String identifier = identifierNode.getTokenValue();
    final PackageNodeHelper packageHelper = new PackageNodeHelper(identifierNode);
    final String currentPackage = packageHelper.getCurrentPackage();
    final TypeString nameTypeStr = TypeString.ofIdentifier(identifier, currentPackage);
    final TypeStringResolver resolver = this.getTypeStringResolver();
    final Collection<ITypeStringDefinition> definitions = resolver.resolve(nameTypeStr);
    return !definitions.isEmpty()
        && definitions.stream().allMatch(ExemplarDefinition.class::isInstance);
  }
}
