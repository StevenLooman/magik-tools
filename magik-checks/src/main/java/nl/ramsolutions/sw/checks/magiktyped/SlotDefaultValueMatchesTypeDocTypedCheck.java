package nl.ramsolutions.sw.checks.magiktyped;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import java.util.Map;
import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.analysis.definitions.parsers.DefSlottedExemplarParser;
import nl.ramsolutions.sw.magik.analysis.helpers.ArgumentsNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.SimpleVectorNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.parser.TypeDocParser;
import org.sonar.check.Rule;

/** Check if a slot's default value matches its {@code @slot} type documentation. */
@Rule(key = SlotDefaultValueMatchesTypeDocTypedCheck.CHECK_KEY)
public class SlotDefaultValueMatchesTypeDocTypedCheck extends MagikTypedCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "SlotDefaultValueMatchesTypeDoc";

  private static final String MESSAGE = "Default value type (%s) does not match @slot type (%s).";

  @Override
  protected void walkPostProcedureInvocation(final AstNode node) {
    if (!DefSlottedExemplarParser.isDefSlottedExemplar(node)) {
      return;
    }

    // Read @slot types from the documentation.
    final TypeDocParser docParser = new TypeDocParser(node.getParent());
    final Map<String, TypeString> slotTypes = docParser.getSlotTypes();
    if (slotTypes.isEmpty()) {
      return;
    }

    final AstNode argumentsNode = node.getFirstChild(MagikGrammar.ARGUMENTS);
    final ArgumentsNodeHelper argumentsHelper = new ArgumentsNodeHelper(argumentsNode);
    final AstNode slotsNode = argumentsHelper.getArgument(1, MagikGrammar.SIMPLE_VECTOR);
    if (slotsNode == null) {
      return;
    }

    final TypeStringResolver resolver = this.getTypeStringResolver();
    final LocalTypeReasonerState state = this.getTypeReasonerState();
    for (final AstNode slotDefNode : slotsNode.getChildren(MagikGrammar.EXPRESSION)) {
      this.checkSlotDefault(slotDefNode, slotTypes, resolver, state);
    }
  }

  private void checkSlotDefault(
      final AstNode slotDefNode,
      final Map<String, TypeString> slotTypes,
      final TypeStringResolver resolver,
      final LocalTypeReasonerState state) {
    final SimpleVectorNodeHelper simpleVectorHelper =
        SimpleVectorNodeHelper.fromExpressionSafe(slotDefNode);
    if (simpleVectorHelper == null) {
      return;
    }

    final AstNode slotNameNode = simpleVectorHelper.getNth(0, MagikGrammar.SYMBOL);
    if (slotNameNode == null) {
      return;
    }

    final String slotName = slotNameNode.getTokenValue().substring(1);
    final TypeString slotType = slotTypes.get(slotName);
    if (slotType == null || slotType.isUndefined()) {
      // No (usable) @slot type to check against.
      return;
    }

    final AstNode defaultValueNode = this.getDefaultValueNode(slotDefNode);
    if (defaultValueNode == null) {
      // No explicit default value.
      return;
    }

    final TypeString defaultType = state.getNodeType(defaultValueNode).get(0, TypeString.UNDEFINED);
    if (defaultType.isUndefined()) {
      // Cannot determine the default value type.
      return;
    }

    if (resolver.isKindOf(defaultType, slotType)) {
      return;
    }

    final String message = MESSAGE.formatted(defaultType.getFullString(), slotType.getFullString());
    this.addIssue(defaultValueNode, message);
  }

  private AstNode getDefaultValueNode(final AstNode slotDefNode) {
    final AstNode atomNode = slotDefNode.getFirstChild(MagikGrammar.ATOM);
    if (atomNode == null) {
      return null;
    }

    final AstNode simpleVectorNode = atomNode.getFirstChild(MagikGrammar.SIMPLE_VECTOR);
    if (simpleVectorNode == null) {
      return null;
    }

    final List<AstNode> elements = simpleVectorNode.getChildren(MagikGrammar.EXPRESSION);
    if (elements.size() < 2) {
      return null;
    }

    return elements.get(1);
  }
}
