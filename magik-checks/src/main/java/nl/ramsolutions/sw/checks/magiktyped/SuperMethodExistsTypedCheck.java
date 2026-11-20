package nl.ramsolutions.sw.checks.magiktyped;

import com.sonar.sslr.api.AstNode;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.sonar.check.Rule;

/**
 * Check if method exists on super type.
 *
 * <p>A plain {@code _super.m()} is bound to the one parent answering {@code m()} when the method is
 * loaded: no answering parent and two or more answering parents both fail to load. A {@code
 * _super(name).m()} is reported when the named parent does not answer.
 */
@Rule(key = SuperMethodExistsTypedCheck.CHECK_KEY)
public class SuperMethodExistsTypedCheck extends MagikTypedCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "SuperMethodExists";

  private static final String MESSAGE = "Unknown method on super: %s";
  private static final String MESSAGE_TOO_MANY = "Too many super implementations of %s";

  @Override
  protected void walkPostMethodInvocation(final AstNode node) {
    // Check if receiver is _super.
    final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(node);
    final AstNode receiverNode = helper.getReceiverNode();
    if (receiverNode == null || !receiverNode.is(MagikGrammar.ATOM)) {
      return;
    }
    final AstNode superNode = receiverNode.getFirstChild(MagikGrammar.SUPER);
    if (superNode == null) {
      return;
    }

    // Get super type from reasoner state.
    final LocalTypeReasonerState state = this.getTypeReasonerState();
    final ExpressionResultString superResult = state.getNodeType(receiverNode);
    final TypeString superTypeStr = superResult.get(0, TypeString.UNDEFINED);
    if (superTypeStr.isUndefined()) {
      return;
    }

    final TypeString combinedTypeString = TypeString.combine(superTypeStr);
    Objects.requireNonNull(combinedTypeString);
    final List<TypeString> memberTypeStrs = combinedTypeString.getCombinedTypes();
    final boolean namedSuper = superNode.getFirstChild(MagikGrammar.IDENTIFIER) != null;
    if (namedSuper) {
      this.checkNamedSuper(node, helper, memberTypeStrs);
    } else {
      this.checkPlainSuper(node, helper, memberTypeStrs);
    }
  }

  /** Report each member of the named super that does not answer the method. */
  private void checkNamedSuper(
      final AstNode node,
      final MethodInvocationNodeHelper helper,
      final List<TypeString> memberTypeStrs) {
    final String methodName = helper.getMethodName();
    final TypeStringResolver resolver = this.getTypeStringResolver();
    for (final TypeString typeString : memberTypeStrs) {
      if (typeString.isUndefined()) {
        continue;
      }

      final Collection<MethodDefinition> methodDefs =
          resolver.getRespondingMethodDefinitionsAsParent(typeString, methodName);
      if (methodDefs.isEmpty()) {
        this.addUnknownMethodIssue(node, helper, typeString);
      }
    }
  }

  /**
   * Report a method no parent answers, once per parent, or one that two or more parents answer. Two
   * parents reaching the same inherited method are two answers.
   */
  private void checkPlainSuper(
      final AstNode node,
      final MethodInvocationNodeHelper helper,
      final List<TypeString> memberTypeStrs) {
    final String methodName = helper.getMethodName();
    final TypeStringResolver resolver = this.getTypeStringResolver();
    final Set<TypeString> parentTypeStrs =
        memberTypeStrs.stream()
            .filter(typeString -> !typeString.isUndefined())
            .map(resolver::getResolvedTypeString)
            .map(TypeString::getWithoutGenerics)
            .collect(Collectors.toSet());
    final Set<TypeString> answeringTypeStrs =
        parentTypeStrs.stream()
            .filter(
                typeString -> {
                  final Collection<MethodDefinition> methodDefs =
                      resolver.getRespondingMethodDefinitionsAsParent(typeString, methodName);
                  return !methodDefs.isEmpty();
                })
            .collect(Collectors.toSet());
    final int answeringCount = answeringTypeStrs.size();
    if (answeringCount > 1) {
      final String message = MESSAGE_TOO_MANY.formatted(methodName);
      this.addIssueAtMethodName(node, message);
      return;
    }

    // An unknown parent may answer.
    final boolean unknownParent = memberTypeStrs.stream().anyMatch(TypeString::isUndefined);
    if (answeringCount == 1 || unknownParent) {
      return;
    }

    memberTypeStrs.forEach(typeString -> this.addUnknownMethodIssue(node, helper, typeString));
  }

  private void addUnknownMethodIssue(
      final AstNode node, final MethodInvocationNodeHelper helper, final TypeString typeString) {
    final String fullName = helper.getReceiverMethodName(typeString);
    final String message = MESSAGE.formatted(fullName);
    this.addIssueAtMethodName(node, message);
  }

  private void addIssueAtMethodName(final AstNode node, final String message) {
    final AstNode firstIdentifierNode = node.getFirstChild(MagikGrammar.IDENTIFIER);
    final AstNode issueNode = firstIdentifierNode != null ? firstIdentifierNode : node;
    this.addIssue(issueNode, message);
  }
}
